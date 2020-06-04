package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class NoSuchTeamRequestException extends RuntimeException implements HandledException {

  private int requestId;
  private int teamId;

  public NoSuchTeamRequestException(int requestId, int teamId) {
    super();
    this.requestId = requestId;
    this.teamId = teamId;
  }

  public int getRequestId() {
    return requestId;
  }

  public int getTeamId() {
    return teamId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleNoSuchTeamRequest(ctx, this);
  }
}
