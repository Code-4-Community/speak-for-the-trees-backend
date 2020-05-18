package com.codeforcommunity.auth;

import com.codeforcommunity.enums.PrivilegeLevel;

public class JWTData {

  private final Integer userId;
  private final PrivilegeLevel privilegeLevel;
  private final Integer userTeamId;

  public JWTData(Integer userId, PrivilegeLevel privilegeLevel, Integer userTeamId) {
    this.userId = userId;
    this.privilegeLevel = privilegeLevel;
    this.userTeamId = userTeamId;
  }

  public Integer getUserId() {
    return userId;
  }

  public PrivilegeLevel getPrivilegeLevel() {
    return privilegeLevel;
  }

  public Integer getUserTeamId() {
    return userTeamId;
  }
}
