package com.codeforcommunity.processor;

import com.codeforcommunity.api.IProtectedUserProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.enums.TeamRole;
import org.jooq.DSLContext;
import org.jooq.generated.tables.records.UserTeamRecord;

import java.util.Optional;

import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.generated.Tables.VERIFICATION_KEYS;

public class ProtectedUserProcessorImpl implements IProtectedUserProcessor {

  private final DSLContext db;

  public ProtectedUserProcessorImpl(DSLContext db) {
    this.db = db;
  }

  @Override
  public void deleteUser(JWTData userData) {
    int userId = userData.getUserId();

    db.deleteFrom(VERIFICATION_KEYS)
        .where(VERIFICATION_KEYS.USER_ID.eq(userId))
        .executeAsync();

    Optional<UserTeamRecord> maybeUserTeamRecord = Optional.ofNullable(
        db.selectFrom(USER_TEAM)
            .where(USER_TEAM.USER_ID.eq(userId))
            .fetchOne()
    );

    if (maybeUserTeamRecord.isPresent()) {
      UserTeamRecord userTeamRecord = maybeUserTeamRecord.get();
      if (userTeamRecord.getTeamRole() == TeamRole.LEADER) {
        db.deleteFrom(USER_TEAM)
            .where(USER_TEAM.TEAM_ID.eq(userTeamRecord.getTeamId()))
            .executeAsync();

        db.deleteFrom(TEAM)
            .where(TEAM.ID.eq(userTeamRecord.getTeamId()))
            .executeAsync();
      } else {
        db.executeDelete(userTeamRecord, USER_TEAM.USER_ID.eq(userId));
      }
    }

    db.deleteFrom(USERS)
        .where(USERS.ID.eq(userId))
        .executeAsync();
  }
}
