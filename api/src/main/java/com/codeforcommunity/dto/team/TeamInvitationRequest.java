package com.codeforcommunity.dto.team;

import com.codeforcommunity.dto.ApiDto;
import java.util.ArrayList;
import java.util.List;

public class TeamInvitationRequest extends ApiDto {
  private String name;
  private String email;

  private TeamInvitationRequest() {}

  public TeamInvitationRequest(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() {
    return this.name;
  }

  public String getEmail() {
    return this.email;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "team_invitation_request.";
    List<String> fields = new ArrayList<>();

    if (isEmpty(name)) {
      fields.add(fieldName + "name");
    }
    if (emailInvalid(email)) {
      fields.add(fieldName + "email");
    }
    return fields;
  }
}
