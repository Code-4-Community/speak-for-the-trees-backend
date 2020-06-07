package com.codeforcommunity.rest.subrouter;

import static com.codeforcommunity.rest.ApiRouter.end;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.GetUserTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TransferOwnershipRequest;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TeamsRouter implements IRouter {

  private final ITeamsProcessor processor;

  public TeamsRouter(ITeamsProcessor processor) {
    this.processor = processor;
  }

  @Override
  public Router initializeRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    registerGetUserTeams(router);
    registerCreate(router);
    registerJoin(router);
    registerLeave(router);
    registerDisband(router);
    registerKick(router);
    registerInvite(router);
    registerGetAllTeams(router);
    registerGetSingleTeam(router);
    registerTransferOwnership(router);

    return router;
  }

  private void registerGetUserTeams(Router router) {
    Route getUserTeamsRoute = router.get("/user_teams");
    getUserTeamsRoute.handler(this::handleGetUserTeams);
  }

  private void registerGetAllTeams(Router router) {
    Route getAllTeamsRoute = router.get("/");
    getAllTeamsRoute.handler(this::handleGetAllTeams);
  }

  private void registerGetSingleTeam(Router router) {
    Route getSingleTeamRoute = router.get("/:team_id");
    getSingleTeamRoute.handler(this::handleGetSingleTeam);
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

  private void registerTransferOwnership(Router router) {
    Route transferRoute = router.post("/:team_id/transfer_ownership");
    transferRoute.handler(this::transferOwnershipRoute);
  }

  private void handleGetUserTeams(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    GetUserTeamsResponse response = processor.getUserTeams(userData);
    end(ctx.response(), 200, JsonObject.mapFrom(response).toString());
  }

  private void handleGetAllTeams(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    GetAllTeamsResponse response = processor.getAllTeams(userData);
    end(ctx.response(), 200, JsonObject.mapFrom(response).toString());
  }

  private void handleGetSingleTeam(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    TeamResponse response = processor.getSingleTeam(userData, teamId);
    end(ctx.response(), 200, JsonObject.mapFrom(response).toString());
  }

  private void handleCreateRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    CreateTeamRequest createTeamRequest =
        RestFunctions.getJsonBodyAsClass(ctx, CreateTeamRequest.class);

    TeamResponse response = processor.createTeam(userData, createTeamRequest);

    end(ctx.response(), 200, JsonObject.mapFrom(response).toString());
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
    InviteMembersRequest inviteMembersRequest =
        RestFunctions.getJsonBodyAsClass(ctx, InviteMembersRequest.class);
    inviteMembersRequest.setTeamId(teamId);

    processor.inviteToTeam(userData, inviteMembersRequest);

    end(ctx.response(), 200);
  }

  private void transferOwnershipRoute(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    TransferOwnershipRequest transferOwnershipRequest =
        RestFunctions.getJsonBodyAsClass(ctx, TransferOwnershipRequest.class);
    transferOwnershipRequest.setTeamId(teamId);

    processor.transferOwnership(userData, transferOwnershipRequest);

    end(ctx.response(), 200);
  }
}
