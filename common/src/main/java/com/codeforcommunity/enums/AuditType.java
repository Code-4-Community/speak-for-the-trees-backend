package com.codeforcommunity.enums;

public enum AuditType {
  INSERT("insert"),
  UPDATE("update"),
  DELETE("delete");

  private String typeName;

  AuditType(String typeName) {
    this.typeName = typeName;
  }

  public static AuditType to(String typeName) {
    for (AuditType auditType : AuditType.values()) {
      if (auditType.typeName.equals(typeName)) {
        return auditType;
      }
    }
    throw new IllegalArgumentException(
        String.format("Given string (%s) that doesn't correspond to any AuditType", typeName));
  }

  public static String from(AuditType auditType) {
    return auditType.typeName;
  }
}
