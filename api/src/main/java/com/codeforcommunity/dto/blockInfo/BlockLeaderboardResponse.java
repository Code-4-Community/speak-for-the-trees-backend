package com.codeforcommunity.dto.blockInfo;

import java.util.List;

public class BlockLeaderboardResponse {
  private final List<Team> teams;
  private final List<Individual> individuals;

  public BlockLeaderboardResponse(List<Team> teams, List<Individual> individuals) {
    this.teams = teams;
    this.individuals = individuals;
  }

  public List<Individual> getIndividuals() {
    return individuals;
  }

  public List<Team> getTeams() {
    return teams;
  }
}
