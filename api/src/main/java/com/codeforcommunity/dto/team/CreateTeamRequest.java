package com.codeforcommunity.dto.team;

import com.codeforcommunity.exceptions.MalformedParameterException;
import java.sql.Timestamp;
import java.util.List;

public class CreateTeamRequest {
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

  /**
   * Validates the request.
   *
   * @throws MalformedParameterException if any of the request parameters are invalid
   */
  public void validate() {
    if (goal == null && goal < 0) {
      throw new MalformedParameterException("goal");
    }
  }
}
