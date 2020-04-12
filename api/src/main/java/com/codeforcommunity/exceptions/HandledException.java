package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;

import io.vertx.ext.web.RoutingContext;

public interface HandledException {

  void callHandler(FailureHandler handler, RoutingContext ctx);
}
