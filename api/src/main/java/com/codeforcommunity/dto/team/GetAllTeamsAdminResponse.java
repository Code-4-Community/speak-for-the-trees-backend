package com.codeforcommunity.dto.team;

import java.util.List;

public class GetAllTeamsAdminResponse {
  private List<AdminTeamSummary> teams;
  private int rowCount;

  public GetAllTeamsAdminResponse() {}

  public GetAllTeamsAdminResponse(List<AdminTeamSummary> teams, int rowCount) {
    this.teams = teams;
    this.rowCount = rowCount;
  }

  public List<AdminTeamSummary> getTeams() {
    return teams;
  }

  public int getRowCount() {
    return rowCount;
  }
}
