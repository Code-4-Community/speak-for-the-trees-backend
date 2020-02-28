package com.codeforcommunity.auth;

public class JWTCreator {
  private final JWTHandler handler;

  public JWTCreator(JWTHandler handler) {
    this.handler = handler;
  }

  public String createNewRefreshToken(JWTData userData) {
    return handler.createNewRefreshToken(userData);
  }

  public String getNewAccessToken(String refreshToken) {
    return handler.getNewAccessToken(refreshToken);
  }


}
