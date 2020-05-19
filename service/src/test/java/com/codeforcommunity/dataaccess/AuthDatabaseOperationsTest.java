//package com.codeforcommunity.dataaccess;
//
//import com.codeforcommunity.JooqMock;
//import com.codeforcommunity.auth.JWTData;
//import com.codeforcommunity.auth.Passwords;
//import com.codeforcommunity.dto.userEvents.components.*;
//import com.codeforcommunity.dto.userEvents.requests.*;
//import com.codeforcommunity.dto.userEvents.responses.*;
//
//import com.codeforcommunity.enums.PrivilegeLevel;
//import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
//import com.codeforcommunity.exceptions.UserDoesNotExistException;
//import java.util.ArrayList;
//import org.jooq.generated.Tables;
//import org.jooq.generated.tables.records.BlacklistedRefreshesRecord;
//import org.jooq.generated.tables.records.EventsRecord;
//import org.jooq.generated.tables.records.UsersRecord;
//import org.jooq.impl.UpdatableRecordImpl;
//import org.junit.jupiter.api.BeforeAll;
//import org.mockito.Mockito;
//
//import static org.mockito.Mockito.when;
//import static org.junit.jupiter.api.Assertions.*;
//
//import org.junit.jupiter.api.Test;
//
//// Contains tests for AuthDatabaseOperations.java
//public class AuthDatabaseOperationsTest {
//    JooqMock myJooqMock;
//    AuthDatabaseOperations myAuthDatabaseOperations;
//
//    // set up all the mocks
//    @BeforeAll
//    public void setup() {
//        this.myJooqMock = new JooqMock();
//        this.myAuthDatabaseOperations = new AuthDatabaseOperations(myJooqMock.getContext());
//    }
//
//    // proper exception is thrown when user doesn't exist in DB
//    @Test
//    public void testGetUserJWTData1() {
//        String myEmail = "kimin@example.com";
//
//        // no users in DB
//        myJooqMock.addReturn("SELECT", new ArrayList<UsersRecord>());
//
//        try {
//            myAuthDatabaseOperations.getUserJWTData(myEmail);
//            fail();
//        } catch (UserDoesNotExistException e) {
//            assertEquals(e.getIdentifierMessage(), "email = " + myEmail);
//        }
//    }
//
//    // works as expected when user does indeed exist
//    @Test
//    public void testGetUserJWTData2() {
//        String myEmail = "kimin@example.com";
//
//        // one user in DB
//        UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
//        newUser.setUsername("kiminusername");
//        myUser.setEmail(myEmail);
//        myUser.setId(1);
//        myUser.setPrivilegeLevel(PrivilegeLevel.GP);
//        myJooqMock.addReturn("SELECT", myUser);
//
//        JWTData userData = myAuthDatabaseOperations.getUserJWTData(myEmail);
//
//        assertEquals(userData.getUserId(), myUser.getId());
//        assertEquals(userData.getPrivilegeLevel(), myUser.getPrivilegeLevel());
//    }
//
//    // returns false for incorrect login
//    @Test
//    public void testIsValidLogin1() {
//        String myEmail = "kimin@example.com";
//
//        // one user in DB
//        UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
//        newUser.setUsername("kiminusername");
//        myUser.setEmail(myEmail);
//        myUser.setPassHash(Passwords.createHash("password"));
//        myUser.setId(1);
//        myUser.setPrivilegeLevel(PrivilegeLevel.GP);
//        myJooqMock.addReturn("SELECT", myUser);
//
//        assertFalse(myAuthDatabaseOperations.isValidLogin(myEmail, "wrongPassword"));
//    }
//
//    // returns true for correct login
//    @Test
//    public void testIsValidLogin2() {
//        String myEmail = "kimin@example.com";
//
//        // one user in DB
//        UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
//        newUser.setUsername("kiminusername");
//        myUser.setEmail(myEmail);
//        myUser.setPassHash(Passwords.createHash("password"));
//        myUser.setId(1);
//        myUser.setPrivilegeLevel(PrivilegeLevel.GP);
//        myJooqMock.addReturn("SELECT", myUser);
//
//        assertTrue(myAuthDatabaseOperations.isValidLogin(myEmail, "password"));
//    }
//
//    // creating a new user fails when the email is already in use
//    @Test
//    public void testCreateNewUser1() {
//        String myEmail = "kimin@example.com";
//
//        // one user in DB
//        UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
//        newUser.setUsername("kiminusername");
//        myUser.setEmail(myEmail);
//        myUser.setPassHash(Passwords.createHash("password"));
//        myUser.setId(1);
//        myUser.setPrivilegeLevel(PrivilegeLevel.GP);
//        myJooqMock.addReturn("SELECT", myUser);
//
//        try {
//            myAuthDatabaseOperations.createNewUser("diffusername", myEmail, "password", "Kimin", "Lee");
//            fail();
//        } catch (EmailAlreadyInUseException e) {
//            assertEquals(e.getEmail(), "kimin@example.com");
//        }
//    }
//
//    // creating a new user fails when the username is already in use
//    @Test
//    public void testCreateNewUser2() {
//        String myEmail = "kimin@example.com";
//        String myUsername = "kiminusername";
//
//        // one user in DB
//        UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
//        newUser.setUsername(myUsername);
//        myUser.setEmail(myEmail);
//        myUser.setPassHash(Passwords.createHash("password"));
//        myUser.setId(1);
//        myUser.setPrivilegeLevel(PrivilegeLevel.GP);
//        myJooqMock.addReturn("SELECT", myUser);
//
//        try {
//            myAuthDatabaseOperations.createNewUser(myUsername, "diff@example.com", "password", "Kimin", "Lee");
//            fail();
//        } catch (EmailAlreadyInUseException e) {
//            assertEquals(e.getEmail(), "kimin@example.com");
//        }
//    }
//
//    // creating a new user succeeds when the email and username isn't already in use
//    @Test
//    public void testCreateNewUser3() {
//        // no users in DB
//        myJooqMock.addReturn("SELECT", new ArrayList<UsersRecord>());
//
//        myAuthDatabaseOperations.createNewUser("connerusername", "conner@example.com", "password", "Conner", "Nilsen");
//    }
//
//    // creating a new user succeeds when the email and username isn't already in use
//    @Test
//    public void testCreateNewUser3() {
//        // no users in DB
//        myJooqMock.addReturn("SELECT", new ArrayList<UsersRecord>());
//
//        myAuthDatabaseOperations.createNewUser("connerusername", "conner@example.com", "password", "Conner", "Nilsen");
//    }
//
//}