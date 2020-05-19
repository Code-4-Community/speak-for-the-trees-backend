package com.codeforcommunity.processor;

import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.processor.TeamsProcessorImpl;

import com.codeforcommunity.requester.Emailer;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.exceptions.MalformedParameterException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.enums.PrivilegeLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.codeforcommunity.auth.Passwords;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.TeamRecord;
import com.codeforcommunity.enums.TeamRole;
import org.jooq.generated.Tables;
import java.util.ArrayList;
import java.util.Arrays;
import java.sql.Timestamp;
import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TeamsProcessorImplTest {
    // the JooqMock to use for testing
    JooqMock mockDb;
    // the ProcessorImpl to use for testing
    TeamsProcessorImpl processor;

    JWTData jwtData;
    Emailer emailer;

    /**
     * Method to setup mockDb and processor.
     */
    void setup() {
        this.mockDb = new JooqMock();
        emailer = mock(Emailer.class);
        this.processor = new TeamsProcessorImpl(mockDb.getContext(), emailer);

        jwtData = null;
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
        setup();
        mockDb.addEmptyReturn("SELECT");
        mockDb.addEmptyReturn("UPDATE");
        mockDb.addEmptyReturn("DROP/CREATE");
        mockDb.addEmptyReturn("INSERT");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", 2, timestamp, emailList);

        jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        processor.createTeam(jwtData, teamRequest);
        assertEquals(2, mockDb.timesCalled("SELECT"));
        assertEquals(0, mockDb.timesCalled("UPDATE"));
        assertEquals(2, mockDb.timesCalled("INSERT"));
        assertEquals(0, mockDb.getSqlBindings().get("UNKNOWN").size());
        assertEquals(0, mockDb.getSqlBindings().get("DROP/CREATE").size());

        verify(emailer, times(3)).sendInviteEmail(anyString(),
                anyString(), any(Users.class), any(Team.class));
    }

    // UserAlreadyOnTeamException from user being already on the team
    @Test
    public void testCreateTeam2() {
        setup();
        userCreatingTeam();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", 1, timestamp, emailList);
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
        setup();
        mockDb.addEmptyReturn("SELECT");
        mockDb.addEmptyReturn("UPDATE");
        mockDb.addEmptyReturn("INSERT");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", -1, timestamp, emailList);

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
        setup();
        mockDb.addEmptyReturn("SELECT");
        mockDb.addEmptyReturn("UPDATE");
        mockDb.addEmptyReturn("INSERT");
        team1();

        jwtData = new JWTData(2, PrivilegeLevel.STANDARD);
        processor.joinTeam(jwtData, 5);

        assertEquals(2, mockDb.timesCalled("SELECT"));
        assertEquals(0, mockDb.timesCalled("UPDATE"));
        assertEquals(1, mockDb.timesCalled("INSERT"));
        assertEquals(0, mockDb.getSqlBindings().get("UNKNOWN").size());
        assertEquals(0, mockDb.getSqlBindings().get("DROP/CREATE").size());
    }

    // NoSuchTeamException
    @Test
    public void testJoinTeam2() {
        testCreateTeam1();

        try {
            processor.joinTeam(jwtData, 3);
            fail();
        } catch (NoSuchTeamException e) {
            assertEquals(e.getTeamId(), 3);
        }
    }

    // UserAlreadyOnTeamException
    @Test
    public void testJoinTeam3() {
        setup();
        userCreatingTeam();

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
        setup();
        UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
        myUser.setUsername("kiminusername");
        myUser.setEmail("kimin@example.com");
        myUser.setPassHash(Passwords.createHash("password"));
        myUser.setId(1);
        myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
        mockDb.addReturn("SELECT", myUser);
        jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        mockDb.addReturn("DELETE", myUser);
        processor.leaveTeam(jwtData, 5);

        assertEquals(1, mockDb.timesCalled("SELECT"));
        assertEquals(1, mockDb.timesCalled("DELETE"));
    }

    // UserNotOnTeamException for user not being in the team
    @Test
    public void testLeaveTeam2() {
        setup();
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
        setup();
        mockDb.addEmptyReturn("SELECT");
        mockDb.addEmptyReturn("UPDATE");
        mockDb.addEmptyReturn("INSERT");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", 1, timestamp, emailList);
        team1();

        jwtData = new JWTData(2, PrivilegeLevel.STANDARD);
        processor.createTeam(jwtData, teamRequest);
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
        setup();
        mockDb.addEmptyReturn("SELECT");
        mockDb.addEmptyReturn("UPDATE");
        mockDb.addEmptyReturn("DROP/CREATE");
        mockDb.addEmptyReturn("INSERT");

        TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
        myTeam.setId(5);
        myTeam.setName("teamKimin");
        myTeam.setBio("bioTeam");
        myTeam.setGoal(2);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        myTeam.setGoalCompletionDate(timestamp);

        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", 2, timestamp, emailList);

        mockDb.addReturn("SELECT", myTeam);
        mockDb.addReturn("DELETE", myTeam);

        jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        processor.createTeam(jwtData, teamRequest);
        processor.disbandTeam(jwtData, 5);
        assertEquals(2, mockDb.timesCalled("DELETE"));
        assertEquals(1, mockDb.timesCalled("SELECT"));
    }

    // UserNotOnTeamException for user not being on the team
    @Test
    public void testDisbandTeam2() {
        setup();
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
        setup();
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
        setup();
        TeamRecord myTeam = mockDb.getContext().newRecord(Tables.TEAM);
        myTeam.setId(5);
        myTeam.setName("teamKimin");
        myTeam.setBio("bioTeam");
        myTeam.setGoal(2);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        myTeam.setGoalCompletionDate(timestamp);
        mockDb.addReturn("SELECT", myTeam);
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        CreateTeamRequest teamRequest = new CreateTeamRequest("teamName", "teamBio", 2, timestamp, emailList);

        jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        processor.createTeam(jwtData, teamRequest);
        processor.kickFromTeam(jwtData, 5, 1);

        assertEquals(1, mockDb.timesCalled("DELETE"));
        assertEquals(1, mockDb.timesCalled("SELECT"));
    }

    // TeamLeaderOnlyRouteException
    @Test
    public void testKickFromTeam2() {
        setup();
        team1();
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
        setup();
        team1();
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
        InviteMembersRequest imr = new InviteMembersRequest(emailList, 5);
        jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        processor.inviteToTeam(jwtData, imr);

        assertEquals(3, mockDb.timesCalled("SELECT"));
        verify(emailer, times(3)).sendInviteEmail(anyString(),
                anyString(), any(Users.class), any(Team.class));
    }

    // TeamLeaderOnlyRouteException since user is not team leader
    @Test
    public void testInviteToTeam2() {
        setup();
        team1();
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
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
        setup();
        ArrayList<String> emailList = new ArrayList<String>(
                Arrays.asList("ex1@example.com", "ex2@example.com", "ex3@example.com"));
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