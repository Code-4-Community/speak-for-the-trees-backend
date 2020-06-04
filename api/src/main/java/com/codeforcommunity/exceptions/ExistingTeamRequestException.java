package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class ExistingTeamRequestException extends RuntimeException implements HandledException {

  private int userId;
  private int teamId;

  public ExistingTeamRequestException(int userId, int teamId) {
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
    handler.handleExistingTeamRequest(ctx, this);
  }
}
