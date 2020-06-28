package com.codeforcommunity.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.VerificationKeyType;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.ExpiredSecretKeyException;
import com.codeforcommunity.exceptions.InvalidSecretKeyException;
import com.codeforcommunity.exceptions.UsedSecretKeyException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import java.sql.Timestamp;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.records.BlacklistedRefreshesRecord;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.generated.tables.records.VerificationKeysRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Contains tests for AuthDatabaseOperations.java
public class AuthDatabaseOperationsTest {
  JooqMock myJooqMock;
  AuthDatabaseOperations myAuthDatabaseOperations;

  // set up all the mocks
  @BeforeEach
  public void setup() {
    this.myJooqMock = new JooqMock();
    this.myAuthDatabaseOperations = new AuthDatabaseOperations(myJooqMock.getContext());
  }

  // proper exception is thrown when user doesn't exist in DB
  @Test
  public void testGetUserJWTData1() {
    String myEmail = "kimin@example.com";

    // no users in DB
    myJooqMock.addEmptyReturn("SELECT");
    try {
      myAuthDatabaseOperations.getUserJWTData(myEmail);
      fail();
    } catch (UserDoesNotExistException e) {
      assertEquals(e.getIdentifierMessage(), "email = " + myEmail);
    }
  }

  // works as expected when user does indeed exist
  @Test
  public void testGetUserJWTData2() {
    String myEmail = "kimin@example.com";

    // one user in DB
    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail(myEmail);
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    JWTData userData = myAuthDatabaseOperations.getUserJWTData(myEmail);

    assertEquals(userData.getUserId(), myUser.getId());
    assertEquals(userData.getPrivilegeLevel(), myUser.getPrivilegeLevel());
  }

  // returns false for incorrect login
  @Test
  public void testIsValidLogin1() {
    String myEmail = "kimin@example.com";

    // one user in DB
    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail(myEmail);
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    assertFalse(myAuthDatabaseOperations.isValidLogin(myEmail, "wrongPassword"));
  }

  // returns true for correct login
  @Test
  public void testIsValidLogin2() {
    String myEmail = "kimin@example.com";

    // one user in DB
    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail(myEmail);
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    assertTrue(myAuthDatabaseOperations.isValidLogin(myEmail, "password"));
  }

  // creating a new user fails when the email is already in use
  @Test
  public void testCreateNewUser1() {
    String myEmail = "kimin@example.com";

    // one user in DB
    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail(myEmail);
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    try {
      myAuthDatabaseOperations.createNewUser("diffusername", myEmail, "password", "Kimin", "Lee");
      fail();
    } catch (EmailAlreadyInUseException e) {
      assertEquals(e.getEmail(), "kimin@example.com");
    }
  }

  // creating a new user fails when the username is already in use
  @Test
  public void testCreateNewUser2() {
    String myEmail = "kimin@example.com";
    String myUsername = "kiminusername";

    // one user in DB
    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername(myUsername);
    myUser.setEmail(myEmail);
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    try {
      myAuthDatabaseOperations.createNewUser(
          myUsername, "diff@example.com", "password", "Kimin", "Lee");
      fail();
    } catch (EmailAlreadyInUseException e) {
      assertEquals(e.getEmail(), "diff@example.com");
    }
  }

  // creating a new user succeeds when the email and username isn't already in use
  @Test
  public void testCreateNewUser3() {
    // no users in DB
    myJooqMock.addEmptyReturn("SELECT");

    UsersRecord returned =
        myAuthDatabaseOperations.createNewUser(
            "connerusername", "conner@example.com", "password", "Conner", "Nilsen");
    assertEquals("Conner", returned.getFirstName());
    assertEquals("Nilsen", returned.getLastName());
    assertEquals("conner@example.com", returned.getEmail());
    assertEquals("connerusername", returned.getUsername());
    assertEquals(2, myJooqMock.timesCalled("SELECT"));
  }

  @Test
  public void testValidateSecretKey1() {
    String secretKey = "secretKey";
    VerificationKeyType type = VerificationKeyType.VERIFY_EMAIL;

    VerificationKeysRecord myVerKeys = myJooqMock.getContext().newRecord(Tables.VERIFICATION_KEYS);
    myVerKeys.setId("id");
    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    myVerKeys.setCreated(timestamp);
    myVerKeys.setType(type);
    myVerKeys.setUsed(false);
    myVerKeys.setUserId(1);
    myJooqMock.addReturn("SELECT", myVerKeys);

    UsersRecord myUser = myJooqMock.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash("pwpw"));
    myUser.setEmailVerified(true);
    myUser.setFirstName("Kimin");
    myUser.setLastName("Lee");
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    myJooqMock.addReturn("SELECT", myUser);

    UsersRecord returned = myAuthDatabaseOperations.validateSecretKey(secretKey, type);
    assertEquals(3, myJooqMock.timesCalled("SELECT"));
    assertEquals("kiminusername", returned.getUsername());
    assertEquals("secretKey", returned.get(0).toString());
  }

  // InvalidSecretKeyException
  @Test
  public void testValidateSecretKey2() {
    String secretKey = "secretKey";
    VerificationKeyType type = VerificationKeyType.VERIFY_EMAIL;

    try {
      myAuthDatabaseOperations.validateSecretKey(secretKey, type);
      fail();
    } catch (InvalidSecretKeyException e) {
      assertEquals(type, e.getType());
    }
  }

  // UsedSecretKeyException
  @Test
  public void testValidateSecretKey3() {
    String secretKey = "secretKey";
    VerificationKeyType type = VerificationKeyType.VERIFY_EMAIL;

    VerificationKeysRecord myVerKeys = myJooqMock.getContext().newRecord(Tables.VERIFICATION_KEYS);
    myVerKeys.setId("id");
    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    myVerKeys.setCreated(timestamp);
    myVerKeys.setType(type);
    myVerKeys.setUsed(true);
    myVerKeys.setUserId(1);

    try {
      myAuthDatabaseOperations.validateSecretKey(secretKey, type);
      fail();
    } catch (UsedSecretKeyException e) {
      assertEquals(type, e.getType());
    }
  }

  // ExpiredSecretKeyException
  @Test
  public void testValidateSecretKey4() {
    String secretKey = "secretKey";
    VerificationKeyType type = VerificationKeyType.VERIFY_EMAIL;

    VerificationKeysRecord myVerKeys = myJooqMock.getContext().newRecord(Tables.VERIFICATION_KEYS);
    myVerKeys.setId("id");
    Timestamp timestamp = Timestamp.valueOf("2023-05-30 02:00:55.939");
    myVerKeys.setCreated(timestamp);
    myVerKeys.setType(type);
    myVerKeys.setUsed(true);
    myVerKeys.setUserId(1);
    try {
      myAuthDatabaseOperations.validateSecretKey(secretKey, type);
      fail();
    } catch (ExpiredSecretKeyException e) {
      assertEquals(type, e.getType());
    }
  }

  @Test
  public void testCreateSecretKey1() {
    VerificationKeyType type = VerificationKeyType.VERIFY_EMAIL;
    VerificationKeysRecord myVerKeys = myJooqMock.getContext().newRecord(Tables.VERIFICATION_KEYS);
    myVerKeys.setId("id");
    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    myVerKeys.setCreated(timestamp);
    myVerKeys.setType(type);
    myVerKeys.setUsed(false);
    myVerKeys.setUserId(1);
    myJooqMock.addReturn("UPDATE", myVerKeys);

    myAuthDatabaseOperations.createSecretKey(1, type);
    assertEquals(1, myJooqMock.timesCalled("UPDATE"));
  }

  @Test
  public void testAddToBlackList1() {
    String signature = "signature";

    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    BlacklistedRefreshesRecord myBlacklistedRecord = myJooqMock.getContext().newRecord(Tables.BLACKLISTED_REFRESHES);
    myBlacklistedRecord.setExpires(timestamp);
    myBlacklistedRecord.setRefreshHash("refreshHash");
    myJooqMock.addReturn("INSERT", myBlacklistedRecord);

    myAuthDatabaseOperations.addToBlackList(signature);
    assertEquals(1, myJooqMock.timesCalled("INSERT"));
  }

  @Test
  public void testIsOnBlackList1() {
    String signature = "signature";

    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    BlacklistedRefreshesRecord myBlacklistedRecord = myJooqMock.getContext().newRecord(Tables.BLACKLISTED_REFRESHES);
    myBlacklistedRecord.setExpires(timestamp);
    myBlacklistedRecord.setRefreshHash("refreshHash");
    myJooqMock.addReturn("INSERT", myBlacklistedRecord);

    myAuthDatabaseOperations.isOnBlackList(signature);
    assertEquals(1, myJooqMock.timesCalled("INSERT"));
  }
}
