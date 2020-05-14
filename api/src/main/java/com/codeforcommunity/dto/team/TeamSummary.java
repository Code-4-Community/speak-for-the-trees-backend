package com.codeforcommunity.dto.team;

public class TeamSummary {

  private int id;
  private String name;
  private int memberCount;

  public TeamSummary(int id, String name, int memberCount) {
    this.id = id;
    this.name = name;
    this.memberCount = memberCount;
  }
}
