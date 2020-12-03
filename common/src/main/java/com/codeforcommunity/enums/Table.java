package com.codeforcommunity.enums;

public enum Table {
  USERS("users"),
  TEAM("team"),
  USER_TEAM("user_team");

  private String tableName;

  Table(String tableName) {
    this.tableName = tableName;
  }

  public static Table from(String tableName) {
    for (Table table : Table.values()) {
      if (table.tableName.equals(tableName)) {
        return table;
      }
    }
    throw new IllegalArgumentException(
        String.format("Given string (%s) that doesn't correspond to any Table", tableName));
  }

  public static String to(Table table) {
    return table.tableName;
  }
}
