package com.codeforcommunity.enums;

public enum BlockStatus {
  OPEN(0), RESERVED(1), DONE(2);

  private int val;

  BlockStatus(int val) {
    this.val = val;
  }

  public int getVal() {
    return val;
  }
}
