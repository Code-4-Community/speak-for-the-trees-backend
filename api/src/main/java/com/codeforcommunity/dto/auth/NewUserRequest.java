package com.codeforcommunity.dto.auth;

import com.codeforcommunity.api.ApiDto;
import java.util.ArrayList;
import java.util.List;

public class NewUserRequest extends ApiDto {

  private String email;
  private String username;
  private String password;
  private String firstName;
  private String lastName;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Override
  public List<String> validateFields(String fieldPrefix) {
    String fieldName = fieldPrefix + "new_user_request.";
    List<String> fields = new ArrayList<>();
  }
}
