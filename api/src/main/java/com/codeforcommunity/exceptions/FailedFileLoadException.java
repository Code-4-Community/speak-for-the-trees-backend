package com.codeforcommunity.exceptions;

import com.codeforcommunity.rest.FailureHandler;
import io.vertx.ext.web.RoutingContext;

public class FailedFileLoadException extends HandledException {

  private final String filePath;

  public FailedFileLoadException(String filePath) {
    super();
    this.filePath = filePath;
  }

  @Override
  public void callHandler(FailureHandler handler, RoutingContext ctx) {
    handler.handleFailedFileLoad(ctx, this);
  }

  public String getFilePath() {
    return filePath;
  }
}
