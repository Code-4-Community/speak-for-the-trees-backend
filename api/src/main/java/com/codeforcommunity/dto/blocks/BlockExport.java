package com.codeforcommunity.dto.blocks;

import java.sql.Timestamp;
import java.util.List;

public class BlockExport {
  private String id;
  private String status;
  private Timestamp updatedTimestamp;
  private String firstName;
  private String lastName;
  private String email;
  private String username;
  private int userId;
  private String teamNames;

  /** Constructor for Block & User query. User Team names must be set separately. */
  public BlockExport(
      String id,
      String status,
      Timestamp updatedTimestamp,
      String firstName,
      String lastName,
      String email,
      String username,
      int userId) {
    this.id = id;
    this.status = status;
    this.updatedTimestamp = updatedTimestamp;
    this.firstName = firstName != null ? firstName : "";
    this.lastName = lastName != null ? lastName : "";
    this.email = email != null ? email : "";
    this.username = username != null ? username : "";
    this.userId = userId;
    this.teamNames = "";
  }

  public String getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }

  public Timestamp getUpdatedTimestamp() {
    return updatedTimestamp;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getUsername() {
    return username;
  }

  public int getUserId() {
    return userId;
  }

  public String getTeamNames() {
    return teamNames;
  }

  public void setTeamNames(List<String> teamNamesList) {
    String names = (teamNamesList == null) ? "" : String.join(", ", teamNamesList);
    this.teamNames = (names.equals("null")) ? "" : names;
  }

  public static String getHeaderCSV() {
    return "Block Id,"
        + "Block Status,"
        + "Last Updated,"
        + "First Name,"
        + "Last Name,"
        + "Email,"
        + "Username,"
        + "User ID,"
        + "User Teams\n";
  }

  public String getRowCSV() {
    return this.id
        + ","
        + this.status
        + ","
        + this.updatedTimestamp.toString()
        + ","
        + this.firstName
        + ","
        + this.lastName
        + ","
        + this.email
        + ","
        + this.username
        + ","
        + (this.userId == 0 ? "" : this.userId)
        + ","
        + "\""
        + this.teamNames
        + "\"\n";
  }
}
