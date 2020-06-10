package com.codeforcommunity.dto.team;

import java.util.List;

public class GetUserTeamsResponse {
  private List<TeamResponse> teams;

  private GetUserTeamsResponse() {}

  public GetUserTeamsResponse(List<TeamResponse> teams) {
    this.teams = teams;
  }

  public List<TeamResponse> getTeams() {
    return teams;
  }
}
