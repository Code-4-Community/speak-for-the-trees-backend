package com.codeforcommunity.dto.blocks;

import java.util.List;

public class GetReservedAdminResponse {

  private List<BlockReservation> blocks;

  public GetReservedAdminResponse() {}

  public GetReservedAdminResponse(List<BlockReservation> blocks) {
    this.blocks = blocks;
  }

  public List<BlockReservation> getBlocks() {
    return blocks;
  }
}
