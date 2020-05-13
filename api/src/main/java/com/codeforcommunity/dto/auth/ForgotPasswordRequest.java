package com.codeforcommunity.dto.auth;

public class ForgotPasswordRequest {

  private String email;

  public ForgotPasswordRequest(String email) {
    this.email = email;
  }

  private ForgotPasswordRequest() {}

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
