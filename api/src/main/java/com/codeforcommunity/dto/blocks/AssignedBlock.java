package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;

public class AssignedBlock {

  private String id;
  private String username;
  private Timestamp dateUpdated;

  public AssignedBlock() {}

  public AssignedBlock(String id, String username, Timestamp dateUpdated) {
    this.id = id;
    this.username = username;
    this.dateUpdated = dateUpdated;
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public Timestamp getDateUpdated() {
    return dateUpdated;
  }
}
