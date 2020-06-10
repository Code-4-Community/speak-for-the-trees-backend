package com.codeforcommunity.dto.blocks;

import java.util.List;

public class GetAssignedBlocksResponse {

  private List<AssignedBlock> blocks;

  public GetAssignedBlocksResponse() {}

  public GetAssignedBlocksResponse(List<AssignedBlock> blocks) {
    this.blocks = blocks;
  }

  public List<AssignedBlock> getBlocks() {
    return blocks;
  }
}
