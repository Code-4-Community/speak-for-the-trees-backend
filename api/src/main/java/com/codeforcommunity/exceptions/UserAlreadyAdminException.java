package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class UserAlreadyAdminException extends HandledException {

  private int userId;

  public UserAlreadyAdminException(int userId) {
    super();
    this.userId = userId;
  }

  public int getUserId() {
    return userId;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleUserAlreadyAdmin(ctx, this);
  }
}
