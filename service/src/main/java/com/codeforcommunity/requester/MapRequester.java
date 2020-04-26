package com.codeforcommunity.requester;

import com.codeforcommunity.enums.BlockStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MapRequester {
  private WebClient client;
  private CompletableFuture<String> tokenFuture;

  public MapRequester() {
    Vertx vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    this.tokenFuture = updateToken();
  }

  /**
   * Makes a request to the ArcGIS mapping service to change the status of the all of the blocks
   * in the given list to the specified new block status.
   *
   * @param streetIds a list of block FIDs.
   * @param updateTo The block status all blocks should be updated to.
   */
  public void updateStreets(List<String> streetIds, BlockStatus updateTo) {
    updateLayers(streetIds, updateTo, this.tokenFuture);
  }

  private void updateLayers(List<String> streetIds, BlockStatus updateTo, CompletableFuture<String> tokenFuture) {
    /*
    var settings = {
      "url": "https://services7.arcgis.com/iIw2JoTaLFMnHLgW/ArcGIS/rest/services/boston_street_segments_1/FeatureServer/0/applyEdits",
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
  };
     */
    this.tokenFuture.thenAccept(tokenString -> {
      JsonArray updateJson = new JsonArray();
      streetIds.forEach((fid) -> {
        updateJson.add(JsonObject.mapFrom(new MapRequest(fid, updateTo)));
      });

      MultiMap formData = MultiMap.caseInsensitiveMultiMap()
          .add("f", "json")
          .add("token", tokenString)
          .add("updates", updateJson.encode());
      client.post("https://services7.arcgis.com", "/iIw2JoTaLFMnHLgW/ArcGIS/rest/services/boston_street_segments_1/FeatureServer/0/applyEdits")
          .sendForm(formData, (ar) -> {
            if (ar.succeeded()) {
              //TODO: Check the response to see if token is invalid
              System.out.println("Hooray"); //TODO: Actually log
            } else {
              throw new IllegalStateException(ar.cause());
            }
          });
    });
  }


  private CompletableFuture<String> updateToken() {
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
    CompletableFuture<String> newTokenFuture = new CompletableFuture<>();
    MultiMap formData = MultiMap.caseInsensitiveMultiMap()
        .add("client_id", "I0YjQpduKSwoHave")
        .add("client_secret", "ac11027fcd904482828636bce3fbe517")
        .add("grant_type", "client_credentials");

    client.post("https://www.arcgis.com", "/sharing/rest/oauth2/token")
        .sendForm(formData, ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();

            String newAccessToken = response.bodyAsJsonObject().getString("access_token");
            newTokenFuture.complete(newAccessToken);
          } else {
            newTokenFuture.completeExceptionally(new KillYourselfException());
          }
        });

    return newTokenFuture;
  }

  private static class KillYourselfException extends RuntimeException {}

  private class MapRequest {
    private StreetUpdate attributes;
    public MapRequest(String fid, BlockStatus reserved) {
      this.attributes = new StreetUpdate(fid, reserved);
    }
  }

  private class StreetUpdate {
    private String FID;
    private String RESERVED;
    public StreetUpdate(String fid, BlockStatus reserved) {
      this.FID = fid;
      this.RESERVED = String.valueOf(reserved.getVal());
    }
  }
}
