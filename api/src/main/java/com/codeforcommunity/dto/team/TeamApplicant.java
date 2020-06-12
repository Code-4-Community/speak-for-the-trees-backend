package com.codeforcommunity.dto.team;

public class TeamApplicant {

  private Integer userId;
  private String username;

  public TeamApplicant(Integer userId, String username) {
    this.userId = userId;
    this.username = username;
  }

  public Integer getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }
}
