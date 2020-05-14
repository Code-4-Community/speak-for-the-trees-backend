package com.codeforcommunity.processor;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamMember;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TeamSummary;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.requester.Emailer;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.UserTeam;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.impl.DSL;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;

public class TeamsProcessorImpl implements ITeamsProcessor {

  private final DSLContext db;
  private final Emailer emailer;

  public TeamsProcessorImpl(DSLContext db, Emailer emailer) {
    this.db = db;
    this.emailer = emailer;
  }

  @Override
  public TeamResponse createTeam(JWTData userData, CreateTeamRequest teamRequest) {
    teamRequest.validate();

    boolean userOnTeam = db.fetchExists(USER_TEAM, USER_TEAM.USER_ID.eq(userData.getUserId()));
    if (userOnTeam) {
      throw new UserAlreadyOnTeamException(userData.getUserId());
    }

    TeamRecord teamRecord = db.newRecord(TEAM);
    teamRecord.setName(teamRequest.getName());
    teamRecord.setBio(teamRequest.getBio());
    teamRecord.setGoal(teamRequest.getGoal());
    teamRecord.setGoalCompletionDate(teamRequest.getGoalCompletionDate());
    teamRecord.store();

    Users inviter = db.selectFrom(USERS)
        .where(USERS.ID.eq(userData.getUserId()))
        .fetchOneInto(Users.class);

    teamRequest.getInvites().forEach((invitationRequest) -> {
      emailer.sendInviteEmail(invitationRequest.getEmail(), invitationRequest.getName(), inviter,
          teamRecord.into(Team.class));
    });

    db.insertInto(USER_TEAM).columns(USER_TEAM.fields())
        .values(userData.getUserId(), teamRecord.getId(), TeamRole.LEADER)
        .execute();

    return null;//getTeam(teamRecord.getId());
  }

  @Override
  public void joinTeam(JWTData userData, int teamId) {
    boolean userOnTeam = db.fetchExists(USER_TEAM, USER_TEAM.USER_ID.eq(userData.getUserId()));
    if (userOnTeam) {
      throw new UserAlreadyOnTeamException(userData.getUserId());
    }

    Team teamPojo = db.selectFrom(TEAM).where(TEAM.ID.eq(teamId)).fetchOneInto(Team.class);
    if (teamPojo == null) {
      throw new NoSuchTeamException(teamId);
    }

    db.insertInto(USER_TEAM).columns(USER_TEAM.fields())
        .values(userData.getUserId(), teamPojo.getId(), TeamRole.MEMBER)
        .execute();
  }

  @Override
  public void leaveTeam(JWTData userData, int teamId) {
    UserTeamRecord userTeamRecord = db.selectFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .fetchOne();

    if (userTeamRecord == null) {
      // Maybe just ignore?
      throw new UserNotOnTeamException(userData.getUserId(), teamId);
    }

    if (userTeamRecord.getTeamRole() == TeamRole.LEADER) {
      throw new TeamLeaderExcludedRouteException(teamId);
    }

    db.deleteFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .execute();
  }

  @Override
  public void disbandTeam(JWTData userData, int teamId) {
    UserTeamRecord userTeamRecord = db.selectFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .fetchOne();

    if (userTeamRecord == null) {
      // Maybe just ignore?
      throw new UserNotOnTeamException(userData.getUserId(), teamId);
    }

    if (userTeamRecord.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    db.deleteFrom(USER_TEAM)
        .where(USER_TEAM.TEAM_ID.eq(teamId))
        .execute();

    db.deleteFrom(TEAM)
        .where(TEAM.ID.eq(teamId))
        .execute();
  }

  @Override
  public void kickFromTeam(JWTData userData, int teamId, int kickUserId) {
    UserTeamRecord userTeamRecord = db.selectFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .fetchOne();

    if (userTeamRecord == null || userTeamRecord.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    db.deleteFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(kickUserId))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .execute();
  }

  @Override
  public void inviteToTeam(JWTData userData, InviteMembersRequest inviteMembersRequest) {
    int teamId = inviteMembersRequest.getTeamId();
    UserTeamRecord userTeamRecord = db.selectFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .fetchOne();

    if (userTeamRecord == null || userTeamRecord.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    Users inviter = db.selectFrom(USERS)
        .where(USERS.ID.eq(userTeamRecord.getUserId()))
        .fetchOneInto(Users.class);

    Team inviterTeam = db.selectFrom(TEAM)
        .where(TEAM.ID.eq(userTeamRecord.getTeamId()))
        .fetchOneInto(Team.class);

    inviteMembersRequest.getEmails().forEach((email) -> {
      emailer.sendInviteEmail(email, "Team Member", inviter, inviterTeam);
    });
  }

  @Override
  public GetAllTeamsResponse getAllTeams() {
    List<Team> teamPojos = db.selectFrom(TEAM).fetchInto(Team.class);
    List<TeamSummary> teams = teamPojos.stream().map(
        team -> new TeamSummary(team.getId(), team.getName(), getTeamMembers(team.getId()).size()))
        .collect(Collectors.toList());
    return new GetAllTeamsResponse(teams, teams.size());
  }

  @Override
  public TeamResponse getSingleTeam(int teamId) {
    Team teamPojo = db.selectFrom(TEAM).where(TEAM.ID.eq(teamId)).fetchOneInto(Team.class);
    List<TeamMember> teamMembers = getTeamMembers(teamId);
    int doneBlocks = teamMembers.stream().map(Individual::getBlocksCompleted).reduce(0,
        Integer::sum);
    int reservedBlocks = teamMembers.stream().map(Individual::getBlocksReserved).reduce(0,
        Integer::sum);
    return new TeamResponse(
        teamPojo.getId(),
        teamPojo.getName(),
        teamPojo.getBio(),
        teamPojo.getGoal(),
        teamPojo.getGoalCompletionDate(),
        doneBlocks,
        reservedBlocks,
        teamMembers
    );
  }

  private List<TeamMember> getTeamMembers(int teamId) {
    List<UserTeam> userTeamPojos = db.selectFrom(USER_TEAM).where(USER_TEAM.TEAM_ID.eq(teamId))
        .fetchInto(UserTeam.class);
    return userTeamPojos.stream().map(this::userTeamPojoToTeamMember).collect(
        Collectors.toList());
  }

  private TeamMember userTeamPojoToTeamMember(UserTeam userTeamPojo) {
    int userId = userTeamPojo.getUserId();
    Users user = db.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOneInto(Users.class);
    return new TeamMember(
        userId,
        user.getUsername(),
        getBlocksForMember(userId, BlockStatus.DONE),
        getBlocksForMember(userId, BlockStatus.RESERVED),
        userTeamPojo.getTeamRole()
    );
  }

  private int getBlocksForMember(int userId, BlockStatus status) {
    return db.selectCount().from(BLOCK).where(BLOCK.ASSIGNED_TO.eq(userId))
        .and(BLOCK.STATUS.eq(status)).fetchOneInto(Integer.class);
  }
}
