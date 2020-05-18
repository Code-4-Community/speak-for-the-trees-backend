package com.codeforcommunity.dataaccess;

import static org.jooq.generated.Tables.*;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.VerificationKeyType;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.ExpiredSecretKeyException;
import com.codeforcommunity.exceptions.InvalidSecretKeyException;
import com.codeforcommunity.exceptions.UsedSecretKeyException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.UsernameAlreadyInUseException;
import com.codeforcommunity.processor.AuthProcessorImpl;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import org.jooq.DSLContext;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.pojos.UserTeam;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.generated.tables.records.VerificationKeysRecord;

/** Encapsulates all the database operations that are required for {@link AuthProcessorImpl}. */
public class AuthDatabaseOperations {

  private final DSLContext db;

  private final int secondsVerificationEmailValid;
  private final int secondsForgotPasswordValid;
  private final int msRefreshExpiration;

  public AuthDatabaseOperations(DSLContext db) {
    this.db = db;

    Properties expirationProperties = PropertiesLoader.getExpirationProperties();
    this.secondsVerificationEmailValid =
        Integer.parseInt(expirationProperties.getProperty("seconds_verification_email_valid"));
    this.secondsForgotPasswordValid =
        Integer.parseInt(expirationProperties.getProperty("seconds_forgot_password_valid"));
    this.msRefreshExpiration =
        Integer.parseInt(expirationProperties.getProperty("ms_refresh_expiration"));
  }

  /**
   * Creates a JWTData object for the user with the given email if they exist.
   *
   * @throws UserDoesNotExistException if given email does not match a user.
   */
  public JWTData getUserJWTData(String email) {
    Optional<Users> maybeUser =
        Optional.ofNullable(
            db.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOneInto(Users.class));

    if (maybeUser.isPresent()) {
      Users user = maybeUser.get();
      Integer userId = user.getId();

      Optional<UserTeam> mayberUserTeam =
          Optional.ofNullable(
              db.selectFrom(USER_TEAM)
                  .where(USER_TEAM.USER_ID.eq(userId))
                  .fetchOneInto(UserTeam.class));
      return mayberUserTeam
          .map(userTeam -> new JWTData(userId, user.getPrivilegeLevel(), userTeam.getTeamId()))
          .orElseGet(() -> new JWTData(user.getId(), user.getPrivilegeLevel(), -1));
    } else {
      throw new UserDoesNotExistException(email);
    }
  }

  /**
   * Returns true if the given username and password correspond to a user in the USER table and
   * false otherwise.
   */
  public boolean isValidLogin(String email, String pass) {
    Optional<Users> maybeUser =
        Optional.ofNullable(
            db.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOneInto(Users.class));

    return maybeUser
        .filter(user -> Passwords.isExpectedPassword(pass, user.getPassHash()))
        .isPresent();
  }

  /**
   * TODO: Refactor this method to take in a DTO / POJO instance Creates a new row in the USER table
   * with the given values.
   *
   * @throws EmailAlreadyInUseException if the given username and email are already used in the USER
   *     table.
   */
  public UsersRecord createNewUser(
      String username, String email, String password, String firstName, String lastName) {
    boolean emailUsed = db.fetchExists(db.selectFrom(USERS).where(USERS.EMAIL.eq(email)));
    if (emailUsed) {
      throw new EmailAlreadyInUseException(email);
    }
    boolean usernameUsed = db.fetchExists(db.selectFrom(USERS).where(USERS.USERNAME.eq(username)));
    if (usernameUsed) {
      throw new UsernameAlreadyInUseException(username);
    }

    UsersRecord newUser = db.newRecord(USERS);
    newUser.setUsername(username);
    newUser.setEmail(email);
    newUser.setPassHash(Passwords.createHash(password));
    newUser.setFirstName(firstName);
    newUser.setLastName(lastName);
    newUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    newUser.store();

    return newUser;
  }

  /** Given a JWT signature, store it in the BLACKLISTED_REFRESHES table. */
  public void addToBlackList(String signature) {
    Timestamp expirationTimestamp = Timestamp.from(Instant.now().plusMillis(msRefreshExpiration));
    db.newRecord(Tables.BLACKLISTED_REFRESHES).values(signature, expirationTimestamp).store();
  }

  /** Given a JWT signature return true if it is stored in the BLACKLISTED_REFRESHES table. */
  public boolean isOnBlackList(String signature) {
    return db.fetchExists(
        Tables.BLACKLISTED_REFRESHES.where(
            Tables.BLACKLISTED_REFRESHES.REFRESH_HASH.eq(signature)));
  }

  /**
   * Validates the secret key for the user it was created for and returns the appropriate user.
   *
   * @throws InvalidSecretKeyException if the given token does not exist.
   * @throws UsedSecretKeyException if the given token has already been used.
   * @throws ExpiredSecretKeyException if the given token is expired.
   */
  public UsersRecord validateSecretKey(String secretKey, VerificationKeyType type) {
    VerificationKeysRecord verificationKey =
        db.selectFrom(Tables.VERIFICATION_KEYS)
            .where(Tables.VERIFICATION_KEYS.ID.eq(secretKey).and(VERIFICATION_KEYS.TYPE.eq(type)))
            .fetchOneInto(VerificationKeysRecord.class);

    if (verificationKey == null) {
      throw new InvalidSecretKeyException(type);
    }

    if (verificationKey.getUsed()) {
      throw new UsedSecretKeyException(type);
    }

    if (!isTokenDateValid(verificationKey, type)) {
      throw new ExpiredSecretKeyException(type);
    }

    verificationKey.setUsed(true);
    verificationKey.store();

    return db.selectFrom(USERS).where(USERS.ID.eq(verificationKey.getUserId())).fetchOne();
  }

  /**
   * TODO: This method should be called as part of sign-up flow
   *
   * <p>Given a userId and token, stores the token in the verification_keys table for the user and
   * invalidates all other keys of this type for this user.
   */
  public String createSecretKey(int userId, VerificationKeyType type) {

    // Maybe add a different column besides used?
    db.update(VERIFICATION_KEYS)
        .set(VERIFICATION_KEYS.USED, true)
        .where(VERIFICATION_KEYS.USER_ID.eq(userId))
        .and(VERIFICATION_KEYS.TYPE.eq(type))
        .execute();

    String token = Passwords.generateRandomToken(50);

    VerificationKeysRecord keysRecord = db.newRecord(Tables.VERIFICATION_KEYS);
    keysRecord.setId(token);
    keysRecord.setUserId(userId);
    keysRecord.setType(type);
    keysRecord.store();

    return token;
  }

  /**
   * Determines if given token of a specified type is still valid.
   *
   * @return true if it is within the time specified in the expiration.properties file.
   */
  private boolean isTokenDateValid(VerificationKeysRecord tokenResult, VerificationKeyType type) {
    Timestamp cutoffDate;
    if (type == VerificationKeyType.VERIFY_EMAIL) {
      cutoffDate = Timestamp.from(Instant.now().minusSeconds(secondsVerificationEmailValid));
    } else if (type == VerificationKeyType.FORGOT_PASSWORD) {
      cutoffDate = Timestamp.from(Instant.now().minusSeconds(secondsForgotPasswordValid));
    } else {
      throw new IllegalStateException(
          String.format("Verification type %s not implemented", type.name()));
    }
    return tokenResult.getCreated().after(cutoffDate);
  }
}
