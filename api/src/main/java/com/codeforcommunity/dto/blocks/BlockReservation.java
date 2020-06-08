package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;

public class BlockReservation {

  private String fid;
  private int userId;
  private Timestamp reserveDate;

  public BlockReservation() {}

  public BlockReservation(String fid, int userId, Timestamp reserveDate) {
    this.fid = fid;
    this.userId = userId;
    this.reserveDate = reserveDate;
  }

  public String getFid() {
    return fid;
  }

  public int getUserId() {
    return userId;
  }

  public Timestamp getReserveDate() {
    return reserveDate;
  }
}
