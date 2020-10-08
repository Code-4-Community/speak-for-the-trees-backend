package com.codeforcommunity.processor;

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

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.generated.tables.pojos.Users;
import org.jooq.generated.tables.records.AuditRecord;
import org.jooq.generated.tables.records.UserTeamRecord;
import org.jooq.generated.tables.records.UsersRecord;

import static org.jooq.generated.Tables.*;

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

    String id = db.selectFrom(VERIFICATION_KEYS).where(VERIFICATION_KEYS.USER_ID.eq(userId))
            .fetchOne()
            .getId();

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("verification_keys");
    audit.setTransactionType("delete");
    audit.setResult(id);
    audit.setUserId(userId);
    audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit.insert();

    db.deleteFrom(VERIFICATION_KEYS).where(VERIFICATION_KEYS.USER_ID.eq(userId)).executeAsync();

    Optional<List<UserTeamRecord>> maybeUserTeamRecords =
        Optional.ofNullable(db.selectFrom(USER_TEAM).where(USER_TEAM.USER_ID.eq(userId)).fetch());

    if (maybeUserTeamRecords.isPresent()) {
      List<UserTeamRecord> userTeamRecords = maybeUserTeamRecords.get();
      for (UserTeamRecord userTeamRecord : userTeamRecords) {
        if (userTeamRecord.getTeamRole() == TeamRole.LEADER) {

          AuditRecord audit2 = db.newRecord(AUDIT);
          audit2.setTableName("user_team");
          audit2.setTransactionType("delete");
          audit2.setResult(userId + ", " + userTeamRecord.getTeamId());
          audit2.setUserId(userId);
          audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
          audit2.insert();

          db.deleteFrom(USER_TEAM)
                  .where(USER_TEAM.TEAM_ID.eq(userTeamRecord.getTeamId()))
                  .executeAsync();

          AuditRecord audit3 = db.newRecord(AUDIT);
          audit3.setTableName("team");
          audit3.setTransactionType("delete");
          audit3.setResult(Integer.toString(userTeamRecord.getTeamId()));
          audit3.setUserId(userId);
          audit3.setTimestamp(new Timestamp(System.currentTimeMillis()));
          audit3.insert();

          db.deleteFrom(TEAM).where(TEAM.ID.eq(userTeamRecord.getTeamId())).executeAsync();

        } else {
          db.executeDelete(userTeamRecord, USER_TEAM.USER_ID.eq(userId));

          AuditRecord audit2 = db.newRecord(AUDIT);
          audit2.setTableName("user_team");
          audit2.setTransactionType("delete");
          audit2.setResult(userId + ", " + userId);
          audit2.setUserId(userId);
          audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
          audit2.insert();
        }
      }
    }

    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne();
    user.delete();

    AuditRecord audit2 = db.newRecord(AUDIT);
    audit2.setTableName("users");
    audit2.setTransactionType("delete");
    audit2.setResult(Integer.toString(userId));
    audit2.setUserId(userId);
    audit2.setTimestamp(new Timestamp(System.currentTimeMillis()));
    audit2.insert();

    emailer.sendAccountDeactivatedEmail(
        user.getEmail(), AuthDatabaseOperations.getFullName(user.into(Users.class)));
  }

  @Override
  public void changePassword(JWTData userData, ChangePasswordRequest changePasswordRequest) {
    UsersRecord user = db.selectFrom(USERS).where(USERS.ID.eq(userData.getUserId())).fetchOne();

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("users");
    audit.setTransactionType("update");
    audit.setOldValue(user.toString());

    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    if (Passwords.isExpectedPassword(
        changePasswordRequest.getCurrentPassword(), user.getPassHash())) {
      user.setPassHash(Passwords.createHash(changePasswordRequest.getNewPassword()));
      user.store();

      audit.setResult(user.toString());
      audit.setUserId(user.getId());
      audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
      audit.insert();

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("users");
    audit.setTransactionType("update");
    audit.setOldValue(user.toString());

    if (Passwords.isExpectedPassword(changeEmailRequest.getPassword(), user.getPassHash())) {
      if (db.fetchExists(USERS, USERS.EMAIL.eq(changeEmailRequest.getNewEmail()))) {
        throw new EmailAlreadyInUseException(changeEmailRequest.getNewEmail());
      }
      user.setEmail(changeEmailRequest.getNewEmail());
      user.store();

      audit.setResult(user.toString());
      audit.setUserId(user.getId());
      audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
      audit.insert();

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

    AuditRecord audit = db.newRecord(AUDIT);
    audit.setTableName("users");
    audit.setTransactionType("update");
    audit.setOldValue(user.toString());

    if (user == null) {
      throw new UserDoesNotExistException(userData.getUserId());
    }

    if (Passwords.isExpectedPassword(changeUsernameRequest.getPassword(), user.getPassHash())) {
      if (db.fetchExists(USERS, USERS.USERNAME.eq(changeUsernameRequest.getNewUsername()))) {
        throw new UsernameAlreadyInUseException(changeUsernameRequest.getNewUsername());
      }

      user.setUsername(changeUsernameRequest.getNewUsername());
      user.store();

      audit.setResult(user.toString());
      audit.setUserId(user.getId());
      audit.setTimestamp(new Timestamp(System.currentTimeMillis()));
      audit.insert();
    } else {
      throw new WrongPasswordException();
    }
  }
}
