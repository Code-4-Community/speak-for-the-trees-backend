package com.codeforcommunity.dto.team;

import java.sql.Timestamp;

public class AdminTeamSummary {
  private int id;
  private String name;
  private Timestamp goalCompletionDate;
  private int blocksCompleted;
  private int blocksReserved;
  private int goal;

  public AdminTeamSummary() {}

  public AdminTeamSummary(
      int id,
      String name,
      Timestamp goalCompletionDate,
      int blocksCompleted,
      int blocksReserved,
      int goal) {
    this.id = id;
    this.name = name;
    this.goalCompletionDate = goalCompletionDate;
    this.blocksCompleted = blocksCompleted;
    this.blocksReserved = blocksReserved;
    this.goal = goal;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Timestamp getGoalCompletionDate() {
    return goalCompletionDate;
  }

  public int getBlocksCompleted() {
    return blocksCompleted;
  }

  public int getBlocksReserved() {
    return blocksReserved;
  }

  public int getGoal() {
    return goal;
  }
}
