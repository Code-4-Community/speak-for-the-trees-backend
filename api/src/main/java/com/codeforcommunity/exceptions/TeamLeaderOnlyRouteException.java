package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class TeamLeaderOnlyRouteException extends HandledException {

  private int teamId;

  public TeamLeaderOnlyRouteException(int teamId) {
    super();
    this.teamId = teamId;
  }

  public int getTeamId() {
    return teamId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleTeamLeaderOnlyRoute(ctx, this);
  }
}
