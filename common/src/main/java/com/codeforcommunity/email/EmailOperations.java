package com.codeforcommunity.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simplejavamail.MailException;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.AsyncResponse;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class EmailOperations {
  private static final Logger logger = LogManager.getLogger(EmailOperations.class);

  private final String senderName;
  private final String sendEmail;
  private final Mailer mailer;

  public EmailOperations(String senderName, String sendEmail, String sendPassword, String emailHost, int emailPort) {
    this.senderName = senderName;
    this.sendEmail = sendEmail;
    this.mailer = MailerBuilder
        .withSMTPServer(emailHost, emailPort, sendEmail, sendPassword)
        .withTransportStrategy(TransportStrategy.SMTPS)
        .async()
        .buildMailer();
  }

  /**
   * Read the given email template into a string replacing any placeholder strings with their
   * value in the given map.
   *
   * A placeholder string is any string in a {@code ${...}} block. It is expected that the wrapped
   * string is a key in the given map. The entire {@code ${...}} block will be replaced by the key's
   * value in the map.
   *
   * If an exception is encountered while reading the file {@code Optional.empty()} will be returned
   */
  public Optional<String> getTemplateString(String templateFilePath, Map<String, String> tagValues) {
    FileReader fr;
    try {
      File templateFile = new File(EmailOperations.class.getResource(templateFilePath).getFile());
      fr = new FileReader(templateFile);
    } catch (FileNotFoundException | NullPointerException e) {
      logger.atError().withThrowable(e).log("Could not find the specified email template file at" + templateFilePath);
      return Optional.empty();
    }

    boolean readingTag = false;
    StringBuilder output = new StringBuilder();
    StringBuilder tag = new StringBuilder();

    try {
      int next = fr.read();
      while (next != -1) {
        char c = (char) next;
        if (readingTag) {
          if (c == '}') {
            output.append(tagValues.getOrDefault(tag.toString(), tag.toString()));
            readingTag = false;
            tag = new StringBuilder();
          } else {
            tag.append(c);
          }
          next = fr.read();
        } else {
          if (c == '$') {
            next = fr.read();
            if (next == '{') {
              readingTag = true;
              next = fr.read();
            } else {
              output.append('$');
            }
          } else {
            output.append(c);
            next = fr.read();
          }
        }
      }
    } catch (IOException e) {
      logger.atError().withThrowable(e).log("Threw IO exception while reading template file at " + templateFilePath);
      return Optional.empty();
    }

    return Optional.of(output.toString());
  }

  /**
   * Send an email with the given subject and body to the user with the given name at the given
   * email.
   */
  public void sendEmail(String sendToName, String sendToEmail, String subject, String emailBody) {
    logger.info("Sending email subject " + subject);

    Email email = EmailBuilder.startingBlank()
        .from(senderName, sendEmail)
        .to(sendToName, sendToEmail)
        .withSubject(subject)
        .withHTMLText(emailBody)
        .buildEmail();

    try {
      AsyncResponse mailResponse = mailer.sendMail(email, true);
      mailResponse.onException((e) -> {
        logger.atError().withThrowable(e).log("Threw exception while sending email subject " + subject);
      });
      mailResponse.onSuccess(() -> {
        logger.info("Successfully sent email subject " + subject);
      });
    } catch (MailException e) {
      logger.atError().withThrowable(e).log("Threw exception while sending email subject" + subject);
    }
  }
}
