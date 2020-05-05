package com.codeforcommunity.dto.team;

import java.util.List;

public class InviteMembersRequest {

  private List<String> emails;
  private int teamId;

  public InviteMembersRequest(List<String> emails, int teamId) {
    this.emails = emails;
    this.teamId = teamId;
  }

  private InviteMembersRequest() {}

  public List<String> getEmails() {
    return emails;
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }

  public int getTeamId() {
    return teamId;
  }

  public void setTeamId(int teamId) {
    this.teamId = teamId;
  }
}
