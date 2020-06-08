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
import com.codeforcommunity.dto.team.TeamApplicant;
import com.codeforcommunity.dto.team.TeamMember;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TeamSummary;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.ExistingTeamRequestException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.NoSuchTeamRequestException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.requester.Emailer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
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
    teamRequest.validate();

    TeamRecord teamRecord = db.newRecord(TEAM);
    teamRecord.setName(teamRequest.getName());
    teamRecord.setBio(teamRequest.getBio());
    teamRecord.setGoal(teamRequest.getGoal());
    teamRecord.setGoalCompletionDate(teamRequest.getGoalCompletionDate());
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
  public void applyForTeam(JWTData userData, int teamId) {
    // Check team exists
    // Check user isn't already on team or already has a pending application
    // Add user to team applicants table

    if (!db.fetchExists(TEAM, TEAM.ID.eq(teamId))) {
      throw new NoSuchTeamException(teamId);
    }

    int userId = userData.getUserId();
    Optional<UserTeamRecord> maybeUserTeam =
        Optional.ofNullable(
            db.selectFrom(USER_TEAM)
                .where(USER_TEAM.TEAM_ID.eq(teamId))
                .and(USER_TEAM.USER_ID.eq(userId))
                .fetchOne());

    if (maybeUserTeam.isPresent()) {
      if (maybeUserTeam.get().getTeamRole() == TeamRole.PENDING) {
        throw new ExistingTeamRequestException(userData.getUserId(), teamId);
      } else if (maybeUserTeam.get().getTeamRole() != TeamRole.NONE) {
        throw new UserAlreadyOnTeamException(userData.getUserId(), teamId);
      }
    }

    db.insertInto(USER_TEAM)
        .columns(USER_TEAM.TEAM_ID, USER_TEAM.USER_ID, USER_TEAM.TEAM_ROLE)
        .values(teamId, userId, TeamRole.PENDING)
        .execute();
  }

  @Override
  public List<TeamApplicant> getTeamApplicants(JWTData userData, int teamId) {
    // Check user is a team leader of this team
    // Get all applicant pojos for this team
    // Get info for each applicant

    if (!db.fetchExists(
        USER_TEAM,
        USER_TEAM
            .TEAM_ID
            .eq(teamId)
            .and(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER))
            .and(USER_TEAM.USER_ID.eq(userData.getUserId())))) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    List<TeamApplicant> applicants =
        db.select(USER_TEAM.USER_ID, USERS.USERNAME, USERS.FIRST_NAME, USERS.LAST_NAME)
            .from(USER_TEAM.join(USERS).onKey())
            .where(USER_TEAM.TEAM_ID.eq(teamId))
            .and(USER_TEAM.TEAM_ROLE.eq(TeamRole.PENDING))
            .fetchInto(TeamApplicant.class);
    return applicants;
  }

  @Override
  public void approveTeamRequest(JWTData userData, int teamId, int applicantId) {
    // Check user is a team leader of this team
    // Make sure this request exists for this team
    // Make sure the request's user exists
    // Add the user to the team
    // Delete the team application row

    if (!db.fetchExists(
        USER_TEAM,
        USER_TEAM
            .TEAM_ID
            .eq(teamId)
            .and(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER))
            .and(USER_TEAM.USER_ID.eq(userData.getUserId())))) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    UserTeamRecord applicantRecord =
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.TEAM_ID.eq(teamId))
            .and(USER_TEAM.USER_ID.eq(applicantId))
            .fetchOne();

    if (applicantRecord == null) {
      throw new NoSuchTeamRequestException(applicantId, teamId);
    }

    if (applicantRecord.getTeamRole() != TeamRole.PENDING) {
      throw new UserAlreadyOnTeamException(applicantId, teamId);
    }

    applicantRecord.setTeamRole(TeamRole.MEMBER);
    applicantRecord.store();
  }

  @Override
  public void rejectTeamRequest(JWTData userData, int teamId, int applicantId) {
    // Check user is a team leader of this team
    // Make sure this request exists for this team
    // Delete this request from team applicants table
    // Delete the team application row

    if (!db.fetchExists(
        USER_TEAM,
        USER_TEAM
            .TEAM_ID
            .eq(teamId)
            .and(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER))
            .and(USER_TEAM.USER_ID.eq(userData.getUserId())))) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    UserTeamRecord applicantRecord =
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.TEAM_ID.eq(teamId))
            .and(USER_TEAM.USER_ID.eq(applicantId))
            .fetchOne();

    if (applicantRecord == null) {
      throw new NoSuchTeamRequestException(applicantId, teamId);
    }

    if (applicantRecord.getTeamRole() != TeamRole.PENDING) {
      throw new UserAlreadyOnTeamException(applicantId, teamId);
    }

    applicantRecord.delete();
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

    if (userTeamRecord == null || userTeamRecord.getTeamRole() != TeamRole.LEADER) {
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
            .where(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER)
                .or(USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER)))
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
    TeamRole userTeamRole =
        teamMembers.stream()
            .filter(tm -> tm.getId() == userData.getUserId())
            .map(TeamMember::getRole)
            .findFirst()
            .orElse(TeamRole.NONE);
    teamMembers = teamMembers.stream()
        .filter(tm -> tm.getRole().equals(TeamRole.MEMBER)
            || tm.getRole().equals(TeamRole.LEADER))
        .collect(Collectors.toList());
    int doneBlocks =
        teamMembers.stream().map(Individual::getBlocksCompleted).reduce(0, Integer::sum);
    int reservedBlocks =
        teamMembers.stream().map(Individual::getBlocksReserved).reduce(0, Integer::sum);
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
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.USER_ID.eq(userdata.getUserId()))
            .and(USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER)
                .or(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER)))
            .fetch();

    for (UserTeamRecord record : userTeamRecords) {
      int teamid = record.getTeamId();
      ret.add(getSingleTeam(userdata, teamid));
    }

    return new GetUserTeamsResponse(ret);
  }

  private List<TeamMember> getTeamMembers(int teamId) {
    Result<?> userResult =
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

    for (Record record : userResult) {
      teamMembers.add(
          new TeamMember(
              record.getValue(USERS.ID),
              record.getValue(USERS.USERNAME),
              ((BigDecimal) record.getValue("blocksCompleted")).intValue(),
              ((BigDecimal) record.getValue("blocksReserved")).intValue(),
              record.getValue(USER_TEAM.TEAM_ROLE)));
    }
    return teamMembers;
  }
}
