package com.codeforcommunity.dto.auth;

public class ResetPasswordRequest {

  private String secretKey;
  private String newPassword;

  public ResetPasswordRequest(String secretKey, String newPassword) {
    this.secretKey = secretKey;
    this.newPassword = newPassword;
  }

  private ResetPasswordRequest() {}

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
