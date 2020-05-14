package com.codeforcommunity.dto.team;

import java.util.List;

public class GetAllTeamsResponse {
  private List<TeamSummary> teams;
  private int rowCount;

  public GetAllTeamsResponse(List<TeamSummary> teams, int rowCount) {
    this.teams = teams;
    this.rowCount = rowCount;
  }
}
