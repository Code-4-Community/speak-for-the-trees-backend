package com.codeforcommunity.rest.subrouter;

import static com.codeforcommunity.rest.ApiRouter.end;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.dto.blocks.GetAssignedBlocksResponse;
import com.codeforcommunity.dto.blocks.StandardBlockRequest;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

public class BlocksRouter implements IRouter {

  private final IBlockProcessor processor;

  public BlocksRouter(IBlockProcessor processor) {
    this.processor = processor;
  }

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerReserve(router);
    registerFinish(router);
    registerRelease(router);
    registerReset(router);
    registerGetReserved(router);
    registerResetAllBlocks(router);
    registerGetReservedAdmin(router);
    registerGetDoneAdmin(router);

    return router;
  }

  private void registerReserve(Router router) {
    Route reserveRoute = router.post("/reserve");
    reserveRoute.handler(this::handleReserveRoute);
  }

  private void registerFinish(Router router) {
    Route reserveRoute = router.post("/finish");
    reserveRoute.handler(this::handleFinishRoute);
  }

  private void registerRelease(Router router) {
    Route reserveRoute = router.post("/release");
    reserveRoute.handler(this::handleReleaseRoute);
  }

  private void registerReset(Router router) {
    Route reserveRoute = router.post("/reset");
    reserveRoute.handler(this::handleResetRoute);
  }

  private void registerGetReserved(Router router) {
    Route reserveRoute = router.get("/reserved");
    reserveRoute.handler(this::handleGetReserved);
  }

  private void registerGetReservedAdmin(Router router) {
    Route reservedAdminRoute = router.get("/reserved/admin");
    reservedAdminRoute.handler(this::handleGetReservedAdmin);
  }

  private void registerGetDoneAdmin(Router router) {
    Route doneAdminRoute = router.get("/done/admin");
    doneAdminRoute.handler(this::handleGetDoneAdmin);
  }

  private void registerResetAllBlocks(Router router) {
    Route resetAllRoute = router.post("/reset/hard");
    resetAllRoute.handler(this::handleResetAll);
  }

  private void handleReserveRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    StandardBlockRequest blockRequest =
        RestFunctions.getJsonBodyAsClass(ctx, StandardBlockRequest.class);

    BlockResponse response = processor.reserveBlocks(userData, blockRequest.getBlocks());

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleFinishRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    StandardBlockRequest blockRequest =
        RestFunctions.getJsonBodyAsClass(ctx, StandardBlockRequest.class);

    BlockResponse response = processor.finishBlocks(userData, blockRequest.getBlocks());

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleReleaseRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    StandardBlockRequest blockRequest =
        RestFunctions.getJsonBodyAsClass(ctx, StandardBlockRequest.class);

    BlockResponse response = processor.releaseBlocks(userData, blockRequest.getBlocks());

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleResetRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    StandardBlockRequest blockRequest =
        RestFunctions.getJsonBodyAsClass(ctx, StandardBlockRequest.class);

    BlockResponse response = processor.resetBlocks(userData, blockRequest.getBlocks());

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleGetReserved(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    boolean includeDone = RestFunctions.getRequestParameterAsBoolean(ctx.request(), "done");

    List<String> response = processor.getUserReservedBlocks(userData, includeDone);

    end(ctx.response(), 200, new JsonArray(response).encode());
  }

  private void handleGetReservedAdmin(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");

    GetAssignedBlocksResponse response = processor.getAllReservedBlocks(userData);

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleGetDoneAdmin(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");

    GetAssignedBlocksResponse response = processor.getAllDoneBlocks(userData);

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleResetAll(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");

    processor.resetAllBlocks(userData);

    end(ctx.response(), 200);
  }
}
