package com.codeforcommunity.dto.blocks;

import com.codeforcommunity.api.ApiDto;
import java.util.ArrayList;
import java.util.List;

public class StandardBlockRequest extends ApiDto {
  private List<String> blocks;

  public List<String> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<String> blocks) {
    this.blocks = blocks;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "standard_block_request.";
    List<String> fields = new ArrayList<>();

    if (blocks == null) {
      fields.add(fieldName + "blocks");
    } else {
      for (String block : blocks) {
        if (block == null) {
          fields.add(fieldName + "blocks.block");
        }
      }
    }
    return fields;
  }
}
