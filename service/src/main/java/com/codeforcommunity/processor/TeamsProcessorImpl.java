package com.codeforcommunity.processor;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.requester.Emailer;
import org.jooq.DSLContext;
import org.jooq.User;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;

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

    teamRequest.getInviteEmails().forEach((email) -> {
      emailer.sendInviteEmail(email, "Team Member", inviter, teamRecord.into(Team.class));
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
}
