package com.codeforcommunity.propertiesLoader;

import java.io.IOException;
import java.io.InputStream;
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
}
