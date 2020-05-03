package com.codeforcommunity.requester;

import com.codeforcommunity.email.EmailOperations;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class Emailer {
  private final EmailOperations emailOperations;

  public Emailer() {
    Properties emailProperties = PropertiesLoader.getEmailerProperties();
    String sendEmail = emailProperties.getProperty("sendEmail");
    String sendPassword = emailProperties.getProperty("sendPassword");
    String emailHost = emailProperties.getProperty("emailHost");
    int emailPort = Integer.parseInt(emailProperties.getProperty("emailPort"));

    this.emailOperations = new EmailOperations(sendEmail, sendPassword, emailHost, emailPort);
  }

  public void sendWelcomeEmail(String sendToEmail, String sendToName, String verificationLink) {
    String filePath = "/emails/WelcomeEmail.html";
    String subjectLine = "Welcome to Speak For The Trees Boston";

    Map<String, String> templateValues = new HashMap<>();
    templateValues.put("name", sendToName);
    templateValues.put("link", verificationLink);
    Optional<String> emailBody = emailOperations.getTemplateString(filePath, templateValues);

    emailBody.ifPresent(s -> emailOperations.sendEmail(sendToName, sendToEmail, subjectLine, s));
  }
}
