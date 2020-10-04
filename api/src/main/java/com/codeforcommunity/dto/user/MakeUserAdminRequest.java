package com.codeforcommunity.dto.user;

import com.codeforcommunity.dto.ApiDto;
import com.codeforcommunity.exceptions.HandledException;
import java.util.ArrayList;
import java.util.List;

public class MakeUserAdminRequest extends ApiDto {
  private String newAdminEmail;
  private String password;

  public MakeUserAdminRequest(String newAdminEmail, String password) {
    this.newAdminEmail = newAdminEmail;
    this.password = password;
  }

  private MakeUserAdminRequest() {}

  public String getNewAdminEmail() {
    return this.newAdminEmail;
  }

  public void setNewAdminEmail(String newAdminEmail) {
    this.newAdminEmail = newAdminEmail;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) throws HandledException {
    String fieldName = fieldPrefix + "make user admin request.";
    List<String> fields = new ArrayList<>();

    if (emailInvalid(newAdminEmail)) {
      fields.add(fieldName + "newAdminEmail");
    }
    if (password == null) {
      fields.add(fieldName + "password");
    }
    return fields;
  }
}
