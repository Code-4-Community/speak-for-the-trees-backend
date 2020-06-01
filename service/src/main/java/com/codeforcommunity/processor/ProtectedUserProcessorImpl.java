package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.generated.Tables.VERIFICATION_KEYS;

import com.codeforcommunity.api.IProtectedUserProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.dto.user.ChangePasswordRequest;
import com.codeforcommunity.dto.user.UserDataResponse;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.WrongPasswordException;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;

public class ProtectedUserProcessorImpl implements IProtectedUserProcessor {

  private final DSLContext db;

  public ProtectedUserProcessorImpl(DSLContext db) {
    this.db = db;
  }

  @Override
  public void deleteUser(JWTData userData) {
    int userId = userData.getUserId();

    db.deleteFrom(VERIFICATION_KEYS).where(VERIFICATION_KEYS.USER_ID.eq(userId)).executeAsync();

    Optional<List<UserTeamRecord>> maybeUserTeamRecords =
        Optional.ofNullable(db.selectFrom(USER_TEAM).where(USER_TEAM.USER_ID.eq(userId)).fetch());

    if (maybeUserTeamRecords.isPresent()) {
      List<UserTeamRecord> userTeamRecords = maybeUserTeamRecords.get();
      for (UserTeamRecord userTeamRecord : userTeamRecords) {
        if (userTeamRecord.getTeamRole() == TeamRole.LEADER) {
          db.deleteFrom(USER_TEAM)
              .where(USER_TEAM.TEAM_ID.eq(userTeamRecord.getTeamId()))
              .executeAsync();
          db.deleteFrom(TEAM).where(TEAM.ID.eq(userTeamRecord.getTeamId())).executeAsync();
        } else {
          db.executeDelete(userTeamRecord, USER_TEAM.USER_ID.eq(userId));
        }
      }
    }

    db.deleteFrom(USERS).where(USERS.ID.eq(userId)).executeAsync();
  }

  @Override
  public void changePassword(JWTData userData, ChangePasswordRequest changePasswordRequest) {
    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOne();

    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    if (Passwords.isExpectedPassword(
        changePasswordRequest.getCurrentPassword(), user.getPassHash())) {
      user.setPassHash(Passwords.createHash(changePasswordRequest.getNewPassword()));
      user.store();
    } else {
      throw new WrongPasswordException();
    }
  }

  @Override
  public UserDataResponse getUserData(JWTData userData) {
    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOne();

    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    return new UserDataResponse(
        user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
  }
}
