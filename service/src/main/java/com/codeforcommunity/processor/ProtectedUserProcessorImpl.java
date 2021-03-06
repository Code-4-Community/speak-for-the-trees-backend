package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.DELETED_TEAM;
import static org.jooq.generated.Tables.DELETED_USERS;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.generated.Tables.VERIFICATION_KEYS;

import com.codeforcommunity.api.IProtectedUserProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.dataaccess.AuthDatabaseOperations;
import com.codeforcommunity.dto.user.ChangeEmailRequest;
import com.codeforcommunity.dto.user.ChangePasswordRequest;
import com.codeforcommunity.dto.user.ChangeUsernameRequest;
import com.codeforcommunity.dto.user.UserDataResponse;
import com.codeforcommunity.enums.TeamRole;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.UsernameAlreadyInUseException;
import com.codeforcommunity.exceptions.WrongPasswordException;
import com.codeforcommunity.requester.Emailer;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.TeamRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;

public class ProtectedUserProcessorImpl implements IProtectedUserProcessor {

  private final DSLContext db;
  private final Emailer emailer;

  public ProtectedUserProcessorImpl(DSLContext db, Emailer emailer) {
    this.db = db;
    this.emailer = emailer;
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
          TeamRecord team =
              db.selectFrom(TEAM).where(TEAM.ID.eq(userTeamRecord.getTeamId())).fetchOne();
          db.insertInto(DELETED_TEAM).set(team.intoMap());
          team.delete();
        } else {
          db.executeDelete(userTeamRecord, USER_TEAM.USER_ID.eq(userId));
        }
      }
    }

    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne();

    db.insertInto(DELETED_USERS).set(user.intoMap()).execute();
    user.delete();

    emailer.sendAccountDeactivatedEmail(
        user.getEmail(), AuthDatabaseOperations.getFullName(user.into(Users.class)));
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

    emailer.sendPasswordChangeConfirmationEmail(
        user.getEmail(), AuthDatabaseOperations.getFullName(user.into(Users.class)));
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

  @Override
  public void changeEmail(JWTData userData, ChangeEmailRequest changeEmailRequest) {
    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOne();
    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    String previousEmail = user.getEmail();
    if (Passwords.isExpectedPassword(changeEmailRequest.getPassword(), user.getPassHash())) {
      if (db.fetchExists(USERS, USERS.EMAIL.eq(changeEmailRequest.getNewEmail()))) {
        throw new EmailAlreadyInUseException(changeEmailRequest.getNewEmail());
      }
      user.setEmail(changeEmailRequest.getNewEmail());
      user.store();
    } else {
      throw new WrongPasswordException();
    }

    emailer.sendEmailChangeConfirmationEmail(
        previousEmail,
        AuthDatabaseOperations.getFullName(user.into(Users.class)),
        changeEmailRequest.getNewEmail());
  }

  @Override
  public void changeUsername(JWTData userData, ChangeUsernameRequest changeUsernameRequest) {
    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOne();
    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    if (Passwords.isExpectedPassword(changeUsernameRequest.getPassword(), user.getPassHash())) {
      if (db.fetchExists(USERS, USERS.USERNAME.eq(changeUsernameRequest.getNewUsername()))) {
        throw new UsernameAlreadyInUseException(changeUsernameRequest.getNewUsername());
      }

      user.setUsername(changeUsernameRequest.getNewUsername());
      user.store();
    } else {
      throw new WrongPasswordException();
    }
  }
}
