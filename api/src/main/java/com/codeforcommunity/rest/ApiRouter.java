package com.codeforcommunity.rest;

import com.codeforcommunity.api.IAuthProcessor;
import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTAuthorizer;

import com.codeforcommunity.rest.subrouter.AuthRouter;
import com.codeforcommunity.rest.subrouter.BlocksRouter;
import com.codeforcommunity.rest.subrouter.CommonRouter;
import com.codeforcommunity.rest.subrouter.TeamsRouter;
import io.vertx.core.Vertx;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;


public class ApiRouter implements IRouter {
    private final CommonRouter commonRouter;
    private final BlocksRouter blocksRouter;
    private final TeamsRouter teamsRouter;
    private final AuthRouter authRouter;

    public ApiRouter(IAuthProcessor authProcessor, IBlockProcessor blockProcessor, ITeamsProcessor teamsProcessor, JWTAuthorizer jwtAuthorizer) {
        this.commonRouter = new CommonRouter(jwtAuthorizer);
        this.authRouter = new AuthRouter(authProcessor);
        this.blocksRouter = new BlocksRouter(blockProcessor);
        this.teamsRouter = new TeamsRouter(teamsProcessor);
    }

    /**
     * Initialize a router and register all route handlers on it.
     */
    public Router initializeRouter(Vertx vertx) {
        Router router = commonRouter.initializeRouter(vertx);

        router.mountSubRouter("/user", authRouter.initializeRouter(vertx));
        router.mountSubRouter("/protected", defineProtectedRoutes(vertx));

        return router;
    }

    /**
     * Mounts all routes that require a user to be logged in. All routes defined here
     * require a user to have a valid JWT access token in their header.
     */
    private Router defineProtectedRoutes(Vertx vertx) {
        Router router = Router.router(vertx);

        router.mountSubRouter("/blocks", blocksRouter.initializeRouter(vertx));
        router.mountSubRouter("/teams", teamsRouter.initializeRouter(vertx));

        return router;
    }


    public static void end(HttpServerResponse response, int statusCode) {
        end(response, statusCode, null);
    }

    public static void end(HttpServerResponse response, int statusCode, String jsonBody) {
        response.setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "DELETE, POST, GET, OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
        if (jsonBody == null || jsonBody.equals("")) {
            response.end();
        } else {
            response.end(jsonBody);
        }
    }
}
