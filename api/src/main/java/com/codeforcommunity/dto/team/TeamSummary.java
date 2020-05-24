package com.codeforcommunity.dto.team;

import com.codeforcommunity.enums.TeamRole;

public class TeamSummary {

  private int id;
  private String name;
  private int memberCount;
  private TeamRole userTeamRole;

  public TeamSummary(int id, String name, int memberCount, TeamRole userTeamRole) {
    this.id = id;
    this.name = name;
    this.memberCount = memberCount;
    this.userTeamRole = userTeamRole == null ? TeamRole.NONE : userTeamRole;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getMemberCount() {
    return memberCount;
  }

  public TeamRole getUserTeamRole() {
    return userTeamRole;
  }
}
