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

public class Requester {
  private WebClient client;

  private void startThings() {
    Vertx vertx = Vertx.vertx();
    client = WebClient.create(vertx);

  }

  public void updateStreets(List<String> streetIds, BlockStatus updateTo) {
    // TODO
  }

  private void updateLayers() {
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
    JsonArray updateJson = new JsonArray(); //TODO
    MultiMap formData = MultiMap.caseInsensitiveMultiMap()
        .add("f", "json")
        .add("token", "") //TODO
        .add("updates", updateJson.encode());
    client.post("https://services7.arcgis.com", "/iIw2JoTaLFMnHLgW/ArcGIS/rest/services/boston_street_segments_1/FeatureServer/0/applyEdits");
  }


  private void updateToken() {
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
    MultiMap formData = MultiMap.caseInsensitiveMultiMap()
        .add("client_id", "I0YjQpduKSwoHave")
        .add("client_secret", "ac11027fcd904482828636bce3fbe517")
        .add("grant_type", "client_credentials");
    client.post("https://www.arcgis.com", "/sharing/rest/oauth2/token")
        .sendForm(formData, ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();

            String newAccessToken = response.bodyAsJsonObject().getString("access_token");
          } else {
            throw new KillYourselfException();
          }
        });

  }

  private static class KillYourselfException extends RuntimeException {}
}
