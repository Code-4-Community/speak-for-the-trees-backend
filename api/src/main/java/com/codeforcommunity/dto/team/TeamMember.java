package com.codeforcommunity.dto.team;

import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.enums.TeamRole;

public class TeamMember extends Individual {
  private final TeamRole role;

  public TeamMember(
      int id, String username, int blocksCompleted, int blocksReserved, TeamRole teamRole) {
    super(id, username, blocksCompleted, blocksReserved);
    this.role = teamRole;
  }

  public TeamRole getRole() {
    return this.role;
  }
}
