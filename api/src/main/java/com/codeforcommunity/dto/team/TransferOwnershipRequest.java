package com.codeforcommunity.dto.team;

import java.util.ArrayList;
import java.util.List;

public class TransferOwnershipRequest {
  private String newOwnerEmail;
  private Integer teamId;

  public TransferOwnershipRequest() {}

  public TransferOwnershipRequest(Integer teamId, String newOwnerEmail) {
    this.teamId = teamId;
    this.newOwnerEmail = newOwnerEmail;
  }

  public Integer getTeamId() {
    return teamId;
  }

  public String getNewOwnerEmail() {
    return newOwnerEmail;
  }

  public void setTeamId(Integer teamId) {
    this.teamId = teamId;
  }

  public void setNewOwnerEmail(String newOwnerEmail) {
    this.newOwnerEmail = newOwnerEmail;
  }

  // TODO: use this in ApiDto and user isBlank for checking email
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "transfer_ownership_request.";
    List<String> fields = new ArrayList<>();
    if (newOwnerEmail == null) {
      fields.add(fieldName + "new_owner_email");
    }
    return fields;
  }
}
