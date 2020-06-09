package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;

public class AssignedBlock {

  private String fid;
  private String username;
  private Timestamp dateUpdated;

  public AssignedBlock() {}

  public AssignedBlock(String fid, String username, Timestamp dateUpdated) {
    this.fid = fid;
    this.username = username;
    this.dateUpdated = dateUpdated;
  }

  public String getFid() {
    return fid;
  }

  public String getUsername() {
    return username;
  }

  public Timestamp getDateUpdated() {
    return dateUpdated;
  }
}
