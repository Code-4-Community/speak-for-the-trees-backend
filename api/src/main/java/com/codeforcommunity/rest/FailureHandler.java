package com.codeforcommunity.rest;

import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.ExpiredSecretKeyException;
import com.codeforcommunity.exceptions.HandledException;
import com.codeforcommunity.exceptions.InvalidSecretKeyException;
import com.codeforcommunity.exceptions.MalformedParameterException;
import com.codeforcommunity.exceptions.MissingHeaderException;
import com.codeforcommunity.exceptions.MissingParameterException;
import com.codeforcommunity.exceptions.NoSuchTeamException;
import com.codeforcommunity.exceptions.TeamLeaderExcludedRouteException;
import com.codeforcommunity.exceptions.TeamLeaderOnlyRouteException;
import com.codeforcommunity.exceptions.TokenInvalidException;
import com.codeforcommunity.exceptions.UsedSecretKeyException;
import com.codeforcommunity.exceptions.UserAlreadyOnTeamException;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.UserNotOnTeamException;
import com.codeforcommunity.exceptions.UsernameAlreadyInUseException;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler {

  public void handleFailure(RoutingContext ctx) {
    Throwable throwable = ctx.failure();

    if (throwable instanceof HandledException) {
      ((HandledException) throwable).callHandler(this, ctx);
    } else {
      this.handleUncaughtError(ctx, throwable);
    }
  }

  public void handleAuth(RoutingContext ctx) {
    end(ctx, "Unauthorized user", 401);
  }

  public void handleTokenInvalid(RoutingContext ctx, TokenInvalidException e) {
    String message = String.format("Given %s token is expired or invalid", e.getTokenType());
    end(ctx, message, 401);
  }

  public void handleWrongPassword(RoutingContext ctx) {
    String message = "Given password is not correct";
    end(ctx, message, 401);
  }

  public void handleMissingParameter(RoutingContext ctx, MissingParameterException e) {
    String message =
        String.format("Missing required path parameter: %s", e.getMissingParameterName());
    end(ctx, message, 400);
  }

  public void handleMissingHeader(RoutingContext ctx, MissingHeaderException e) {
    String message = String.format("Missing required request header: %s", e.getMissingHeaderName());
    end(ctx, message, 400);
  }

  public void handleRequestBodyMapping(RoutingContext ctx) {
    String message = "Malformed json request body";
    end(ctx, message, 400);
  }

  public void handleMissingBody(RoutingContext ctx) {
    String message = "Missing required request body";
    end(ctx, message, 400);
  }

  public void handleMalformedParameter(RoutingContext ctx, MalformedParameterException exception) {
    String message = String.format("Given parameter %s is malformed", exception.getParameterName());
    end(ctx, message, 400);
  }

  public void handleEmailAlreadyInUse(RoutingContext ctx, EmailAlreadyInUseException exception) {
    String message =
        String.format("Error creating new user, given email %s already used", exception.getEmail());

    end(ctx, message, 409);
  }

  public void handleUsernameAlreadyInUse(
      RoutingContext ctx, UsernameAlreadyInUseException exception) {
    String message =
        String.format(
            "Error creating new user, given username %s already used", exception.getUsername());

    end(ctx, message, 409);
  }

  public void handleInvalidSecretKey(RoutingContext ctx, InvalidSecretKeyException exception) {
    String message = String.format("Given %s token is invalid", exception.getType());
    end(ctx, message, 401);
  }

  public void handleUsedSecretKey(RoutingContext ctx, UsedSecretKeyException exception) {
    String message = String.format("Given %s token has already been used", exception.getType());
    end(ctx, message, 401);
  }

  public void handleExpiredSecretKey(RoutingContext ctx, ExpiredSecretKeyException exception) {
    String message = String.format("Given %s token is expired", exception.getType());
    end(ctx, message, 401);
  }

  public void handleInvalidPassword(RoutingContext ctx) {
    String message = "Given password does not meet the security requirements";
    end(ctx, message, 400);
  }

  public void handleUserDoesNotExist(RoutingContext ctx, UserDoesNotExistException exception) {
    String message =
        String.format("No user with property <%s> exists", exception.getIdentifierMessage());
    end(ctx, message, 400);
  }

  public void handleAdminOnlyRoute(RoutingContext ctx) {
    String message = "This route is only available to admin users";
    end(ctx, message, 401);
  }

  public void handleTeamLeaderOnlyRoute(RoutingContext ctx, TeamLeaderOnlyRouteException e) {
    String message =
        String.format("This route is only available to the leader of team %d", e.getTeamId());
    end(ctx, message, 401);
  }

  public void handleTeamLeaderExcludedRoute(
      RoutingContext ctx, TeamLeaderExcludedRouteException e) {
    String message =
        String.format("This route is not callable by the leader of team %d", e.getTeamId());
    end(ctx, message, 401);
  }

  public void handleUserAlreadyOnTeam(RoutingContext ctx, UserAlreadyOnTeamException e) {
    String message =
        String.format("User <%d> is already on team <%d>", e.getUserId(), e.getTeamId());
    end(ctx, message, 400);
  }

  public void handleNoSuchTeam(RoutingContext ctx, NoSuchTeamException e) {
    String message = String.format("There is no team with id <%d>", e.getTeamId());
    end(ctx, message, 400);
  }

  public void handleUserNotOnTeam(RoutingContext ctx, UserNotOnTeamException e) {
    String message =
        String.format("The user <%d> is not on a team with id <%d>", e.getUserId(), e.getTeamId());
    end(ctx, message, 400);
  }

  /** A general handler for all exceptions not explicitly handled above. */
  private void handleUncaughtError(RoutingContext ctx, Throwable throwable) {
    String message = String.format("Internal server error caused by: %s", throwable.getMessage());
    end(ctx, message, 500);
  }

  private void end(RoutingContext ctx, String message, int statusCode) {
    ctx.response().setStatusCode(statusCode).end(message);
  }
}
