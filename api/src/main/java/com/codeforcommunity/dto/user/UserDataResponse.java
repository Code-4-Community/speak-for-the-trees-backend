package com.codeforcommunity.dto.user;

import com.codeforcommunity.enums.PrivilegeLevel;

public class UserDataResponse {

  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private PrivilegeLevel privilegeLevel;

  public UserDataResponse(
      String firstName,
      String lastName,
      String username,
      String email,
      PrivilegeLevel privilegeLevel) {
    this.email = email;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.privilegeLevel = privilegeLevel;
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getUsername() {
    return username;
  }

  public PrivilegeLevel getPrivilegeLevel() {
    return privilegeLevel;
  }
}
