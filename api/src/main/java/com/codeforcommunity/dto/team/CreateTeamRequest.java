package com.codeforcommunity.dto.team;

public class CreateTeamRequest {
  private String name;

  public CreateTeamRequest(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
