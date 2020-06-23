package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.BLOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.dto.team.*;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.*;
import com.codeforcommunity.requester.Emailer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.jooq.*;
import org.jooq.exception.*;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.impl.DSL;
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

  void createUser() {
    UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
    myUser.setUsername("kiminusername");
    myUser.setEmail("kimin@example.com");
    myUser.setPassHash(Passwords.createHash("pwpw"));
    myUser.setEmailVerified(true);
    myUser.setFirstName("Kimin");
    myUser.setLastName("Lee");
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
    createUser();
    team1();
    mockDb.addEmptyReturn("INSERT");

    Timestamp timestamp = Timestamp.valueOf("2020-05-30 02:00:55.939");
    TeamInvitationRequest ex1 = new TeamInvitationRequest("ex1", "ex1@example.com");
    TeamInvitationRequest ex2 = new TeamInvitationRequest("ex2", "ex2@example.com");
    TeamInvitationRequest ex3 = new TeamInvitationRequest("ex3", "ex3@example.com");
    List<TeamInvitationRequest> emailList = Arrays.asList(ex1, ex2, ex3);
    CreateTeamRequest teamRequest =
        new CreateTeamRequest("teamName", "teamBio", 2, timestamp, emailList);

    Record5<Integer, String, BigDecimal, BigDecimal, TeamRole> myUserRecord5 =
        mockDb
            .getContext()
            .newRecord(
                Tables.USERS.ID,
                Tables.USERS.USERNAME,
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.DONE), 1).else_(0))
                    .as("blocksCompleted"),
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.RESERVED), 1).else_(0))
                    .as("blocksReserved"),
                Tables.USER_TEAM.TEAM_ROLE);
    myUserRecord5.values(1, "kiminusername", new BigDecimal(1), new BigDecimal(1), TeamRole.LEADER);
    mockDb.addReturn("SELECT", myUserRecord5);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.createTeam(jwtData, teamRequest);

    assertEquals(3, mockDb.timesCalled("SELECT"));
    assertEquals(2, mockDb.timesCalled("INSERT"));

    ArgumentCaptor<String> stringArgs = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Users> usersArgs = ArgumentCaptor.forClass(Users.class);
    ArgumentCaptor<Team> teamArgs = ArgumentCaptor.forClass(Team.class);
    verify(emailer, times(3))
        .sendInviteEmail(
            stringArgs.capture(), stringArgs.capture(), usersArgs.capture(), teamArgs.capture());

    // Information on invitation request. The email it is sent to, and the name of the email owner /
    // name of the person
    // who received the email
    List<String> capturedStrings = stringArgs.getAllValues();
    assertEquals("ex1@example.com", capturedStrings.get(0));
    assertEquals("ex1", capturedStrings.get(1));
    assertEquals("ex2@example.com", capturedStrings.get(2));
    assertEquals("ex2", capturedStrings.get(3));
    assertEquals("ex3@example.com", capturedStrings.get(4));
    assertEquals("ex3", capturedStrings.get(5));

    // Inviter information
    List<Users> capturedUsers = usersArgs.getAllValues();
    assertEquals("Kimin", capturedUsers.get(0).getFirstName());
    assertEquals("Lee", capturedUsers.get(0).getLastName());
    assertEquals("kimin@example.com", capturedUsers.get(0).getEmail());
    assertEquals("kiminusername", capturedUsers.get(0).getUsername());
    assertEquals(1, capturedUsers.get(0).getId());
    assertEquals(PrivilegeLevel.STANDARD, capturedUsers.get(0).getPrivilegeLevel());
    assertEquals(true, capturedUsers.get(0).getEmailVerified());
    assertEquals("Kimin", capturedUsers.get(1).getFirstName());
    assertEquals("Lee", capturedUsers.get(1).getLastName());
    assertEquals("kimin@example.com", capturedUsers.get(1).getEmail());
    assertEquals("kiminusername", capturedUsers.get(1).getUsername());
    assertEquals(1, capturedUsers.get(1).getId());
    assertEquals(PrivilegeLevel.STANDARD, capturedUsers.get(1).getPrivilegeLevel());
    assertEquals(true, capturedUsers.get(1).getEmailVerified());
    assertEquals("Kimin", capturedUsers.get(2).getFirstName());
    assertEquals("Lee", capturedUsers.get(2).getLastName());
    assertEquals("kimin@example.com", capturedUsers.get(2).getEmail());
    assertEquals("kiminusername", capturedUsers.get(2).getUsername());
    assertEquals(1, capturedUsers.get(2).getId());
    assertEquals(PrivilegeLevel.STANDARD, capturedUsers.get(2).getPrivilegeLevel());
    assertEquals(true, capturedUsers.get(2).getEmailVerified());

    // Information on the TeamRecord
    List<Team> capturedTeam = teamArgs.getAllValues();
    assertEquals("teamName", capturedTeam.get(0).getName());
    assertEquals("teamBio", capturedTeam.get(0).getBio());
    assertEquals(2, capturedTeam.get(0).getGoal());
    assertEquals(3, capturedTeam.get(0).getId());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(0).getGoalCompletionDate().toString());
    assertEquals("teamName", capturedTeam.get(1).getName());
    assertEquals("teamBio", capturedTeam.get(1).getBio());
    assertEquals(2, capturedTeam.get(1).getGoal());
    assertEquals(3, capturedTeam.get(1).getId());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(1).getGoalCompletionDate().toString());
    assertEquals("teamName", capturedTeam.get(2).getName());
    assertEquals("teamBio", capturedTeam.get(2).getBio());
    assertEquals(2, capturedTeam.get(2).getGoal());
    assertEquals(3, capturedTeam.get(2).getId());
    assertEquals("2020-05-30 02:00:55.939", capturedTeam.get(2).getGoalCompletionDate().toString());
  }

  // successfully leaves team
  @Test
  public void testLeaveTeam1() {
    createUser();
    processor.leaveTeam(jwtData, 5);
    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // UserNotOnTeamException for user not being in the team
  @Test
  public void testLeaveTeam2() {
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
    createUser();

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
    createUser();
    mockDb.addReturn("SELECT", myTeam);
    mockDb.addReturn("DELETE", myTeam);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.disbandTeam(jwtData, 5);
    assertEquals(2, mockDb.timesCalled("DELETE"));
    assertEquals(1, mockDb.timesCalled("SELECT"));
  }

  // TeamLeaderOnlyRouteException for user not being a leader
  @Test
  public void testDisbandTeam2() {
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
    createUser();

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

  // Returns 1 team
  @Test
  public void testGetAllTeams1() {
    Record4<Integer, String, Integer, TeamRole> myTeam =
        mockDb
            .getContext()
            .newRecord(
                Tables.TEAM.ID,
                Tables.TEAM.NAME,
                DSL.count().as("memberCount"),
                Tables.USER_TEAM.TEAM_ROLE);
    myTeam.values(5, "kiminTeam", 1, TeamRole.LEADER);
    mockDb.addReturn("SELECT", myTeam);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    GetAllTeamsResponse allTeamsResponse = processor.getAllTeams(jwtData);

    assertEquals(allTeamsResponse.getRowCount(), 1);

    //    List<TeamSummary> tsList = Arrays.asList(ts);
    assertEquals(5, allTeamsResponse.getTeams().get(0).getId());
    assertEquals(1, allTeamsResponse.getTeams().get(0).getMemberCount());
    assertEquals("kiminTeam", allTeamsResponse.getTeams().get(0).getName());
    assertEquals(TeamRole.LEADER, allTeamsResponse.getTeams().get(0).getUserTeamRole());
  }

  // Returns 3 teams
  @Test
  public void testGetAllTeams2() {
    Record4<Integer, String, Integer, TeamRole> myTeam =
        mockDb
            .getContext()
            .newRecord(
                Tables.TEAM.ID,
                Tables.TEAM.NAME,
                DSL.count().as("memberCount"),
                Tables.USER_TEAM.TEAM_ROLE);
    myTeam.values(5, "kiminTeam", 1, TeamRole.LEADER);

    Record4<Integer, String, Integer, TeamRole> myTeam2 =
        mockDb
            .getContext()
            .newRecord(
                Tables.TEAM.ID,
                Tables.TEAM.NAME,
                DSL.count().as("memberCount"),
                Tables.USER_TEAM.TEAM_ROLE);
    myTeam2.values(3, "connerTeam", 6, TeamRole.MEMBER);

    Record4<Integer, String, Integer, TeamRole> myTeam3 =
        mockDb
            .getContext()
            .newRecord(
                Tables.TEAM.ID,
                Tables.TEAM.NAME,
                DSL.count().as("memberCount"),
                Tables.USER_TEAM.TEAM_ROLE);
    myTeam3.values(7, "jackTeam", 4, TeamRole.LEADER);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    List<Record> records = Arrays.asList(myTeam, myTeam2, myTeam3);
    mockDb.addReturn("SELECT", records);
    GetAllTeamsResponse allTeamsResponse =
        processor.getAllTeams(new JWTData(1, PrivilegeLevel.STANDARD));

    assertEquals(3, allTeamsResponse.getRowCount());

    // team1
    assertEquals(5, allTeamsResponse.getTeams().get(0).getId());
    assertEquals(1, allTeamsResponse.getTeams().get(0).getMemberCount());
    assertEquals("kiminTeam", allTeamsResponse.getTeams().get(0).getName());
    assertEquals(TeamRole.LEADER, allTeamsResponse.getTeams().get(0).getUserTeamRole());

    // team2
    assertEquals(3, allTeamsResponse.getTeams().get(1).getId());
    assertEquals(6, allTeamsResponse.getTeams().get(1).getMemberCount());
    assertEquals("connerTeam", allTeamsResponse.getTeams().get(1).getName());
    assertEquals(TeamRole.MEMBER, allTeamsResponse.getTeams().get(1).getUserTeamRole());

    // team3
    assertEquals(7, allTeamsResponse.getTeams().get(2).getId());
    assertEquals(4, allTeamsResponse.getTeams().get(2).getMemberCount());
    assertEquals("jackTeam", allTeamsResponse.getTeams().get(2).getName());
    assertEquals(TeamRole.LEADER, allTeamsResponse.getTeams().get(2).getUserTeamRole());
  }

  // successfully return a single team
  @Test
  public void testGetSingleTeam1() {
    TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
    myTeam.setId(5);
    myTeam.setName("teamKimin");
    myTeam.setBio("bioTeam");
    myTeam.setGoal(2);
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    myTeam.setGoalCompletionDate(timestamp);
    mockDb.addReturn("SELECT", myTeam);

    // calls in getTeamMembers()
    Record5<Integer, String, BigDecimal, BigDecimal, TeamRole> myUserRecord5 =
        mockDb
            .getContext()
            .newRecord(
                Tables.USERS.ID,
                Tables.USERS.USERNAME,
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.DONE), 1).else_(0))
                    .as("blocksCompleted"),
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.RESERVED), 1).else_(0))
                    .as("blocksReserved"),
                Tables.USER_TEAM.TEAM_ROLE);
    myUserRecord5.values(1, "kiminusername", new BigDecimal(1), new BigDecimal(1), TeamRole.LEADER);
    mockDb.addReturn("SELECT", myUserRecord5);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    TeamResponse teamResponse = processor.getSingleTeam(jwtData, 5);

    // check for teamResponse
    assertEquals("teamKimin", teamResponse.getName());
    assertEquals(2, teamResponse.getGoal());
    assertEquals("bioTeam", teamResponse.getBio());
    assertEquals(5, teamResponse.getId());
    assertEquals(TeamRole.LEADER, teamResponse.getUserTeamRole());
    assertEquals(1, teamResponse.getBlocksReserved());
    assertEquals(1, teamResponse.getBlocksCompleted());
    assertEquals("kiminusername", teamResponse.getMembers().get(0).getUsername());
    assertEquals(TeamRole.LEADER, teamResponse.getMembers().get(0).getRole());
    assertEquals(1, teamResponse.getMembers().get(0).getBlocksCompleted());
    assertEquals(1, teamResponse.getMembers().get(0).getBlocksReserved());
    assertEquals(1, teamResponse.getMembers().get(0).getId());
  }

  // NoSuchTeamException
  @Test
  public void testGetSingleTeam2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.getSingleTeam(jwtData, 3);
      fail();
    } catch (NoSuchTeamException e) {
      assertEquals(e.getTeamId(), 3);
    }
  }

  @Test
  public void testGetUserTeams1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);

    mockDb.addReturn("SELECT", userTeamRecord);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.getUserTeams(jwtData);
    GetUserTeamsResponse userTeamsResponse = processor.getUserTeams(jwtData);

    assertEquals(5, userTeamsResponse.getTeams().get(0).getId());
    assertEquals(1, userTeamsResponse.getTeams().get(0).getBio());
    assertEquals("kiminTeam", userTeamsResponse.getTeams().get(0).getName());
    assertEquals(TeamRole.LEADER, userTeamsResponse.getTeams().get(0).getUserTeamRole());
    assertEquals(2, userTeamsResponse.getTeams().get(0).getGoal());
    assertEquals(TeamRole.LEADER, userTeamsResponse.getTeams().get(0).getBlocksCompleted());
    assertEquals(TeamRole.LEADER, userTeamsResponse.getTeams().get(0).getBlocksReserved());
    assertEquals(TeamRole.LEADER, userTeamsResponse.getTeams().get(0).getMembers());
  }

  @Test
  public void testTransferOwnership1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);

    assertEquals(1,2);
  }

  // TeamLeaderOnlyRouteException where currentLeaderTeam == null
  @Test
  public void testTransferOwnership2() {
    createUser();
    team1();
    TransferOwnershipRequest tor = new TransferOwnershipRequest(5, 1);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.transferOwnership(jwtData, tor);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 1);
    }
  }

  // TeamLeaderOnlyRouteException where currentLeaderTeam.getTeamRole() != TeamRole.LEADER
  @Test
  public void testTransferOwnership3() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.MEMBER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(2);
    mockDb.addReturn("SELECT", userTeamRecord);

    createUser();
    team1();
    TransferOwnershipRequest tor = new TransferOwnershipRequest(5, 2);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.transferOwnership(jwtData, tor);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 1);
    }
  }

  // UserDoesNotExistException
  @Test
  public void testTransferOwnership4() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);

    mockDb.addEmptyReturn("SELECT");
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    team1();
    TransferOwnershipRequest tor = new TransferOwnershipRequest(8, 10);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.transferOwnership(jwtData, tor);
      fail();
    } catch (UserDoesNotExistException e) {
      assertEquals(e.getIdentifierMessage(), "id = 10");
    }
  }

  // UserNotOnTeamException
  @Test
  public void testTransferOwnership5() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(9);
    userTeamRecord.setUserId(18);
    mockDb.addReturn("SELECT", userTeamRecord);

    createUser();
    mockDb.addEmptyReturn("SELECT");

    TransferOwnershipRequest tor = new TransferOwnershipRequest(9, 7);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.transferOwnership(jwtData, tor);
      fail();
    } catch (UserNotOnTeamException e) {
      assertEquals(e.getUserId(), 7);
      assertEquals(e.getTeamId(), 9);
    }
  }

  @Test
  public void testApproveTeamRequest1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.PENDING);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(3);
    mockDb.addReturn("SELECT", userTeamRecord);
    mockDb.addReturn("SELECT", userTeamRecord);

    jwtData = new JWTData(3, PrivilegeLevel.STANDARD);
    processor.approveTeamRequest(jwtData, 5, 3);

    assertEquals(2, mockDb.timesCalled("SELECT"));
  }

  // TeamLeaderOnlyRouteException
  @Test
  public void testApproveTeamRequest2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.approveTeamRequest(jwtData, 9, 2);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 9);
    }
  }

  // NoSuchTeamRequestException
  @Test
  public void testApproveTeamRequest3() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(3);
    mockDb.addReturn("SELECT", userTeamRecord);
    mockDb.addEmptyReturn("SELECT");
    jwtData = new JWTData(8, PrivilegeLevel.STANDARD);
    try {
      processor.approveTeamRequest(jwtData, 1, 8);
      fail();
    } catch (NoSuchTeamRequestException e) {
      assertEquals(e.getTeamId(), 1);
    }
  }

  // UserAlreadyOnTeamException
  @Test
  public void testApproveTeamRequest4() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(8);
    mockDb.addReturn("SELECT", userTeamRecord);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.approveTeamRequest(jwtData, 9, 2);
      fail();
    } catch (UserAlreadyOnTeamException e) {
      assertEquals(e.getTeamId(), 9);
    }
  }

  @Test
  public void testRejectTeamRequest1() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.PENDING);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(3);
    mockDb.addReturn("SELECT", userTeamRecord);
    mockDb.addReturn("SELECT", userTeamRecord);
    mockDb.addEmptyReturn("DELETE");

    jwtData = new JWTData(3, PrivilegeLevel.STANDARD);
    processor.rejectTeamRequest(jwtData, 5, 3);

    assertEquals(2, mockDb.timesCalled("SELECT"));
    assertEquals(1, mockDb.timesCalled("DELETE"));
  }

  // TeamLeaderOnlyRouteException
  @Test
  public void testRejectTeamRequest2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.rejectTeamRequest(jwtData, 9, 2);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 9);
    }
  }

  // NoSuchTeamRequestException
  @Test
  public void testRejectTeamRequest3() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(3);
    mockDb.addReturn("SELECT", userTeamRecord);
    mockDb.addEmptyReturn("SELECT");
    jwtData = new JWTData(8, PrivilegeLevel.STANDARD);
    try {
      processor.rejectTeamRequest(jwtData, 1, 8);
      fail();
    } catch (NoSuchTeamRequestException e) {
      assertEquals(e.getTeamId(), 1);
    }
  }

  // UserAlreadyOnTeamException
  @Test
  public void testRejectTeamRequest4() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(8);
    mockDb.addReturn("SELECT", userTeamRecord);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.rejectTeamRequest(jwtData, 9, 2);
      fail();
    } catch (UserAlreadyOnTeamException e) {
      assertEquals(e.getTeamId(), 9);
    }
  }

  @Test
  public void testApplyForTeam1() {
    team1();
    mockDb.addEmptyReturn("SELECT");
    mockDb.addEmptyReturn("INSERT");

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    processor.applyForTeam(jwtData, 5);

    assertEquals(2, mockDb.timesCalled("SELECT"));
    assertEquals(1, mockDb.timesCalled("INSERT"));
  }

  // NoSuchTeamException
  @Test
  public void testApplyForTeam2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.applyForTeam(jwtData, 3);
      fail();
    } catch (NoSuchTeamException e) {
      assertEquals(e.getTeamId(), 3);
    }
  }

  // UserAlreadyOnTeamException
  @Test
  public void testApplyForTeam3() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.LEADER);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.applyForTeam(jwtData, 5);
      fail();
    } catch (UserAlreadyOnTeamException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  // ExistingTeamRequestException
  @Test
  public void testApplyForTeam4() {
    UserTeamRecord userTeamRecord = mockDb.getContext().newRecord(Tables.USER_TEAM);
    userTeamRecord.setTeamRole(TeamRole.PENDING);
    userTeamRecord.setTeamId(5);
    userTeamRecord.setUserId(1);
    mockDb.addReturn("SELECT", userTeamRecord);
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    try {
      processor.applyForTeam(jwtData, 5);
      fail();
    } catch (ExistingTeamRequestException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }

  @Test
  public void testGetTeamApplicants1() {
    Record2<Integer, String> myTeam = mockDb.getContext().newRecord(Tables.USER_TEAM.USER_ID, Tables.USERS.USERNAME);
    myTeam.values(1, "kiminUsername");
    mockDb.addReturn("SELECT", myTeam);

    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
    List<TeamApplicant> teamApplicantList = processor.getTeamApplicants(jwtData, 5);
    assertEquals(1, teamApplicantList.get(0).getUserId());
    assertEquals("kiminUsername", teamApplicantList.get(0).getUsername());
  }

  // TeamLeaderOnlyRouteException
  @Test
  public void testGetTeamApplicants2() {
    jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

    try {
      processor.getTeamApplicants(jwtData, 5);
      fail();
    } catch (TeamLeaderOnlyRouteException e) {
      assertEquals(e.getTeamId(), 5);
    }
  }
}
