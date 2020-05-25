package com.codeforcommunity.auth;

import com.codeforcommunity.enums.PrivilegeLevel;

public class JWTData {

  private final Integer userId;
  private final PrivilegeLevel privilegeLevel;
  private final Integer userTeamId; // array

  public JWTData(Integer userId, PrivilegeLevel privilegeLevel, Integer userTeamId) { // array
    this.userId = userId;
    this.privilegeLevel = privilegeLevel;
    this.userTeamId = userTeamId; // array
  }

  public Integer getUserId() {
    return userId;
  }

  public PrivilegeLevel getPrivilegeLevel() {
    return privilegeLevel;
  }

  public Integer getUserTeamId() {
    return userTeamId;
  } // array
}
