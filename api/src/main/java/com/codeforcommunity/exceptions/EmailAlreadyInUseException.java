package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.subrouter.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class EmailAlreadyInUseException extends RuntimeException implements HandledException {
  private final String email;

  public EmailAlreadyInUseException(String email) {
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleEmailAlreadyInUse(ctx, this);
  }
}
