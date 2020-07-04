package com.codeforcommunity.dto.team;

import com.codeforcommunity.dto.ApiDto;
import java.util.ArrayList;
import java.util.List;

public class InviteMembersRequest extends ApiDto {

  private List<String> emails;
  private Integer teamId;

  public InviteMembersRequest(List<String> emails, Integer teamId) {
    this.emails = emails;
    this.teamId = teamId;
  }

  private InviteMembersRequest() {}

  public List<String> getEmails() {
    return emails;
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }

  public Integer getTeamId() {
    return teamId;
  }

  public void setTeamId(Integer teamId) {
    this.teamId = teamId;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "invite_members_request.";
    List<String> fields = new ArrayList<>();

    if (teamId != null && teamId < 0) {
      fields.add(fieldName + "team_id");
    }
    if (emails == null) {
      fields.add(fieldName + "emails");
    } else {
      for (String email : emails) {
        if (emailInvalid(email)) {
          fields.add(fieldName + "emails.email");
        }
      }
    }
    return fields;
  }
}
