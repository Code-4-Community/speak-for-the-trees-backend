package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class NoUserForRequestException extends RuntimeException implements HandledException {

  private int userId;

  public NoUserForRequestException(int userId) {
    super();
    this.userId = userId;
  }

  public int getUserId() {
    return userId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleNoUserForRequest(ctx, this);
  }
}
