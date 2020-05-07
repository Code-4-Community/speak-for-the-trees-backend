package com.codeforcommunity.dataaccess;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.ExpiredEmailVerificationTokenException;
import com.codeforcommunity.exceptions.InvalidEmailVerificationTokenException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.UsernameAlreadyInUseException;
import com.codeforcommunity.processor.AuthProcessorImpl;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import org.jooq.DSLContext;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.generated.tables.records.VerificationKeysRecord;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

import static org.jooq.generated.Tables.USERS;

/**
 * Encapsulates all the database operations that are required for {@link AuthProcessorImpl}.
 */
public class AuthDatabaseOperations {

    private final DSLContext db;

    public final int secondsVerificationEmailValid;
    public final int msRefreshExpiration;

    public AuthDatabaseOperations(DSLContext db) {
        this.db = db;

        Properties expireationProperties = PropertiesLoader.getExpirationProperties();
        this.secondsVerificationEmailValid = Integer.parseInt(expireationProperties.getProperty("seconds_verification_email_valid"));
        this.msRefreshExpiration = Integer.parseInt(expireationProperties.getProperty("ms_refresh_expiration"));
    }

    /**
     * Creates a JWTData object for the user with the given email if they exist.
     *
     * @throws UserDoesNotExistException if given email does not match a user.
     */
    public JWTData getUserJWTData(String email) {
        Optional<Users> maybeUser = Optional.ofNullable(db.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOneInto(Users.class));

        if (maybeUser.isPresent()) {
            Users user = maybeUser.get();
            return new JWTData(user.getId(), user.getPrivilegeLevel());
        } else {
            throw new UserDoesNotExistException(email);
        }
    }

    /**
     * Returns true if the given username and password correspond to a user in the USER table and
     * false otherwise.
     */
    public boolean isValidLogin(String email, String pass) {
        Optional<Users> maybeUser = Optional.ofNullable(db
            .selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOneInto(Users.class));

        return maybeUser
            .filter(user -> Passwords.isExpectedPassword(pass, user.getPassHash()))
            .isPresent();
    }

    /**
     * TODO: Refactor this method to take in a DTO / POJO instance
     * Creates a new row in the USER table with the given values.
     *
     * @throws EmailAlreadyInUseException if the given username and email are already used in the USER table.
     */
    public void createNewUser(String username, String email, String password, String firstName, String lastName) {
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

        // TODO: Send verification email
    }

    /**
     * Given a JWT signature, store it in the BLACKLISTED_REFRESHES table.
     */
    public void addToBlackList(String signature) {
        Timestamp expirationTimestamp = Timestamp.from(Instant.now().plusMillis(msRefreshExpiration));
        db.newRecord(Tables.BLACKLISTED_REFRESHES)
            .values(signature, expirationTimestamp)
            .store();
    }

    /**
     * Given a JWT signature return true if it is stored in the BLACKLISTED_REFRESHES table.
     */
    public boolean isOnBlackList(String signature) {
        return db.fetchExists(
            Tables.BLACKLISTED_REFRESHES
                .where(Tables.BLACKLISTED_REFRESHES.REFRESH_HASH.eq(signature)));
    }

    /**
     * Validates the email/secret key for the user it was created for.
     *
     * @throws InvalidEmailVerificationTokenException if the given token does not exist.
     * @throws ExpiredEmailVerificationTokenException if the given token is expired.
     */
    public void validateSecretKey(String secretKey) {
        VerificationKeysRecord verificationKey = db.selectFrom(Tables.VERIFICATION_KEYS)
            .where(Tables.VERIFICATION_KEYS.ID.eq(secretKey)
                .and(Tables.VERIFICATION_KEYS.USED.eq(false)))
            .fetchOneInto(VerificationKeysRecord.class);

        if (verificationKey == null) {
            throw new InvalidEmailVerificationTokenException();
        }

        if (!isTokenDateValid(verificationKey)) {
            throw new ExpiredEmailVerificationTokenException();
        }

        verificationKey.setUsed(true);
        verificationKey.store();
        db.update(USERS)
            .set(USERS.EMAIL_VERIFIED, true)
            .where(USERS.ID.eq(verificationKey.getUserId()))
            .execute();
    }

    /**
     * TODO: This method should be called as part of sign-up flow
     *
     * Given a userId and token, stores the token in the verification_keys table for the user.
     *
     * @throws UserDoesNotExistException if given userId does not match a user.
     */
    public String createSecretKey(int userId) {
        if (!db.fetchExists(USERS.where(USERS.ID.eq(userId)))) {
            throw new UserDoesNotExistException(userId);
        }

        String token = Passwords.generateRandomToken(50);

        VerificationKeysRecord keysRecord = db.newRecord(Tables.VERIFICATION_KEYS);
        keysRecord.setId(token);
        keysRecord.setUserId(userId);
        keysRecord.store();

        return token;
    }

    /**
     * Determines if given token date is still valid.
     *
     * @return true if it is within the time specified in the expiration.properties file.
     */
    private boolean isTokenDateValid(VerificationKeysRecord tokenResult) {
        Timestamp cutoffDate = Timestamp.from(Instant.now().minusSeconds(secondsVerificationEmailValid));
        return tokenResult.getCreated().after(cutoffDate);
    }
}
