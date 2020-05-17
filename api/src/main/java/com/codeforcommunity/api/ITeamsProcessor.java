package com.codeforcommunity.api;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.team.CreateTeamRequest;
import com.codeforcommunity.dto.team.GetAllTeamsResponse;
import com.codeforcommunity.dto.team.InviteMembersRequest;
import com.codeforcommunity.dto.team.TeamResponse;

public interface ITeamsProcessor {

  TeamResponse createTeam(JWTData userData, CreateTeamRequest teamRequest);
  void joinTeam(JWTData userData, int teamId);
  void leaveTeam(JWTData userData, int teamId);
  void disbandTeam(JWTData userData, int teamId);
  void kickFromTeam(JWTData userData, int teamId, int kickUserId);
  void inviteToTeam(JWTData userData, InviteMembersRequest inviteMembersRequest);
  GetAllTeamsResponse getAllTeams();
  TeamResponse getSingleTeam(int teamId);
}
