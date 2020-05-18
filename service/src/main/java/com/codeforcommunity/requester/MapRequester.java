package com.codeforcommunity.requester;

import com.codeforcommunity.enums.BlockStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MapRequester {
  private final Logger logger = LogManager.getLogger(MapRequester.class);
  private final WebClient client;
  private Future<String> tokenFuture;

  public MapRequester(Vertx vertx) {
    client = WebClient.create(vertx);
    this.tokenFuture = updateToken();
  }

  /**
   * Makes a request to the ArcGIS mapping service to change the status of the all of the blocks in
   * the given list to the specified new block status.
   *
   * @param streetIds a list of block FIDs.
   * @param updateTo The block status all blocks should be updated to.
   */
  public void updateStreets(List<String> streetIds, BlockStatus updateTo) {
    updateLayers(streetIds, updateTo, this.tokenFuture);
  }

  private Future<Void> updateLayers(
      List<String> streetIds, BlockStatus updateTo, Future<String> tokenFuture) {
    /*
    var settings = {
      "url": "https://services7.arcgis.com/iIw2JoTaLFMnHLgW/arcgis/rest/services/boston_street_segments_2/FeatureServer/0/applyEdits",
      "method": "POST",
      "timeout": 0,
      "headers": {
          "Content-Type": "application/x-www-form-urlencoded"
      },
      "data": {
          "f": "json",
          "updates": "[
              {
                  \"attributes\" : {
                  \"FID\" : \"17894\",
                  \"ST_NAME\" : \"EXAMPLE\",
                  \"RESERVED\": \"1\"
                  }
              }
          ]"
      }
     */
    logger.info("Making request to update blocks to " + updateTo.name());

    JsonArray updateJson = new JsonArray();
    streetIds.forEach(
        (fid) -> {
          updateJson.add(JsonObject.mapFrom(new MapRequest(fid, updateTo)));
        });

    Future<Void> updateFuture =
        tokenFuture.compose(
            tokenString -> {
              MultiMap formData =
                  MultiMap.caseInsensitiveMultiMap()
                      .add("f", "json")
                      .add("token", tokenString)
                      .add("updates", updateJson.encode());

              return Future.future(
                  promise -> {
                    client
                        .postAbs(
                            "https://services7.arcgis.com/iIw2JoTaLFMnHLgW/arcgis/rest/services/boston_street_segments_2/FeatureServer/0/applyEdits")
                        .putHeader("content-type", "multipart/form-data")
                        .sendForm(
                            formData,
                            (ar) -> {
                              if (ar.succeeded()) {
                                HttpResponse<Buffer> httpResponse = ar.result();

                                /*
                                 * Invalid Token
                                 *  {"error":{"code":498,"message":"Invalid token.","details":["Invalid token."]}}
                                 * Success
                                 *  {"addResults":[],"updateResults":[{"objectId":2,"uniqueId":2,"globalId":null,"success":true}],"deleteResults":[]}
                                 */
                                if (httpResponse.statusCode() == 200) {
                                  JsonObject responseBody = httpResponse.bodyAsJsonObject();
                                  if (responseBody.containsKey("error")
                                      && responseBody.getJsonObject("error").getInteger("code")
                                          == 498) {
                                    // The API token is invalid, reset it and make this call again.
                                    this.tokenFuture = updateToken();
                                    updateLayers(streetIds, updateTo, this.tokenFuture)
                                        .onSuccess((V) -> promise.complete())
                                        .onFailure(promise::fail);
                                  } else if (responseBody.containsKey("updateResults")) {
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
            });

    return updateFuture;
  }

  private Future<String> updateToken() {
    /*
    var settings = {
      "url": "https://www.arcgis.com/sharing/rest/oauth2/token",
      "method": "POST",
      "timeout": 0,
      "headers": {
          "Content-Type": "application/x-www-form-urlencoded"
      },
      "data": {
          "client_id": "I0YjQpduKSwoHave",
          "client_secret": "ac11027fcd904482828636bce3fbe517",
          "grant_type": "client_credentials"
      }
    };
    */
    logger.info("Making request get an ArcGIS API token");
    MultiMap formData =
        MultiMap.caseInsensitiveMultiMap()
            .add("client_id", "I0YjQpduKSwoHave")
            .add("client_secret", "ac11027fcd904482828636bce3fbe517")
            .add("grant_type", "client_credentials");

    Future<String> newTokenFuture =
        Future.future(
            promise -> {
              client
                  .postAbs("https://www.arcgis.com/sharing/rest/oauth2/token")
                  .putHeader("content-type", "multipart/form-data")
                  .sendForm(
                      formData,
                      ar -> {
                        if (ar.succeeded()) {
                          HttpResponse<Buffer> httpResponse = ar.result();

                          if (httpResponse.statusCode() == 200) {
                            JsonObject responseBody = httpResponse.bodyAsJsonObject();
                            if (responseBody.containsKey("access_token")) {
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
