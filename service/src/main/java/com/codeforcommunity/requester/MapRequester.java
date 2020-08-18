package com.codeforcommunity.requester;

import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.propertiesLoader.PropertiesLoader;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapRequester {
  private final Logger logger = LogManager.getLogger(MapRequester.class);
  private final WebClient client;
  private Future<String> tokenFuture;

  private final String featureLayerApplyEditsRoute;
  private final String featureLayerQueryRoute;
  private final String tokenRoute;
  private final String clientId;
  private final String clientSecret;

  public MapRequester(Vertx vertx) {
    client = WebClient.create(vertx);

    Properties mapProperties = PropertiesLoader.getMapProperties();
    this.featureLayerApplyEditsRoute =
        PropertiesLoader.loadProperty(mapProperties, "feature_layer_route") + "applyEdits";
    this.featureLayerQueryRoute =
        PropertiesLoader.loadProperty(mapProperties, "feature_layer_route") + "query";
    this.tokenRoute = PropertiesLoader.loadProperty(mapProperties, "token_route");
    this.clientId = PropertiesLoader.loadProperty(mapProperties, "client_id");
    this.clientSecret = PropertiesLoader.loadProperty(mapProperties, "client_secret");

    this.tokenFuture = updateToken();
  }

  /**
   * Makes a request to the ArcGIS mapping service to change the status of the all of the blocks in
   * the given list to the specified new block status.
   *
   * @param blockIds a list of block IDs.
   * @param updateTo The block status all blocks should be updated to.
   */
  public void updateBlocks(List<String> blockIds, BlockStatus updateTo) {
    updateLayers(blockIds, updateTo, this.tokenFuture);
  }

  /** Make a request to update the ArcGIS feature layer */
  private Future<Void> updateLayers(
      List<String> blockIds, BlockStatus updateTo, Future<String> tokenFuture) {
    logger.info("Making request to update blocks to " + updateTo.name());

    String BLOCK_ID_QUERY_LIST = "ID IN (" + String.join(",", blockIds) + ")";

    Future<JsonArray> getBlockFIDsFuture =
        tokenFuture.compose(
            tokenString -> {
              MultiMap formData =
                  MultiMap.caseInsensitiveMultiMap()
                      .add("f", "json")
                      .add("token", tokenString)
                      .add("where", BLOCK_ID_QUERY_LIST)
                      .add("outFields", "FID");

              return Future.future(
                  promise -> {
                    client
                        .postAbs(featureLayerQueryRoute)
                        .putHeader("content-type", "multipart/form-data")
                        .sendForm(
                            formData,
                            (asyncResult -> {
                              if (asyncResult.succeeded()) {
                                HttpResponse<Buffer> httpResponse = asyncResult.result();

                                if (httpResponse.statusCode() == 200) {
                                  JsonObject responseBody = httpResponse.bodyAsJsonObject();
                                  if (responseBody.containsKey("features")) {
                                    List<Map> features =
                                        responseBody.getJsonArray("features").getList();
                                    JsonArray updateJson = new JsonArray();
                                    features.forEach(
                                        feature -> {
                                          Map attributes = (Map) feature.get("attributes");
                                          String FID = attributes.get("FID").toString();
                                          updateJson.add(
                                              JsonObject.mapFrom(new MapRequest(FID, updateTo)));
                                        });
                                    promise.complete(updateJson);
                                    return;
                                  }
                                }
                              } else {
                                promise.fail("Error fetching Block FIDs");
                              }
                            }));
                  });
            });

    Future<Void> updateMapFuture =
        tokenFuture.compose(
            tokenString ->
                getBlockFIDsFuture.compose(
                    updateJson -> {
                      MultiMap formData =
                          MultiMap.caseInsensitiveMultiMap()
                              .add("f", "json")
                              .add("token", tokenString)
                              .add("updates", updateJson.encode());

                      return Future.future(
                          promise -> {
                            client
                                .postAbs(featureLayerApplyEditsRoute)
                                .putHeader("content-type", "multipart/form-data")
                                .sendForm(
                                    formData,
                                    (ar) -> {
                                      if (ar.succeeded()) {
                                        HttpResponse<Buffer> httpResponse = ar.result();

                                        if (httpResponse.statusCode() == 200) {
                                          JsonObject responseBody = httpResponse.bodyAsJsonObject();
                                          if (responseBody.containsKey("error")
                                              && responseBody
                                                      .getJsonObject("error")
                                                      .getInteger("code")
                                                  == 498) {
                                            // The API token is invalid, reset it and make this call
                                            // again.
                                            logger.info(
                                                "Remaking token request while updating blocks to "
                                                    + updateTo.name());
                                            this.tokenFuture = updateToken();
                                            updateLayers(blockIds, updateTo, this.tokenFuture)
                                                .onSuccess((V) -> promise.complete())
                                                .onFailure(promise::fail);
                                          } else if (responseBody.containsKey("updateResults")) {
                                            // TODO: NOTE - THIS DOES NOT GUARANTEE A SUCCESS CASE
                                            logger.info(
                                                "Successfully updated blocks to "
                                                    + updateTo.name());
                                            // Check successes
                                            promise.complete();
                                          } else {
                                            // throw error
                                            logger.error(
                                                "Update street request responded with unrecognized response body: "
                                                    + responseBody);
                                          }
                                        } else {
                                          logger.error(
                                              "Update street response request responded with non-200 status code: "
                                                  + httpResponse.statusCode());
                                        }
                                      } else {
                                        logger
                                            .atError()
                                            .withThrowable(ar.cause())
                                            .log("Error sending update street request", ar.cause());
                                        promise.fail(ar.cause());
                                      }
                                    });
                          });
                    }));

    return updateMapFuture;
  }

  /**
   * Query ArcGIS to get a new API token that can be used to make privileged feature layer calls.
   */
  private Future<String> updateToken() {
    logger.info("Making request get an ArcGIS API token");
    MultiMap formData =
        MultiMap.caseInsensitiveMultiMap()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "client_credentials");

    Future<String> newTokenFuture =
        Future.future(
            promise -> {
              client
                  .postAbs(tokenRoute)
                  .putHeader("content-type", "multipart/form-data")
                  .sendForm(
                      formData,
                      ar -> {
                        if (ar.succeeded()) {
                          HttpResponse<Buffer> httpResponse = ar.result();

                          if (httpResponse.statusCode() == 200) {
                            JsonObject responseBody = httpResponse.bodyAsJsonObject();
                            if (responseBody.containsKey("access_token")) {
                              logger.info("Received ArcGIS token successfully");
                              promise.complete(responseBody.getString("access_token"));
                            } else {
                              logger.error(
                                  "ArcGIS token request responded with unrecognized response body: "
                                      + responseBody);
                            }
                          } else {
                            logger.error(
                                "ArcGIS token request responded with non-200 status code: "
                                    + httpResponse.statusCode());
                          }
                        } else {
                          logger
                              .atError()
                              .withThrowable(ar.cause())
                              .log("Error sending ArcGIS token request");
                          promise.fail(ar.cause());
                        }
                      });
            });

    return newTokenFuture;
  }

  private class MapRequest {
    private StreetUpdate attributes;

    public MapRequest(String fid, BlockStatus reserved) {
      this.attributes = new StreetUpdate(fid, reserved);
    }

    public StreetUpdate getAttributes() {
      return attributes;
    }
  }

  /** DTO for ArcGIS API. Important note: needs to be FID for JSON serialization */
  private class StreetUpdate {
    private String FID;
    private String RESERVED;

    public StreetUpdate(String fid, BlockStatus reserved) {
      this.FID = fid;
      this.RESERVED = String.valueOf(reserved.getVal());
    }

    public String getFID() {
      return FID;
    }

    public String getRESERVED() {
      return RESERVED;
    }
  }
}
