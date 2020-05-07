package com.codeforcommunity.api;

import com.codeforcommunity.auth.JWTData;

public interface IProtectedUserProcessor {

  /**
   * Deletes the given user from the database. Does NOT invalidate the user's JWTs
   */
  void deleteUser(JWTData userData);

}
