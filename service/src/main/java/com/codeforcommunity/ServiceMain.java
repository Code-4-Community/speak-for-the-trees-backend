package com.codeforcommunity;

import com.codeforcommunity.api.IAuthProcessor;
import com.codeforcommunity.api.IBlockInfoProcessor;
import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.api.IProtectedUserProcessor;
import com.codeforcommunity.api.ITeamsProcessor;
import com.codeforcommunity.auth.JWTAuthorizer;
import com.codeforcommunity.auth.JWTCreator;
import com.codeforcommunity.auth.JWTHandler;
import com.codeforcommunity.processor.AuthProcessorImpl;
import com.codeforcommunity.processor.BlockInfoProcessorImpl;
import com.codeforcommunity.processor.BlocksProcessorImpl;
import com.codeforcommunity.processor.ProtectedUserProcessorImpl;
import com.codeforcommunity.processor.TeamsProcessorImpl;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import com.codeforcommunity.requester.Emailer;
import com.codeforcommunity.requester.MapRequester;
import com.codeforcommunity.rest.ApiRouter;
import io.vertx.core.Vertx;
import java.util.Properties;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

public class ServiceMain {
  private DSLContext db;
  private final Properties dbProperties = PropertiesLoader.getDbProperties();

  public static void main(String[] args) {
    try {
      ServiceMain serviceMain = new ServiceMain();
      serviceMain.initialize();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Start the server, get everything going. */
  public void initialize() {
    setUpSystemProperties();
    connectDb();
    initializeServer();
  }

  /** Adds any necessary system properties. */
  private void setUpSystemProperties() {
    Properties systemProperties = System.getProperties();
    systemProperties.setProperty(
        "vertx.logger-delegate-factory-class-name",
        "io.vertx.core.logging.SLF4JLogDelegateFactory");
    System.setProperties(systemProperties);
  }

  /** Connect to the database and create a DSLContext so jOOQ can interact with it. */
  private void connectDb() {
    // This block ensures that the Postgres driver is loaded in the classpath
    try {
      Class.forName(dbProperties.getProperty("database.driver"));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    DSLContext db =
        DSL.using(
            dbProperties.getProperty("database.url"),
            dbProperties.getProperty("database.username"),
            dbProperties.getProperty("database.password"));
    this.db = db;
  }

  /** Initialize the server and get all the supporting classes going. */
  private void initializeServer() {
    JWTHandler jwtHandler =
        new JWTHandler(PropertiesLoader.getJwtProperties().getProperty("secret_key"));
    JWTAuthorizer jwtAuthorizer = new JWTAuthorizer(jwtHandler);
    JWTCreator jwtCreator = new JWTCreator(jwtHandler);

    Vertx vertx = Vertx.vertx();
    MapRequester mapRequester = new MapRequester(vertx);
    Emailer emailer = new Emailer();

    IAuthProcessor authProcessor = new AuthProcessorImpl(this.db, jwtCreator);
    IProtectedUserProcessor protectedUserProcessor = new ProtectedUserProcessorImpl(this.db);
    IBlockProcessor blockProcessor = new BlocksProcessorImpl(this.db, mapRequester);
    IBlockInfoProcessor blockInfoProcessor = new BlockInfoProcessorImpl(this.db);
    ITeamsProcessor teamsProcessor = new TeamsProcessorImpl(this.db, emailer);
    ApiRouter router =
        new ApiRouter(
            authProcessor,
            protectedUserProcessor,
            blockProcessor,
            blockInfoProcessor,
            teamsProcessor,
            jwtAuthorizer);

    startApiServer(router, vertx);
  }

  /** Start up the actual API server that will listen for requests. */
  private void startApiServer(ApiRouter router, Vertx vertx) {
    ApiMain apiMain = new ApiMain(router);
    apiMain.startApi(vertx);
  }
}
