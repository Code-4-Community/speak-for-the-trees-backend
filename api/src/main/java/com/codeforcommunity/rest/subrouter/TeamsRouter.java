package com.codeforcommunity.rest.subrouter;

import static com.codeforcommunity.rest.ApiRouter.end;

import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsAdminResponse;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.GetUserTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamApplicant;
import com.codeforcommunity.dto.team.TeamApplicantsResponse;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TransferOwnershipRequest;
import com.codeforcommunity.rest.IRouter;
import com.codeforcommunity.rest.RestFunctions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

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
    registerExportTeams(router);
    registerApply(router);
    registerGetApplicants(router);
    registerApproveApplicant(router);
    registerRejectApplicant(router);
    registerLeave(router);
    registerDisband(router);
    registerKick(router);
    registerInvite(router);
    registerGetAllTeams(router);
    registerGetAllTeamsAdmin(router);
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

  private void registerGetAllTeamsAdmin(Router router) {
    Route route = router.get("/admin");
    route.handler(this::handleGetAllTeamsAdmin);
  }

  private void registerGetSingleTeam(Router router) {
    Route getSingleTeamRoute = router.get("/:team_id");
    getSingleTeamRoute.handler(this::handleGetSingleTeam);
  }

  private void registerCreate(Router router) {
    Route createRoute = router.post("/");
    createRoute.handler(this::handleCreateRoute);
  }

  private void registerExportTeams(Router router) {
    Route exportRoute = router.get("/export");
    exportRoute.handler(this::handleExportTeams);
  }

  private void registerApply(Router router) {
    Route applyRoute = router.post("/:team_id/apply");
    applyRoute.handler(this::handleApply);
  }

  private void registerGetApplicants(Router router) {
    Route applicantsRoute = router.get("/:team_id/applicants");
    applicantsRoute.handler(this::handleGetApplicants);
  }

  private void registerApproveApplicant(Router router) {
    Route approveRoute = router.post("/:team_id/applicants/:request_id/approve");
    approveRoute.handler(this::handleApproveApplicant);
  }

  private void registerRejectApplicant(Router router) {
    Route rejectRoute = router.post("/:team_id/applicants/:request_id/reject");
    rejectRoute.handler(this::handleRejectApplicant);
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

  private void handleGetAllTeamsAdmin(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    GetAllTeamsAdminResponse response = processor.getAllTeamsAdmin(userData);
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

  private void handleApply(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");

    processor.applyForTeam(userData, teamId);

    end(ctx.response(), 200);
  }

  private void handleGetApplicants(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");

    List<TeamApplicant> applicants = processor.getTeamApplicants(userData, teamId);
    TeamApplicantsResponse response = new TeamApplicantsResponse(applicants);

    end(ctx.response(), 200, JsonObject.mapFrom(response).encode());
  }

  private void handleApproveApplicant(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    int requestId = RestFunctions.getRequestParameterAsInt(ctx.request(), "request_id");

    processor.approveTeamRequest(userData, teamId, requestId);

    end(ctx.response(), 200);
  }

  private void handleRejectApplicant(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");
    int teamId = RestFunctions.getRequestParameterAsInt(ctx.request(), "team_id");
    int requestId = RestFunctions.getRequestParameterAsInt(ctx.request(), "request_id");

    processor.rejectTeamRequest(userData, teamId, requestId);

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

  private void handleExportTeams(RoutingContext ctx) {
    JWTData userData = ctx.get("jwt_data");

    String csvResults = processor.getAllTeamsForExport(userData);

    end(ctx.response(), 200, csvResults, "text/csv");
  }
}
