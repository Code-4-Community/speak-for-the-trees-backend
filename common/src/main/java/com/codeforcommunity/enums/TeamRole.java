package com.codeforcommunity.enums;

public enum TeamRole {
  MEMBER(1), LEADER(2);

  private int val;

  TeamRole(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }

  public static TeamRole from(Integer val) {
    for (TeamRole role : TeamRole.values()) {
      if (role.val == val) {
        return role;
      }
    }
    throw new IllegalArgumentException(String.format("Given num (%d) that doesn't correspond to any TeamRole", val));
  }
}
