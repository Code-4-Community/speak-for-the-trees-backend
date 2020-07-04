package com.codeforcommunity.dto.blocks;

import com.codeforcommunity.dto.ApiDto;
import com.codeforcommunity.exceptions.HandledException;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BlockSeedingInfo extends ApiDto {

  private String id;
  @JsonFormat(pattern="MM/dd/yyyy")
  private Timestamp dateCompleted;

  public BlockSeedingInfo(String id, Timestamp dateCompleted) {
    this.id = id;
    this.dateCompleted = dateCompleted;
  }

  private BlockSeedingInfo() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Timestamp getDateCompleted() {
    return dateCompleted;
  }

  public void setDateCompleted(Timestamp dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) throws HandledException {
    String fieldName = fieldPrefix + "block_seeding_info.";
    List<String> fields = new ArrayList<>();

    if (id == null || id.isEmpty()) {
      fields.add(fieldName + "id");
    }
    if (dateCompleted == null) {
      fields.add(fieldName + "dateCompleted");
    }

    return fields;
  }
}
