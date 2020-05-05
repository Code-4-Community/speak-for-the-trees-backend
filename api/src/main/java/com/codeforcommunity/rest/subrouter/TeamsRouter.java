package com.codeforcommunity.rest.subrouter;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.dto.blocks.StandardBlockRequest;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.codeforcommunity.rest.ApiRouter.end;

public class TeamsRouter implements IRouter {

  private final ITeamsProcessor processor;

  public TeamsRouter(ITeamsProcessor processor) {
    this.processor = processor;
  }

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerCreate(router);
    registerJoin(router);
    registerLeave(router);
    registerDisband(router);
    registerKick(router);
    registerInvite(router);

    return router;
  }

  private void registerCreate(Router router) {
    Route createRoute = router.post("/");
    createRoute.handler(this::handleCreateRoute);
  }

  private void registerJoin(Router router) {
    Route joinRoute = router.post("/:team_id/join");
    joinRoute.handler(this::handleJoinRoute);
  }

  private void registerLeave(Router router) {
    Route leaveRoute = router.post("/:team_id/leave");
    leaveRoute.handler(this::handleLeaveRoute);
  }

  private void registerDisband(Router router) {
    Route disbandRoute = router.post("/:team_id/disband");
    disbandRoute.handler(this::handleDisbandRoute);
  }

  private void registerKick(Router router) {
    Route kickRoute = router.post("/:team_id/members/:member_id/kick");
    kickRoute.handler(this::handleKickRoute);
  }

  private void registerInvite(Router router) {
    Route inviteRoute = router.post("/:team_id/invite");
    inviteRoute.handler(this::handleInviteRoute);
  }


  private void handleCreateRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    CreateTeamRequest createTeamRequest = RestFunctions.getJsonBodyAsClass(ctx, CreateTeamRequest.class);

    TeamResponse response = processor.createTeam(userData, createTeamRequest);

    //TODO: Implement get team route and set create team to return the same json
    // JsonObject.mapFrom(response).encode());
    end(ctx.response(), 200);
  }

  private void handleJoinRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");

    processor.joinTeam(userData, teamId);

    end(ctx.response(), 200);
  }

  private void handleLeaveRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");

    processor.leaveTeam(userData, teamId);

    end(ctx.response(), 200);
  }

  private void handleDisbandRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");

    processor.disbandTeam(userData, teamId);

    end(ctx.response(), 200);
  }

  private void handleKickRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    int kickUserId = RestFunctions.getRequestParameterAsInt(ctx.request(), "member_id");

    processor.kickFromTeam(userData, teamId, kickUserId);

    end(ctx.response(), 200);
  }

  private void handleInviteRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    InviteMembersRequest inviteMembersRequest = RestFunctions.getJsonBodyAsClass(ctx, InviteMembersRequest.class);
    inviteMembersRequest.setTeamId(teamId);

    processor.inviteToTeam(userData, inviteMembersRequest);

    end(ctx.response(), 200);
  }
}
