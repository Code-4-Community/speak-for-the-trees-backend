package com.codeforcommunity.enums;

public enum VerificationKeyType {
  VERIFY_EMAIL(1), FORGOT_PASSWORD(2);

  private int val;

  VerificationKeyType(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }

  public static VerificationKeyType from(Integer val) {
    for (VerificationKeyType type : VerificationKeyType.values()) {
      if (type.val == val) {
        return type;
      }
    }
    throw new IllegalArgumentException(String.format("Given num (%d) that doesn't correspond to any VerificationKeyType", val));
  }
}
