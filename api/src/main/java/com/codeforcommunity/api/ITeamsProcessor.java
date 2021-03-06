package com.codeforcommunity.api;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsAdminResponse;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.GetUserTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamApplicant;
import com.codeforcommunity.dto.team.TeamResponse;
import com.codeforcommunity.dto.team.TransferOwnershipRequest;
import java.util.List;

public interface ITeamsProcessor {

  TeamResponse createTeam(JWTData userData, CreateTeamRequest teamRequest);

  void applyForTeam(JWTData userData, int teamId);

  List<TeamApplicant> getTeamApplicants(JWTData userData, int teamId);

  void approveTeamRequest(JWTData userData, int teamId, int applicantId);

  void rejectTeamRequest(JWTData userData, int teamId, int applicantId);

  void leaveTeam(JWTData userData, int teamId);

  void disbandTeam(JWTData userData, int teamId);

  void kickFromTeam(JWTData userData, int teamId, int kickUserId);

  void inviteToTeam(JWTData userData, InviteMembersRequest inviteMembersRequest);

  GetAllTeamsResponse getAllTeams(JWTData userData);

  GetAllTeamsAdminResponse getAllTeamsAdmin(JWTData userData);

  TeamResponse getSingleTeam(JWTData userData, int teamId);

  GetUserTeamsResponse getUserTeams(JWTData userData);

  void transferOwnership(JWTData userData, TransferOwnershipRequest request);

  String getAllTeamsForExport(JWTData userData);
}
