package com.codeforcommunity;

import com.codeforcommunity.rest.ApiRouter;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.codeforcommunity.rest.ApiRouter.end;

/**
 * The main point for the API.
 */
public class ApiMain {
  private final ApiRouter apiRouter;

  public ApiMain(ApiRouter apiRouter) {
    this.apiRouter = apiRouter;
  }

  /**
   * Start the API to start listening on a port.
   */
  public void startApi(Vertx vertx) {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

    Route homeRoute = router.route("/");
    homeRoute.handler(this::handleHealthCheck);

    router.mountSubRouter("/api/v1", apiRouter.initializeRouter(vertx));

    server.requestHandler(router).listen(8081);
  }

  private void handleHealthCheck(RoutingContext ctx) {
    end(ctx.response(), 200);
  }
}
