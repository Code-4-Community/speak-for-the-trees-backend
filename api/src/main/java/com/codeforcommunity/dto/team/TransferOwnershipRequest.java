package com.codeforcommunity.dto.team;

import com.codeforcommunity.dto.ApiDto;
import java.util.ArrayList;
import java.util.List;

public class TransferOwnershipRequest extends ApiDto {
  private Integer newLeaderId;
  private Integer teamId;

  public TransferOwnershipRequest() {}

  public TransferOwnershipRequest(Integer teamId, Integer newLeaderId) {
    this.teamId = teamId;
    this.newLeaderId = newLeaderId;
  }

  public Integer getTeamId() {
    return teamId;
  }

  public Integer getNewLeaderId() {
    return newLeaderId;
  }

  public void setTeamId(Integer teamId) {
    this.teamId = teamId;
  }

  public void setNewLeaderId(Integer newLeaderId) {
    this.newLeaderId = newLeaderId;
  }

  // TODO: use this in ApiDto and user isBlank for checking email
  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "transfer_ownership_request.";
    List<String> fields = new ArrayList<>();
    if (newLeaderId == null || newLeaderId < 0) {
      fields.add(fieldName + "new_leader_id");
    }
    if (teamId != null && teamId < 0) {
      fields.add(fieldName + "team_id");
    }
    return fields;
  }
}
