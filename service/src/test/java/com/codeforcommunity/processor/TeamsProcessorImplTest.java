package com.codeforcommunity.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamInvitationRequest;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.MalformedParameterException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.requester.Emailer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TeamsProcessorImplTest {
  // the JooqMock to use for testing
  JooqMock mockDb;
  // the ProcessorImpl to use for testing
  TeamsProcessorImpl processor;

  JWTData jwtData;
  Emailer emailer;

  /** Method to setup mockDb and processor. */
  @BeforeEach
  void setup() {
    mockDb = new JooqMock();
    emailer = mock(Emailer.class);
    processor = new TeamsProcessorImpl(mockDb.getContext(), emailer);
  }

  void userCreatingTeam() {
    UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash("password"));
    myUser.setId(1);
    myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
    mockDb.addReturn("SELECT", myUser);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
  }

  void team1() {
    TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
    myTeam.setId(5);
    myTeam.setName("teamKimin");
    myTeam.setBio("bioTeam");
    myTeam.setGoal(2);
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    myTeam.setGoalCompletionDate(timestamp);
    mockDb.addReturn("SELECT", myTeam);
  }

  // successfully create a team
  @Test
  public void testCreateTeam1() {
    mockDb.addEmptyReturn("SELECT");
    mockDb.addEmptyReturn("INSERT");

    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    TeamInvitationRequest ex1 = new TeamInvitationRequest("ex1", "ex1@example.com");
    TeamInvitationRequest ex2 = new TeamInvitationRequest("ex2", "ex2@example.com");
    TeamInvitationRequest ex3 = new TeamInvitationRequest("ex3", "ex3@example.com");
    List<TeamInvitationRequest> emailList = Arrays.asList(ex1, ex2, ex3);
    CreateTeamRequest teamRequest =
        new CreateTeamRequest("teamName", "teamBio", 2, timestamp, emailList);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.createTeam(jwtData, teamRequest);
    assertEquals(2, mockDb.timesCalled("SELECT"));
    assertEquals(2, mockDb.timesCalled("INSERT"));

    ArgumentCaptor<String> stringArgs = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Users> usersArgs = ArgumentCaptor.forClass(Users.class);
    ArgumentCaptor<Team> teamArgs = ArgumentCaptor.forClass(Team.class);
    verify(emailer, times(3))
        .sendInviteEmail(
            stringArgs.capture(), stringArgs.capture(), usersArgs.capture(), teamArgs.capture());

    List<String> capturedStrings = stringArgs.getAllValues();
    assertEquals("ex1@example.com", capturedStrings.get(0));
    assertEquals("Team Member", capturedStrings.get(1));
    assertEquals("ex2@example.com", capturedStrings.get(2));
    assertEquals("Team Member", capturedStrings.get(3));
    assertEquals("ex3@example.com", capturedStrings.get(4));
    assertEquals("Team Member", capturedStrings.get(5));
    List<Users> capturedUsers = usersArgs.getAllValues();
    // code should be fixed
    // assertEquals("test", capturedUsers.get(0));
    //        assertEquals("null", capturedUsers.get(1));
    //        assertEquals("null", capturedUsers.get(2));
    List<Team> capturedTeam = teamArgs.getAllValues();
    assertEquals("teamName", capturedTeam.get(0).getName());
    assertEquals("teamBio", capturedTeam.get(0).getBio());
    assertEquals(2, capturedTeam.get(0).getGoal());
    assertEquals(1, capturedTeam.get(0).getId());
    assertEquals(Team.class, capturedTeam.get(0).getClass());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(0).getGoalCompletionDate().toString());
    assertEquals("teamName", capturedTeam.get(1).getName());
    assertEquals("teamBio", capturedTeam.get(2).getBio());
    assertEquals(2, capturedTeam.get(1).getGoal());
    assertEquals(1, capturedTeam.get(1).getId());
    assertEquals(Team.class, capturedTeam.get(1).getClass());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(1).getGoalCompletionDate().toString());
    assertEquals("teamName", capturedTeam.get(2).getName());
    assertEquals("teamBio", capturedTeam.get(2).getBio());
    assertEquals(2, capturedTeam.get(2).getGoal());
    assertEquals(1, capturedTeam.get(2).getId());
    assertEquals(Team.class, capturedTeam.get(2).getClass());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(2).getGoalCompletionDate().toString());
  }

  // UserAlreadyOnTeamException from user being already on the team
  @Test
  public void testCreateTeam2() {
    team1();
    UserTeamRecord myUserTeam = mockDb.getContext().newRecord(Tables.USER_TEAM);
    myUserTeam.setUserId(1);
    myUserTeam.setTeamId(5);
    myUserTeam.setTeamRole(TeamRole.MEMBER);
    mockDb.addReturn("SELECT", myUserTeam);
    userCreatingTeam();

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    TeamInvitationRequest ex1 = new TeamInvitationRequest("ex1", "ex1@example.com");
    TeamInvitationRequest ex2 = new TeamInvitationRequest("ex2", "ex2@example.com");
    TeamInvitationRequest ex3 = new TeamInvitationRequest("ex3", "ex3@example.com");
    List<TeamInvitationRequest> emailList = Arrays.asList(ex1, ex2, ex3);
    CreateTeamRequest teamRequest =
        new CreateTeamRequest("teamName", "teamBio", 1, timestamp, emailList);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.createTeam(jwtData, teamRequest);
      fail();
    } catch (UserAlreadyOnTeamException e) {
      assertEquals(e.getUserId(), 1);
    }
  }

  // MalformedParameterException from false validation of teamRequest
  @Test
  public void testCreateTeam3() {
    mockDb.addEmptyReturn("SELECT");
    mockDb.addEmptyReturn("INSERT");
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    TeamInvitationRequest ex1 = new TeamInvitationRequest("ex1", "ex1@example.com");
    TeamInvitationRequest ex2 = new TeamInvitationRequest("ex2", "ex2@example.com");
    TeamInvitationRequest ex3 = new TeamInvitationRequest("ex3", "ex3@example.com");
    List<TeamInvitationRequest> emailList = Arrays.asList(ex1, ex2, ex3);
    CreateTeamRequest teamRequest =
        new CreateTeamRequest("teamName", "teamBio", -1, timestamp, emailList);

    try {
      processor.createTeam(jwtData, teamRequest);
      fail();
    } catch (MalformedParameterException e) {
      assertEquals(e.getParameterName(), "goal");
    }
  }

  // user successfully joins team
  @Test
  public void testJoinTeam1() {
    mockDb.addEmptyReturn("SELECT");
    mockDb.addEmptyReturn("UPDATE");
    mockDb.addEmptyReturn("INSERT");
    team1();

    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);
    processor.joinTeam(jwtData, 5);

    assertEquals(2, mockDb.timesCalled("SELECT"));
    assertEquals(1, mockDb.timesCalled("INSERT"));
  }

  // NoSuchTeamException
  @Test
  public void testJoinTeam2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.joinTeam(jwtData, 8);
      fail();
    } catch (NoSuchTeamException e) {
      assertEquals(e.getTeamId(), 8);
    }
  }

  // UserAlreadyOnTeamException
  @Test
  public void testJoinTeam3() {
    UserTeamRecord myUserTeam = mockDb.getContext().newRecord(Tables.USER_TEAM);
    myUserTeam.setUserId(1);
    myUserTeam.setTeamId(5);
    myUserTeam.setTeamRole(TeamRole.MEMBER);
    mockDb.addReturn("SELECT", myUserTeam);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.joinTeam(jwtData, 5);
      fail();
    } catch (UserAlreadyOnTeamException e) {
      assertEquals(e.getUserId(), 1);
    }
  }

  // successfully leaves team
  @Test
  public void testLeaveTeam1() {
    userCreatingTeam();
    processor.leaveTeam(jwtData, 5);
    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // UserNotOnTeamException for user not being in the team
  @Test
  public void testLeaveTeam2() {
    testCreateTeam1();
    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);
    try {
      processor.leaveTeam(jwtData, 5);
      fail();
    } catch (UserNotOnTeamException e) {
      assertEquals(e.getUserId(), 2);
      assertEquals(e.getTeamId(), 5);
    }
  }

  // TeamLeaderExcludedRouteException for the user being the team leader
  @Test
  public void testLeaveTeam3() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);
    userCreatingTeam();

    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);
    try {
      processor.leaveTeam(jwtData, 5);
      fail();
    } catch (TeamLeaderExcludedRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  // successfully disbands team
  @Test
  public void testDisbandTeam1() {
    TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
    myTeam.setId(5);
    myTeam.setName("teamKimin");
    myTeam.setBio("bioTeam");
    myTeam.setGoal(2);
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    myTeam.setGoalCompletionDate(timestamp);

    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);
    userCreatingTeam();
    mockDb.addReturn("SELECT", myTeam);
    mockDb.addReturn("DELETE", myTeam);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.disbandTeam(jwtData, 5);
    assertEquals(2, mockDb.timesCalled("DELETE"));
    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // UserNotOnTeamException for user not being on the team
  @Test
  public void testDisbandTeam2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.disbandTeam(jwtData, 5);
      fail();
    } catch (UserNotOnTeamException e) {
      assertEquals(e.getUserId(), 1);
      assertEquals(e.getTeamId(), 5);
    }
  }

  // TeamLeaderOnlyRouteException for user not being a leader
  @Test
  public void testDisbandTeam3() {
    TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
    myTeam.setId(5);
    myTeam.setName("teamKimin");
    myTeam.setBio("bioTeam");
    myTeam.setGoal(2);
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    myTeam.setGoalCompletionDate(timestamp);
    mockDb.addReturn("SELECT", myTeam);

    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);

    try {
      processor.disbandTeam(jwtData, 5);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  @Test
  public void testKickFromTeam1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.kickFromTeam(jwtData, 5, 1);

    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // TeamLeaderOnlyRouteException
  @Test
  public void testKickFromTeam2() {
    team1();
    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);

    try {
      processor.kickFromTeam(jwtData, 5, 1);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  // userTeamRecord == null
  @Test
  public void testKickFromTeam3() {
    jwtData = new JWTData(2, PrivilegeLevel.STANDARD);

    try {
      processor.kickFromTeam(jwtData, 5, 1);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  @Test
  public void testInviteToTeam1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);
    userCreatingTeam();

    List<String> emailList = Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com");
    InviteMembersRequest imr = new InviteMembersRequest(emailList, 5);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.inviteToTeam(jwtData, imr);

    assertEquals(3, mockDb.timesCalled("SELECT"));
    verify(emailer, times(3))
        .sendInviteEmail(anyString(), anyString(), any(Users.class), any(Team.class));
  }

  // TeamLeaderOnlyRouteException since user is not team leader
  @Test
  public void testInviteToTeam2() {
    team1();
    List<String> emailList = Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com");
    InviteMembersRequest imr = new InviteMembersRequest(emailList, 5);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.inviteToTeam(jwtData, imr);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  // TeamLeaderOnlyRouteException since team doesn't exist
  @Test
  public void testInviteToTeam3() {
    List<String> emailList = Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com");
    InviteMembersRequest imr = new InviteMembersRequest(emailList, 5);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.inviteToTeam(jwtData, imr);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }
}
