package com.codeforcommunity.dto.team;

import java.sql.Timestamp;
import java.util.List;

public class TeamResponse {

  private int id;
  private String name;
  private String bio;
  private int goal;
  private Timestamp goalCompleteDate;
  private int blocksCompleted;
  private int blocksReserved;
  List<TeamMember> members;

  private TeamResponse() {}

  public TeamResponse(int id, String name, String bio, int goal, Timestamp goalCompleteDate,
      int blocksCompleted, int blocksReserved,
      List<TeamMember> members) {
    this.id = id;
    this.name = name;
    this.bio = bio;
    this.goal = goal;
    this.goalCompleteDate = goalCompleteDate;
    this.blocksCompleted = blocksCompleted;
    this.blocksReserved = blocksReserved;
    this.members = members;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getBio() {
    return bio;
  }

  public int getGoal() {
    return goal;
  }

  public Timestamp getGoalCompleteDate() {
    return goalCompleteDate;
  }

  public int getBlocksCompleted() {
    return blocksCompleted;
  }

  public int getBlocksReserved() {
    return blocksReserved;
  }

  public List<TeamMember> getMembers() {
    return members;
  }
}
