package com.codeforcommunity.dto.blockInfo;

public class Team {
  private final int id;
  private final String name;
  private final int blocksCompleted;
  private final int blocksReserved;

  public Team(int id, String name, int blocksCompleted, int blocksReserved) {
    this.id = id;
    this.name = name;
    this.blocksCompleted = blocksCompleted;
    this.blocksReserved = blocksReserved;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getBlocksCompleted() {
    return blocksCompleted;
  }

  public int getBlocksReserved() {
    return blocksReserved;
  }
}
