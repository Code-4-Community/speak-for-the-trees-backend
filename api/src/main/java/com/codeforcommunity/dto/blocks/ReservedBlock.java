package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;

public class ReservedBlock {

  private String fid;
  private String username;
  private Timestamp reserveDate;

  public ReservedBlock() {}

  public ReservedBlock(String fid, String username, Timestamp reserveDate) {
    this.fid = fid;
    this.username = username;
    this.reserveDate = reserveDate;
  }

  public String getFid() {
    return fid;
  }

  public String getUsername() {
    return username;
  }

  public Timestamp getReserveDate() {
    return reserveDate;
  }
}
