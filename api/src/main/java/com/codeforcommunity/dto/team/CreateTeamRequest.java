package com.codeforcommunity.dto.team;

import com.codeforcommunity.api.ApiDto;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CreateTeamRequest extends ApiDto {
  private String name;
  private String bio;
  private Integer goal;
  private Timestamp goalCompletionDate;
  private List<TeamInvitationRequest> invites;

  public CreateTeamRequest(
      String name,
      String bio,
      int goal,
      Timestamp goalCompletionDate,
      List<TeamInvitationRequest> invites) {
    this.name = name;
    this.bio = bio;
    this.goal = goal;
    this.goalCompletionDate = goalCompletionDate;
    this.invites = invites;
  }

  private CreateTeamRequest() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public int getGoal() {
    return goal;
  }

  public void setGoal(int goal) {
    this.goal = goal;
  }

  public Timestamp getGoalCompletionDate() {
    return goalCompletionDate;
  }

  public void setGoalCompletionDate(Timestamp goalCompletionDate) {
    this.goalCompletionDate = goalCompletionDate;
  }

  public List<TeamInvitationRequest> getInvites() {
    return invites;
  }

  public void setInvites(List<TeamInvitationRequest> invites) {
    this.invites = invites;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "create_team_request.";
    List<String> fields = new ArrayList<>();

    if (isEmpty(name)) {
      fields.add(fieldName + "name");
    }
    if (bio == null) {
      fields.add(fieldName + "bio");
    }
    if (goal != null && goal < 0) {
      fields.add(fieldName + "goal");
    }
    if (invites == null) {
      fields.add(fieldName + "invites");
    } else {
      for (TeamInvitationRequest invite : invites) {
        fields.addAll(invite.validateFields(fieldName));
      }
    }
    return fields;
  }
}
