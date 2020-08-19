package com.codeforcommunity.propertiesLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class PropertiesLoader {
  private static final String basePath = "properties/";

  private static Properties getProperties(String file) {
    String path = basePath + file;

    try (InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream(path)) {
      Properties prop = new Properties();
      prop.load(input);
      return prop;
    } catch (IOException ex) {
      throw new IllegalArgumentException("Cannot find file: " + path, ex);
    }
  }

  public static String loadProperty(Properties propFile, String propertyName) {
    Optional<String> maybeProperty = Optional.ofNullable(propFile.getProperty(propertyName));
    if (maybeProperty.isPresent()) {
      return maybeProperty.get();
    } else {
      throw new IllegalArgumentException(
          String.format("No property found %s in property file", propertyName));
    }
  }

  public static Properties getEmailerProperties() {
    return getProperties("emailer.properties");
  }

  public static Properties getDbProperties() {
    return getProperties("db.properties");
  }

  public static Properties getExpirationProperties() {
    return getProperties("expiration.properties");
  }

  public static Properties getJwtProperties() {
    return getProperties("jwt.properties");
  }

  public static Properties getFrontendProperties() {
    return getProperties("frontend.properties");
  }

  public static Properties getMapProperties() {
    return getProperties("map.properties");
  }

  public static Properties getSlackProperties() {
    return getProperties("slack.properties");
  }
}
