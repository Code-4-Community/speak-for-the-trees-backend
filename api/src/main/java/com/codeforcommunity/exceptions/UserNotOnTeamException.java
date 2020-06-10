package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class UserNotOnTeamException extends HandledException {

  private int userId;
  private int teamId;

  public UserNotOnTeamException(int userId, int teamId) {
    super();
    this.userId = userId;
    this.teamId = teamId;
  }

  public int getUserId() {
    return userId;
  }

  public int getTeamId() {
    return teamId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleUserNotOnTeam(ctx, this);
  }
}
