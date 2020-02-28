package com.codeforcommunity.rest.subrouter;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.StandardBlockRequest;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.codeforcommunity.rest.ApiRouter.end;

public class BlocksRouter implements IRouter {

  private IBlockProcessor processor;

  // TODO: Make constructor and pass in processor properly

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerReserve(router);

    return router;
  }

  private void registerReserve(Router router) {
    Route reserveRoute = router.post("/reserve");
    reserveRoute.handler(this::handleReserveRoute);
  }

  private void handleReserveRoute(RoutingContext ctx) {
    StandardBlockRequest blockRequest = RestFunctions.getJsonBodyAsClass(ctx, StandardBlockRequest.class);

    // TODO: Figure out how to get a JWT data out of routing context
    processor.reserveBlocks(new JWTData(1, PrivilegeLevel.NONE), blockRequest.getBlocks());

    end(ctx.response(), 200);
  }
}
