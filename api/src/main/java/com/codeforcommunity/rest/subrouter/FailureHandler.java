package com.codeforcommunity.rest.subrouter;

import com.codeforcommunity.exceptions.CreateUserException;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.HandledException;
import com.codeforcommunity.exceptions.MalformedParameterException;
import com.codeforcommunity.exceptions.MissingHeaderException;
import com.codeforcommunity.exceptions.MissingParameterException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.codeforcommunity.exceptions.UserDoesNotExistException;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler {

   public void handleFailure(RoutingContext ctx) {
    Throwable throwable = ctx.failure();

    if(throwable instanceof HandledException) {
      ((HandledException) throwable).callHandler(this, ctx);
    } else {
      this.handleUncaughtError(ctx, throwable);
    }
  }

  public void handleAuth(RoutingContext ctx) {
    end(ctx, "Unauthorized user", 401);
  }

  public void handleMissingParameter(RoutingContext ctx, MissingParameterException e) {
    String message = String.format("Missing required path parameter: %s", e.getMissingParameterName());
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

  public void handleCreateUser(RoutingContext ctx, CreateUserException exception) {
    CreateUserException.UsedField reason = exception.getUsedField();

    String reasonMessage = reason.equals(CreateUserException.UsedField.BOTH) ? "email and user name":
            reason.toString();

    String message = String.format("Error creating new user, given %s already used", reasonMessage);

    end(ctx, message, 409);
  }

  public void handleMalformedParameter(RoutingContext ctx, MalformedParameterException exception) {
     String message = String.format("Given parameter %s is malformed", exception.getParameterName());
     end(ctx, message, 400);
  }

  public void handleEmailAlreadyInUse(RoutingContext ctx, EmailAlreadyInUseException exception) {
    String message = String.format("Error creating new user, given email %s already used", exception.getEmail());

    end(ctx, message, 409);
  }

  public void handleUserDoesNotExist(RoutingContext ctx, UserDoesNotExistException exception) {
    String message = String.format("No user with property <%s> exists", exception.getIdentifierMessage());
    end(ctx, message, 400);
  }

  private void handleUncaughtError(RoutingContext ctx, Throwable throwable){
    String message = String.format("Internal server error caused by: %s", throwable.getMessage());
    end(ctx, message, 500);
  }

  private void end(RoutingContext ctx, String message, int statusCode) {
    ctx.response().setStatusCode(statusCode).end(message);
  }

}
