package com.codeforcommunity.dto.blockInfo;

public class BlockInfoResponse {
  private int blocksCompleted;
  private int blocksReserved;
  private int blocksOpen;

  public BlockInfoResponse(int blocksCompleted, int blocksReserved, int blocksToDo) {
    this.blocksCompleted = blocksCompleted;
    this.blocksReserved = blocksReserved;
    this.blocksOpen = blocksToDo;
  }

  public int getBlocksCompleted() {
    return blocksCompleted;
  }

  public int getBlocksReserved() {
    return blocksReserved;
  }

  public int getBlocksToDo() {
    return blocksOpen;
  }
}
