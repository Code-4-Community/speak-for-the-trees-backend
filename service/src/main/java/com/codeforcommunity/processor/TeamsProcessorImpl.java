package com.codeforcommunity.processor;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import org.jooq.DSLContext;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;

import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USER_TEAM;

public class TeamsProcessorImpl implements ITeamsProcessor {

  DSLContext db;

  public TeamsProcessorImpl(DSLContext db) {
    this.db = db;
  }

  @Override
  public TeamResponse createTeam(JWTData userData, CreateTeamRequest teamRequest) {
    boolean userOnTeam = db.fetchExists(USER_TEAM, USER_TEAM.USER_ID.eq(userData.getUserId()));
    if (userOnTeam) {
      throw new UserAlreadyOnTeamException(userData.getUserId());
    }

    TeamRecord teamRecord = db.newRecord(TEAM);
    teamRecord.setName(teamRequest.getName());
    teamRecord.store();

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

    if (userTeamRecord.getTeamRole() != TeamRole.LEADER) {
      throw new TeamLeaderOnlyRouteException(teamId);
    }

    db.deleteFrom(USER_TEAM)
        .where(USER_TEAM.USER_ID.eq(kickUserId))
        .and(USER_TEAM.TEAM_ID.eq(teamId))
        .execute();
  }
}
