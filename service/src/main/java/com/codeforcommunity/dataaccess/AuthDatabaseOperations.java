package com.codeforcommunity.dataaccess;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.processor.AuthProcessorImpl;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import org.jooq.DSLContext;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.UsersRecord;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.jooq.generated.Tables.USERS;

/**
 * Encapsulates all the database operations that are required for {@link AuthProcessorImpl}.
 */
public class AuthDatabaseOperations {

    private final DSLContext db;

    public final int MS_REFRESH_EXPIRATION;

    public AuthDatabaseOperations(DSLContext db) {
        this.db = db;

        this.MS_REFRESH_EXPIRATION = Integer.parseInt(PropertiesLoader
            .getExpirationProperties().getProperty("ms_refresh_expiration"));
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
    public void createNewUser(String email, String password, String firstName, String lastName) {
        boolean emailUsed = db.fetchExists(db.selectFrom(USERS).where(USERS.EMAIL.eq(email)));
        if (emailUsed) {
            throw new EmailAlreadyInUseException(email);
        }

        UsersRecord newUser = db.newRecord(USERS);
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
        Timestamp expirationTimestamp = Timestamp.from(Instant.now().plusMillis(MS_REFRESH_EXPIRATION));
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
}
