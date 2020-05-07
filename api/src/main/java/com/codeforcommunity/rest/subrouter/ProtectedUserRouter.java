package com.codeforcommunity.rest.subrouter;

import com.codeforcommunity.api.IProtectedUserProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.dto.blocks.StandardBlockRequest;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.codeforcommunity.rest.ApiRouter.end;

public class ProtectedUserRouter  implements IRouter {

  private final IProtectedUserProcessor processor;

  public ProtectedUserRouter(IProtectedUserProcessor processor) {
    this.processor = processor;
  }

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerDeleteUser(router);

    return router;
  }

  private void registerDeleteUser(Router router) {
    Route deleteUserRoute = router.delete("/");
    deleteUserRoute.handler(this::handleDeleteUserRoute);
  }

  private void handleDeleteUserRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");

    processor.deleteUser(userData);

    end(ctx.response(), 200);
  }
}
