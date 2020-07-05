package com.codeforcommunity.dto.blocks;

import com.codeforcommunity.dto.ApiDto;
import com.codeforcommunity.exceptions.HandledException;
import java.util.ArrayList;
import java.util.List;

public class BlocksSeedingRequest extends ApiDto {

  private List<BlockSeedingInfo> blocks;

  public BlocksSeedingRequest(List<BlockSeedingInfo> blocks) {
    this.blocks = blocks;
  }

  private BlocksSeedingRequest() {}

  public List<BlockSeedingInfo> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<BlockSeedingInfo> blocks) {
    this.blocks = blocks;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) throws HandledException {
    String fieldName = fieldPrefix + "blocks_seeding_request.";
    List<String> fields = new ArrayList<>();

    if (blocks == null) {
      fields.add(fieldName + "blocks");
    } else {
      for (BlockSeedingInfo block : blocks) {
        block.validate();
      }
    }

    return fields;
  }
}
