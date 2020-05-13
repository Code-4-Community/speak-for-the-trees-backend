package com.codeforcommunity.dto.blockInfo;

public class Individual {
  protected final int id;
  protected final String username;
  protected final int blocksCompleted;
  protected final int blocksReserved;

  public Individual(int id, String username, int blocksCompleted, int blocksReserved) {
    this.id = id;
    this.username = username;
    this.blocksCompleted = blocksCompleted;
    this.blocksReserved = blocksReserved;
  }

  public int getBlocksReserved() {
    return blocksReserved;
  }

  public int getBlocksCompleted() {
    return blocksCompleted;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }
}
