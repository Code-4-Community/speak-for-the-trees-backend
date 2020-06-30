package com.codeforcommunity.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.dto.user.ChangePasswordRequest;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.WrongPasswordException;
import com.codeforcommunity.requester.Emailer;
import java.sql.Timestamp;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtectedUserProcessorImplTest {
  // the JooqMock to use for testing
  private JooqMock mockDb;
  // the ProcessorImpl to use for testing
  private ProtectedUserProcessorImpl processor;

  private Emailer emailer;

  /** Method to setup mockDb and processor. */
  @BeforeEach
  void setup() {
    this.mockDb = new JooqMock();
    emailer = mock(Emailer.class);
    this.processor = new ProtectedUserProcessorImpl(mockDb.getContext(), emailer);
  }

  // successfully deletes user when user is a member
  @Test
  void testDeleteUser1() {
    UserTeamRecord myUserTeam = mockDb.getContext().newRecord(Tables.USER_TEAM);
    myUserTeam.setUserId(1);
    myUserTeam.setTeamId(5);
    myUserTeam.setTeamRole(TeamRole.MEMBER);
    mockDb.addReturn("SELECT", myUserTeam);
    mockDb.addEmptyReturn("DELETE");

    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    this.processor.deleteUser(jwtData);
    assertEquals(1, mockDb.timesCalled("SELECT"));
    assertEquals(3, mockDb.timesCalled("DELETE"));
    // user_id
    assertEquals(1, mockDb.getSqlBindings().get("DELETE").get(0)[0]);
    // user_id from user_team
    assertEquals(1, mockDb.getSqlBindings().get("DELETE").get(1)[0]);
    // user_id from users
    assertEquals(1, mockDb.getSqlBindings().get("DELETE").get(2)[0]);
    // user_id from user_team
    assertEquals(1, mockDb.getSqlBindings().get("SELECT").get(0)[0]);
  }

  // successfully deletes user when user is a leader
  @Test
  void testDeleteUser2() {
    UserTeamRecord myUserTeam = mockDb.getContext().newRecord(Tables.USER_TEAM);
    myUserTeam.setUserId(1);
    myUserTeam.setTeamId(5);
    myUserTeam.setTeamRole(TeamRole.LEADER);
    mockDb.addReturn("SELECT", myUserTeam);
    mockDb.addEmptyReturn("DELETE");

    TeamRecord myTeamRecord = mockDb.getContext().newRecord(Tables.TEAM);
    myTeamRecord.setId(5);
    myTeamRecord.setName("Team 5");
    myTeamRecord.setBio("bio");
    myTeamRecord.setGoal(2);
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    myTeamRecord.setGoalCompletionDate(timestamp);
    mockDb.addReturn("SELECT", myTeamRecord);
    mockDb.addEmptyReturn("DELETE");

    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    this.processor.deleteUser(jwtData);
    assertEquals(1, mockDb.timesCalled("SELECT"));
    // user_id
    assertEquals(1, mockDb.getSqlBindings().get("DELETE").get(0)[0]);
    // user_id from user_team
    assertEquals(5, mockDb.getSqlBindings().get("DELETE").get(1)[0]);
    // user_id from user_team
    assertEquals(1, mockDb.getSqlBindings().get("SELECT").get(0)[0]);
  }

  // attempts to delete user when there are no users
  @Test
  void testDeleteUser3() {
    UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    mockDb.addEmptyReturn("SELECT");
    mockDb.addEmptyReturn("DELETE");

    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    this.processor.deleteUser(jwtData);
    assertEquals(1, mockDb.timesCalled("SELECT"));
    assertEquals(1, mockDb.timesCalled("DELETE"));
  }

  // UserDoesNotExistException because user does not exist
  @Test
  void testChangePassword1() {
    mockDb.addEmptyReturn("SELECT");
    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    // ChangePasswordRequest
    ChangePasswordRequest changePwRequest = new ChangePasswordRequest("password", "newPassword");
    try {
      this.processor.changePassword(jwtData, changePwRequest);
      fail();
    } catch (UserDoesNotExistException e) {
      assertEquals(e.getIdentifierMessage(), "id = 1");
    }

    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // WrongPasswordException because user inputted wrong current password
  @Test
  void testChangePassword2() {
    String myPw = "password";
    String wrongPw = "wrongPassword";

    UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash(myPw));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    mockDb.addReturn("SELECT", myUser);

    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    // ChangePasswordRequest
    ChangePasswordRequest changePwRequest = new ChangePasswordRequest(wrongPw, "newPassword");
    try {
      this.processor.changePassword(jwtData, changePwRequest);
      fail();
    } catch (WrongPasswordException e) {
      // exception doesn't have any data to test.
    }
  }

  // successfully changes password
  @Test
  void testChangePassword3() {
    String myPw = "password";
    String newPw = "newPassword";

    UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash(myPw));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    mockDb.addReturn("SELECT", myUser);
    byte[] currentPw = myUser.getPassHash();
    JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    // ChangePasswordRequest
    ChangePasswordRequest changePwRequest = new ChangePasswordRequest(myPw, newPw);
    mockDb.addReturn("UPDATE", myUser);
    this.processor.changePassword(jwtData, changePwRequest);
    myUser.setPassHash(Passwords.createHash(newPw));
    assertNotEquals(myUser.getPassHash(), currentPw);
  }
}
