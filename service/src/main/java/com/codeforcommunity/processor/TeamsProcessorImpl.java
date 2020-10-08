package com.codeforcommunity.processor;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.team.AdminTeamSummary;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsAdminResponse;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.GetUserTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamApplicant;
import com.codeforcommunity.dto.team.TeamMember;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TeamSummary;
import com.codeforcommunity.dto.team.TeamsExport;
import com.codeforcommunity.dto.team.TransferOwnershipRequest;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.AdminOnlyRouteException;
import com.codeforcommunity.exceptions.ExistingTeamRequestException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.NoSuchTeamRequestException;
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
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record5;
import org.jooq.Result;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.AuditRecord;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.impl.DSL;

import static org.jooq.generated.Tables.*;

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("team");
    audit.setTransactionType("insert");
    audit.setResult(Integer.toString(teamRecord.getId()));
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

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

    UserTeamRecord userTeam = db.newRecord(USER_TEAM);
    userTeam.setUserId(userData.getUserId());
    userTeam.setTeamId(teamRecord.getId());
    userTeam.setTeamRole(TeamRole.LEADER);
    userTeam.store();

    AuditRecord audit2 = db.newRecord(AUDIT);
    audit2.setTableName("user_team");
    audit2.setTransactionType("insert");
    audit2.setResult(userData.getUserId() + ", " + userTeam.getTeamId());
    audit2.setUserId(userData.getUserId());
    audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit2.insert();

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

    AuditRecord audit2 = db.newRecord(AUDIT);
    audit2.setTableName("user_team");
    audit2.setTransactionType("insert");
    audit2.setResult(userData.getUserId() + ", " + teamId);
    audit2.setUserId(userData.getUserId());
    audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit2.insert();
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
        db.select(USER_TEAM.USER_ID, USERS.USERNAME)
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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("update");
    audit.setOldValue(applicantRecord.toString());

    applicantRecord.setTeamRole(TeamRole.MEMBER);
    applicantRecord.store();

    audit.setResult(applicantRecord.toString());
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();
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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("delete");
    audit.setResult(userData.getUserId() + ", " + teamId);
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("delete");
    audit.setResult(userData.getUserId() + ", " + teamId);
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("delete");
    audit.setResult(userData.getUserId() + ", " + teamId);
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

    db.deleteFrom(USER_TEAM).where(USER_TEAM.TEAM_ID.eq(teamId)).execute();

    AuditRecord audit2 = db.newRecord(AUDIT);
    audit2.setTableName("team");
    audit2.setTransactionType("delete");
    audit2.setResult(Integer.toString(teamId));
    audit2.setUserId(userData.getUserId());
    audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit2.insert();

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("delete");
    audit.setResult(kickUserId + ", " + teamId);
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

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
            .leftJoin(USER_TEAM)
            .on(TEAM.ID.eq(USER_TEAM.TEAM_ID))
            .where(
                USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER).or(USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER)))
            .groupBy(TEAM.ID)
            .orderBy(TEAM.NAME.asc())
            .fetchInto(TeamSummary.class);
    return new GetAllTeamsResponse(teams, teams.size());
  }

  @Override
  public GetAllTeamsAdminResponse getAllTeamsAdmin(JWTData userData) {
    if (userData.getPrivilegeLevel() != PrivilegeLevel.ADMIN) {
      throw new AdminOnlyRouteException();
    }
    List<AdminTeamSummary> teams =
        db.select(
                TEAM.ID,
                TEAM.NAME,
                TEAM.GOAL_COMPLETION_DATE,
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.DONE), 1).else_(0))
                    .as("blocksCompleted"),
                DSL.sum(DSL.when(BLOCK.STATUS.eq(BlockStatus.RESERVED), 1).else_(0))
                    .as("blocksReserved"),
                TEAM.GOAL)
            .from(TEAM)
            .leftJoin(USER_TEAM)
            .on(TEAM.ID.eq(USER_TEAM.TEAM_ID))
            .leftJoin(BLOCK)
            .on(USER_TEAM.USER_ID.eq(BLOCK.ASSIGNED_TO))
            .where(
                USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER).or(USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER)))
            .groupBy(TEAM.ID)
            .orderBy(TEAM.GOAL_COMPLETION_DATE.asc())
            .fetchInto(AdminTeamSummary.class);
    return new GetAllTeamsAdminResponse(teams, teams.size());
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

    boolean applicantsToReview =
        userTeamRole.equals(TeamRole.LEADER)
            && teamMembers.stream().anyMatch(tm -> tm.getRole().equals(TeamRole.PENDING));

    List<TeamMember> activeMembers =
        teamMembers.stream()
            .filter(
                tm -> tm.getRole().equals(TeamRole.MEMBER) || tm.getRole().equals(TeamRole.LEADER))
            .collect(Collectors.toList());
    int doneBlocks =
        activeMembers.stream().map(Individual::getBlocksCompleted).reduce(0, Integer::sum);
    int reservedBlocks =
        activeMembers.stream().map(Individual::getBlocksReserved).reduce(0, Integer::sum);
    return new TeamResponse(
        teamPojo.getId(),
        teamPojo.getName(),
        teamPojo.getBio(),
        teamPojo.getGoal(),
        teamPojo.getGoalCompletionDate(),
        doneBlocks,
        reservedBlocks,
        userTeamRole,
        applicantsToReview,
        activeMembers);
  }

  @Override
  public GetUserTeamsResponse getUserTeams(JWTData userdata) {
    List<TeamResponse> ret = new ArrayList<TeamResponse>();
    List<UserTeamRecord> userTeamRecords =
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.USER_ID.eq(userdata.getUserId()))
            .and(
                USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER).or(USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER)))
            .fetch();

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
            .where(
                USER_TEAM.TEAM_ROLE.eq(TeamRole.LEADER).or(USER_TEAM.TEAM_ROLE.eq(TeamRole.MEMBER)))
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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("user_team");
    audit.setTransactionType("update");
    audit.setOldValue(currentLeaderTeam.toString());

    db.update(USER_TEAM)
        .set(USER_TEAM.TEAM_ROLE, TeamRole.MEMBER)
        .where(USER_TEAM.USER_ID.eq(currentLeaderId))
        .and(USER_TEAM.TEAM_ID.eq(request.getTeamId()))
        .execute();

    audit.setResult(currentLeaderTeam.toString());
    audit.setUserId(userData.getUserId());
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

    AuditRecord audit2 = db.newRecord(AUDIT);
    audit2.setTableName("user_team");
    audit2.setTransactionType("update");
    audit2.setOldValue(currentLeaderTeam.toString());

    db.update(USER_TEAM)
        .set(USER_TEAM.TEAM_ROLE, TeamRole.LEADER)
        .where(USER_TEAM.USER_ID.eq(newLeaderId))
        .and(USER_TEAM.TEAM_ID.eq(request.getTeamId()))
        .execute();

    audit2.setResult(currentLeaderTeam.toString());
    audit2.setUserId(userData.getUserId());
    audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit2.insert();
  }
}
