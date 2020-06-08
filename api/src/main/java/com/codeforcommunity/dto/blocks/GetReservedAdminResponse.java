package com.codeforcommunity.dto.blocks;

import java.util.List;

public class GetReservedAdminResponse {

  private List<ReservedBlock> blocks;

  public GetReservedAdminResponse() {}

  public GetReservedAdminResponse(List<ReservedBlock> blocks) {
    this.blocks = blocks;
  }

  public List<ReservedBlock> getBlocks() {
    return blocks;
  }
}
