package com.codeforcommunity.dto.team;

public class TeamApplicant {

  private Integer id;
  private String username;
  private String firstName;
  private String lastName;

  public TeamApplicant(Integer id, String username, String firstName, String lastName) {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public Integer getId() {
    return id;
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
