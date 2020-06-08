package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;

public class AssignedBlock {

  private String fid;
  private String username;
  private Timestamp date;

  public AssignedBlock() {}

  public AssignedBlock(String fid, String username, Timestamp date) {
    this.fid = fid;
    this.username = username;
    this.date = date;
  }

  public String getFid() {
    return fid;
  }

  public String getUsername() {
    return username;
  }

  public Timestamp getDate() {
    return date;
  }
}
