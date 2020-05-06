package com.codeforcommunity.requester;

import com.codeforcommunity.email.EmailOperations;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import org.jooq.generated.tables.pojos.Team;
import org.jooq.generated.tables.pojos.Users;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class Emailer {
  private final EmailOperations emailOperations;
  private final String teamPageUrlTemplate;

  public Emailer() {
    Properties emailProperties = PropertiesLoader.getEmailerProperties();
    String senderName = emailProperties.getProperty("senderName");
    String sendEmail = emailProperties.getProperty("sendEmail");
    String sendPassword = emailProperties.getProperty("sendPassword");
    String emailHost = emailProperties.getProperty("emailHost");
    int emailPort = Integer.parseInt(emailProperties.getProperty("emailPort"));

    this.emailOperations = new EmailOperations(senderName, sendEmail, sendPassword, emailHost, emailPort);

    Properties frontendProperties = PropertiesLoader.getFrontendProperties();
    this.teamPageUrlTemplate = frontendProperties.getProperty("base_url") + frontendProperties.getProperty("team_page_route");
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

  public void sendInviteEmail(String sendToEmail, String sendToName, Users inviter, Team invitedTeam) {
    String filePath = "/emails/InviteEmail.html";
    String subjectLine = String.format("You've Been Invited to Join %s's Team!", inviter.getFirstName());

    Map<String, String> templateValues = new HashMap<>();
    templateValues.put("inviter name", String.format("%s %s", inviter.getFirstName(), inviter.getLastName()));
    templateValues.put("link", String.format(this.teamPageUrlTemplate, invitedTeam.getId()));
    Optional<String> emailBody = emailOperations.getTemplateString(filePath, templateValues);

    emailBody.ifPresent(s -> emailOperations.sendEmail(sendToName, sendToEmail, subjectLine, s));
  }
}
