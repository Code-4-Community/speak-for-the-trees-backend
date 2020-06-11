package com.codeforcommunity.auth;

import com.codeforcommunity.enums.PrivilegeLevel;

public class JWTData {

  private final Integer userId;
  private final PrivilegeLevel privilegeLevel;

  public JWTData(Integer userId, PrivilegeLevel privilegeLevel) {
    this.userId = userId;
    this.privilegeLevel = privilegeLevel;
  }

  public Integer getUserId() {
    return userId;
  }

  public PrivilegeLevel getPrivilegeLevel() {
    return privilegeLevel;
  }
}
