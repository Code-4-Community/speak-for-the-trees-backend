package com.codeforcommunity.dto.team;

public class TeamInvitationRequest {
  private String name;
  private String email;

  private TeamInvitationRequest() {}

  public TeamInvitationRequest(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() {
    return this.name;
  }

  public String getEmail() {
    return this.email;
  }
}
