package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class UserAlreadyOnTeamException extends RuntimeException implements HandledException {

  private int userId;

  public UserAlreadyOnTeamException(int userId) {
    super();
    this.userId = userId;
  }

  public int getUserId() {
    return userId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleUserAlreadyOnTeam(ctx, this);
  }
}
