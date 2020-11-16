package com.codeforcommunity.enums;

public enum PrivilegeLevel {
  NONE(-1),
  STANDARD(0),
  ADMIN(1),
  SUPER_ADMIN(2);

  private int val;

  PrivilegeLevel(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }

  public static PrivilegeLevel from(Integer val) {
    for (PrivilegeLevel privilegeLevel : PrivilegeLevel.values()) {
      if (privilegeLevel.val == val) {
        return privilegeLevel;
      }
    }
    throw new IllegalArgumentException(
        String.format("Given num (%d) that doesn't correspond to any PrivilegeLevel", val));
  }
}
