package com.codeforcommunity.dto.team;

import java.sql.Timestamp;

public class TeamsExport {
  private int teamId;
  private String teamName;
  private String teamBio;
  private String teamGoal;
  private String teamGoalCompletionDate;
  private String teamCreatedAt;
  private String userId;
  private String teamRole;
  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private String privilegeLevel;
  private String blocksReserved;
  private String blocksCompleted;

  /** Constructor for creating a TeamsExport from a Team query. */
  public TeamsExport(
      int teamId,
      String teamName,
      String teamBio,
      String teamGoal,
      Timestamp teamGoalCompletionDate,
      Timestamp teamCreatedAt) {
    this.teamId = teamId;
    this.teamName = teamName;
    this.teamBio = teamBio;
    this.teamGoal = teamGoal;
    this.teamGoalCompletionDate = teamGoalCompletionDate.toString();
    this.teamCreatedAt = teamCreatedAt.toString();
    this.userId = "";
    this.teamRole = "";
    this.firstName = "";
    this.lastName = "";
    this.username = "";
    this.email = "";
    this.privilegeLevel = "";
    this.blocksReserved = "";
    this.blocksCompleted = "";
  }

  /** Constructor for creating a TeamsExport from a User/Block query. */
  public TeamsExport(
      int userId,
      int teamId,
      String teamRole,
      String firstName,
      String lastName,
      String username,
      String email,
      String privilegeLevel,
      int blocksReserved,
      int blocksCompleted) {
    this.teamId = teamId;
    this.userId = String.valueOf(userId);
    this.teamRole = teamRole;
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.email = email;
    this.privilegeLevel = privilegeLevel;
    this.blocksReserved = String.valueOf(blocksReserved);
    this.blocksCompleted = String.valueOf(blocksCompleted);
    this.teamName = "";
    this.teamBio = "";
    this.teamGoal = "";
    this.teamGoalCompletionDate = "";
    this.teamCreatedAt = "";
  }

  public int getTeamId() {
    return teamId;
  }

  public String getTeamName() {
    return teamName;
  }

  public String getTeamBio() {
    return teamBio;
  }

  public String getTeamGoal() {
    return teamGoal;
  }

  public String getTeamGoalCompletionDate() {
    return teamGoalCompletionDate;
  }

  public String getUserId() {
    return userId;
  }

  public String getTeamRole() {
    return teamRole;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPrivilegeLevel() {
    return privilegeLevel;
  }

  public String getBlocksReserved() {
    return blocksReserved;
  }

  public String getBlocksCompleted() {
    return blocksCompleted;
  }

  /**
   * Get this user-team object as a formatted string for a CSV title row.
   *
   * @return the comma-separated field names of this object.
   */
  public static String getHeaderCSV() {
    return "Team ID,"
        + "Team Name,"
        + "Team Bio,"
        + "Team Goal,"
        + "Team Goal Completion Date,"
        + "Team Created Datetime,"
        + "User ID,"
        + "Team Role,"
        + "First Name,"
        + "Last Name,"
        + "Username,"
        + "Email,"
        + "Privilege Level,"
        + "Blocks Reserved,"
        + "Blocks Completed\n";
  }

  /**
   * Get this user-team object as a formatted string for a CSV.
   *
   * @return the comma-separated field values of this object.
   */
  public String getRowCSV() {
    return this.teamId
        + ","
        + this.teamName
        + ","
        + this.teamBio
        + ","
        + this.teamGoal
        + ","
        + this.teamGoalCompletionDate
        + ","
        + this.teamCreatedAt
        + ","
        + this.userId
        + ","
        + this.teamRole
        + ","
        + this.firstName
        + ","
        + this.lastName
        + ","
        + this.username
        + ","
        + this.email
        + ","
        + this.privilegeLevel
        + ","
        + this.blocksReserved
        + ","
        + this.blocksCompleted
        + "\n";
  }
}
