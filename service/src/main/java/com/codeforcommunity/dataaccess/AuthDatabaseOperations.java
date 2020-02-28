package com.codeforcommunity.dataaccess;

import com.codeforcommunity.auth.AuthUtils;
import com.codeforcommunity.exceptions.CreateUserException;
import com.codeforcommunity.processor.AuthProcessorImpl;
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
    private AuthUtils sha;

    public AuthDatabaseOperations(DSLContext db) {
        this.sha = new AuthUtils();
        this.db = db;
    }

    /**
     * Returns true if the given username and password correspond to a user in the USER table and
     * false otherwise.
     */
    public boolean isValidLogin(String username, String pass) {
        Optional<Users> maybeUser = Optional.ofNullable(db
            .selectFrom(USERS)
            .where(USERS.USER_NAME.eq(username))
            .fetchOneInto(Users.class));

        return maybeUser
            .filter(userAccount -> sha.hash(pass).equals(userAccount.getPassHash()))
            .isPresent();
    }

    /**
     * TODO: Refactor this method to take in a DTO / POJO instance
     * Creates a new row in the USER table with the given values.
     *
     * @throws CreateUserException if the given username and email are already used in the USER table.
     */
    public void createNewUser(String username, String email, String password, String firstName, String lastName) {

        boolean emailUsed = db.fetchExists(db.selectFrom(USERS).where(USERS.EMAIL.eq(email)));
        boolean usernameUsed = db.fetchExists(db.selectFrom(USERS).where(USERS.USER_NAME.eq(username)));
        if (emailUsed || usernameUsed) {
            if (emailUsed && usernameUsed) {
                throw new CreateUserException(CreateUserException.UsedField.BOTH);
            } else if (emailUsed) {
                throw new CreateUserException(CreateUserException.UsedField.EMAIL);
            } else {
                throw new CreateUserException(CreateUserException.UsedField.USERNAME);
            }
        }

        String pass_hash = sha.hash(password);
        UsersRecord newUser = db.newRecord(USERS);
        newUser.setUserName(username);
        newUser.setEmail(email);
        newUser.setPassHash(pass_hash);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.store();
    }

    /**
     * Given a JWT signature, store it in the BLACKLISTED_REFRESHES table.
     */
    public void addToBlackList(String signature) {
        Timestamp expirationTimestamp = Timestamp.from(Instant.now().plusMillis(AuthUtils.refresh_exp));
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
