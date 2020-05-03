package com.codeforcommunity.rest.subrouter;

import static com.codeforcommunity.rest.ApiRouter.end;

import com.codeforcommunity.api.IBlockInfoProcessor;
import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;
import com.codeforcommunity.rest.IRouter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class BlockInfoRouter implements IRouter {
  private final IBlockInfoProcessor processor;

  public BlockInfoRouter(IBlockInfoProcessor processor) {
    this.processor = processor;
  }

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerGet(router);
    registerGetLeaderboard(router);

    return router;
  }

  private void registerGet(Router router) {
    Route getRoute = router.get("/");

    getRoute.handler(this::handleGetBlocks);
  }

  private void registerGetLeaderboard(Router router) {
    Route getLeaderboardRoute = router.get("/leaderboard");

    getLeaderboardRoute.handler(this::handleGetLeaderboardBlocks);
  }

  private void handleGetBlocks(RoutingContext ctx) {
    BlockInfoResponse response = processor.getBlocks();

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleGetLeaderboardBlocks(RoutingContext ctx) {
    BlockLeaderboardResponse response = processor.getBlockLeaderboards();

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }
}
