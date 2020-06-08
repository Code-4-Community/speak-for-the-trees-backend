package com.codeforcommunity.dto.team;

public class TeamApplicant {

  private Integer userId;
  private String username;
  private String firstName;
  private String lastName;

  public TeamApplicant(Integer userId, String username, String firstName, String lastName) {
    this.userId = userId;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public Integer getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }
}
