package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.GetUserTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamMember;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TeamSummary;
import com.codeforcommunity.dto.team.TeamsExport;
import com.codeforcommunity.dto.team.TransferOwnershipRequest;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.AdminOnlyRouteException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.requester.Emailer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.impl.DSL;

public class TeamsProcessorImpl implements ITeamsProcessor {

  private final DSLContext db;
  private final Emailer emailer;

  public TeamsProcessorImpl(DSLContext db, Emailer emailer) {
    this.db = db;
    this.emailer = emailer;
  }

  @Override
  public TeamResponse createTeam(JWTData userData, CreateTeamRequest teamRequest) {
    TeamRecord teamRecord = db.newRecord(TEAM);
    teamRecord.setName(teamRequest.getName());
    teamRecord.setBio(teamRequest.getBio());
    teamRecord.setGoal(teamRequest.getGoal());
    teamRecord.setGoalCompletionDate(teamRequest.getGoalCompletionDate());
    teamRecord.setCreatedTimestamp(new Timestamp(System.currentTimeMillis()));
    teamRecord.store();

    Users inviter =
        db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOneInto(Users.class);

    teamRequest
        .getInvites()
        .forEach(
            (invitationRequest) -> {
              emailer.sendInviteEmail(
                  invitationRequest.getEmail(),
                  invitationRequest.getName(),
                  inviter,
                  teamRecord.into(Team.class));
            });

    db.insertInto(USER_TEAM)
        .columns(USER_TEAM.fields())
        .values(userData.getUserId(), teamRecord.getId(), TeamRole.LEADER)
        .execute();

    return getSingleTeam(userData, teamRecord.getId());
  }

  @Override
  public void joinTeam(JWTData userData, int teamId) {

    Team teamPojo = db.selectFrom(TEAM).where(TEAM.ID.eq(teamId)).fetchOneInto(Team.class);
    if (teamPojo == null) {
      throw new NoSuchTeamException(teamId);
    }

    boolean alreadyOnTeam =
        db.fetchExists(
            db.selectFrom(USER_TEAM)
                .where(USER_TEAM.TEAM_ID.eq(teamId))
                .and(USER_TEAM.USER_ID.eq(userData.getUserId())));

    if (alreadyOnTeam) {
      throw new UserAlreadyOnTeamException(userData.getUserId(), teamId);
    }

    db.insertInto(USER_TEAM)
        .columns(USER_TEAM.fields())
        .values(userData.getUserId(), teamPojo.getId(), TeamRole.MEMBER)
        .execute();
  }

  @Override
  public void leaveTeam(JWTData userData, int teamId) {
    UserTeamRecord userTeamRecord =
        db.selectFrom(USER_TEAM)
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
    UserTeamRecord userTeamRecord =
        db.selectFrom(USER_TEAM)
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

    db.deleteFrom(USER_TEAM).where(USER_TEAM.TEAM_ID.eq(teamId)).execute();

    db.deleteFrom(TEAM).where(TEAM.ID.eq(teamId)).execute();
  }

  @Override
  public void kickFromTeam(JWTData userData, int teamId, int kickUserId) {
    UserTeamRecord userTeamRecord =
        db.selectFrom(USER_TEAM)
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
    UserTeamRecord userTeamRecord =
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.USER_ID.eq(userData.getUserId()))
            .and(USER_TEAM.TEAM_ID.eq(teamId))
            .fetchOne();

    if (userTeamRecord == null || userTeamRecord.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    Users inviter =
        db.selectFrom(USERS)
            .where(USERS.ID.eq(userTeamRecord.getUserId()))
            .fetchOneInto(Users.class);

    Team inviterTeam =
        db.selectFrom(TEAM).where(TEAM.ID.eq(userTeamRecord.getTeamId())).fetchOneInto(Team.class);

    inviteMembersRequest
        .getEmails()
        .forEach(
            (email) -> {
              emailer.sendInviteEmail(email, "Team Member", inviter, inviterTeam);
            });
  }

  @Override
  public GetAllTeamsResponse getAllTeams(JWTData userData) {
    Field<TeamRole> userTeamRole =
        db.select(USER_TEAM.TEAM_ROLE)
            .from(USER_TEAM)
            .where(USER_TEAM.TEAM_ID.eq(TEAM.ID))
            .and(USER_TEAM.USER_ID.eq(userData.getUserId()))
            .asField("userTeamRole");

    List<TeamSummary> teams =
        db.select(TEAM.ID, TEAM.NAME, DSL.count().as("memberCount"), userTeamRole)
            .from(TEAM)
            .innerJoin(USER_TEAM)
            .on(TEAM.ID.eq(USER_TEAM.TEAM_ID))
            .groupBy(TEAM.ID)
            .orderBy(TEAM.NAME.asc())
            .fetchInto(TeamSummary.class);
    return new GetAllTeamsResponse(teams, teams.size());
  }

  @Override
  public TeamResponse getSingleTeam(JWTData userData, int teamId) {
    Team teamPojo = db.selectFrom(TEAM).where(TEAM.ID.eq(teamId)).fetchOneInto(Team.class);
    if (teamPojo == null) {
      throw new NoSuchTeamException(teamId);
    }
    List<TeamMember> teamMembers = getTeamMembers(teamId);
    int doneBlocks =
        teamMembers.stream().map(Individual::getBlocksCompleted).reduce(0, Integer::sum);
    int reservedBlocks =
        teamMembers.stream().map(Individual::getBlocksReserved).reduce(0, Integer::sum);
    TeamRole userTeamRole =
        teamMembers.stream()
            .filter(tm -> tm.getId() == userData.getUserId())
            .map(TeamMember::getRole)
            .findFirst()
            .orElse(TeamRole.NONE);
    return new TeamResponse(
        teamPojo.getId(),
        teamPojo.getName(),
        teamPojo.getBio(),
        teamPojo.getGoal(),
        teamPojo.getGoalCompletionDate(),
        doneBlocks,
        reservedBlocks,
        userTeamRole,
        teamMembers);
  }

  @Override
  public GetUserTeamsResponse getUserTeams(JWTData userdata) {
    List<TeamResponse> ret = new ArrayList<TeamResponse>();
    List<UserTeamRecord> userTeamRecords =
        db.selectFrom(USER_TEAM).where(USER_TEAM.USER_ID.eq(userdata.getUserId())).fetch();

    for (UserTeamRecord record : userTeamRecords) {
      int teamid = record.getTeamId();
      ret.add(getSingleTeam(userdata, teamid));
    }

    return new GetUserTeamsResponse(ret);
  }

  @Override
  public String getAllTeamsForExport(JWTData userData) {
    if (userData.getPrivilegeLevel() != PrivilegeLevel.ADMIN) {
      throw new AdminOnlyRouteException();
    }

    List<TeamsExport> teams = db.selectFrom(TEAM).fetchInto(TeamsExport.class);

    List<TeamsExport> users =
        db.select(
                USERS.ID,
                USER_TEAM.TEAM_ID,
                USER_TEAM.TEAM_ROLE,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USERS.USERNAME,
                USERS.EMAIL,
                USERS.PRIVILEGE_LEVEL,
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.RESERVED), 1).otherwise(0))
                    .as("blocksReserved"),
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.DONE), 1).otherwise(0))
                    .as("blocksCompleted"))
            .from(USER_TEAM)
            .join(USERS)
            .on(USERS.ID.eq(USER_TEAM.USER_ID))
            .leftJoin(BLOCK)
            .on(USERS.ID.eq(BLOCK.ASSIGNED_TO))
            .groupBy(
                USERS.ID,
                USER_TEAM.TEAM_ID,
                USER_TEAM.TEAM_ROLE,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USERS.USERNAME,
                USERS.EMAIL,
                USERS.PRIVILEGE_LEVEL)
            .orderBy(USER_TEAM.TEAM_ID)
            .fetchInto(TeamsExport.class);

    teams.addAll(users);
    teams.sort(Comparator.comparing(TeamsExport::getTeamId));

    StringBuilder builder = new StringBuilder();
    builder.append(TeamsExport.getHeaderCSV());
    for (TeamsExport export : teams) {
      builder.append(export.getRowCSV());
    }

    return builder.toString();
  }

  private List<TeamMember> getTeamMembers(int teamId) {
    Result<Record5<Integer, String, BigDecimal, BigDecimal, TeamRole>> userResult =
        db.select(
                USERS.ID,
                USERS.USERNAME,
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.DONE), 1).else_(0))
                    .as("blocksCompleted"),
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.RESERVED), 1).else_(0))
                    .as("blocksReserved"),
                USER_TEAM.TEAM_ROLE)
            .from(USERS)
            .innerJoin(USER_TEAM)
            .on(USERS.ID.eq(USER_TEAM.USER_ID))
            .fullJoin(BLOCK)
            .on(USER_TEAM.USER_ID.eq(BLOCK.ASSIGNED_TO))
            .where(USER_TEAM.TEAM_ID.eq(teamId))
            .groupBy(USERS.ID, USERS.USERNAME, USER_TEAM.TEAM_ROLE)
            .orderBy(USERS.USERNAME)
            .fetch();

    List<TeamMember> teamMembers = new ArrayList<>();

    for (Record5<Integer, String, BigDecimal, BigDecimal, TeamRole> record : userResult) {
      teamMembers.add(
          new TeamMember(
              record.getValue(USERS.ID),
              record.getValue(USERS.USERNAME),
              record.value3().intValue(),
              record.value4().intValue(),
              record.getValue(USER_TEAM.TEAM_ROLE)));
    }
    return teamMembers;
  }

  @Override
  public void transferOwnership(JWTData userData, TransferOwnershipRequest request) {
    int currentLeaderId = userData.getUserId();
    int newLeaderId = request.getNewLeaderId();
    int teamId = request.getTeamId();

    UserTeamRecord currentLeaderTeam =
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.USER_ID.eq(currentLeaderId))
            .and(USER_TEAM.TEAM_ID.eq(teamId))
            .fetchOneInto(UserTeamRecord.class);

    if (currentLeaderTeam == null || currentLeaderTeam.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(currentLeaderId);
    }

    boolean newOwnerExists = db.fetchExists(db.selectFrom(USERS).where(USERS.ID.eq(newLeaderId)));

    if (!newOwnerExists) {
      throw new UserDoesNotExistException(request.getNewLeaderId());
    }

    boolean userOnTeam =
        db.fetchExists(
            db.selectFrom(USER_TEAM)
                .where(USER_TEAM.USER_ID.eq(newLeaderId))
                .and(USER_TEAM.TEAM_ID.eq(teamId)));

    if (!userOnTeam) {
      throw new UserNotOnTeamException(newLeaderId, request.getTeamId());
    }

    db.update(USER_TEAM)
        .set(USER_TEAM.TEAM_ROLE, TeamRole.MEMBER)
        .where(USER_TEAM.USER_ID.eq(currentLeaderId))
        .and(USER_TEAM.TEAM_ID.eq(request.getTeamId()))
        .execute();
    db.update(USER_TEAM)
        .set(USER_TEAM.TEAM_ROLE, TeamRole.LEADER)
        .where(USER_TEAM.USER_ID.eq(newLeaderId))
        .and(USER_TEAM.TEAM_ID.eq(request.getTeamId()))
        .execute();
  }
}
