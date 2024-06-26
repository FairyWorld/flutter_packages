// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import io.flutter.plugin.common.MethodChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PolygonsController {

  private final Map<String, PolygonController> polygonIdToController;
  private final Map<String, String> googleMapsPolygonIdToDartPolygonId;
  private final MethodChannel methodChannel;
  private final float density;
  private GoogleMap googleMap;

  PolygonsController(MethodChannel methodChannel, float density) {
    this.polygonIdToController = new HashMap<>();
    this.googleMapsPolygonIdToDartPolygonId = new HashMap<>();
    this.methodChannel = methodChannel;
    this.density = density;
  }

  void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
  }

  void addJsonPolygons(List<Object> polygonsToAdd) {
    if (polygonsToAdd != null) {
      for (Object polygonToAdd : polygonsToAdd) {
        addJsonPolygon(polygonToAdd);
      }
    }
  }

  void addPolygons(@NonNull List<Messages.PlatformPolygon> polygonsToAdd) {
    for (Messages.PlatformPolygon polygonToAdd : polygonsToAdd) {
      addJsonPolygon(polygonToAdd.getJson());
    }
  }

  void changePolygons(@NonNull List<Messages.PlatformPolygon> polygonsToChange) {
    for (Messages.PlatformPolygon polygonToChange : polygonsToChange) {
      changeJsonPolygon(polygonToChange.getJson());
    }
  }

  void removePolygons(@NonNull List<String> polygonIdsToRemove) {
    for (String polygonId : polygonIdsToRemove) {
      final PolygonController polygonController = polygonIdToController.remove(polygonId);
      if (polygonController != null) {
        polygonController.remove();
        googleMapsPolygonIdToDartPolygonId.remove(polygonController.getGoogleMapsPolygonId());
      }
    }
  }

  boolean onPolygonTap(String googlePolygonId) {
    String polygonId = googleMapsPolygonIdToDartPolygonId.get(googlePolygonId);
    if (polygonId == null) {
      return false;
    }
    methodChannel.invokeMethod("polygon#onTap", Convert.polygonIdToJson(polygonId));
    PolygonController polygonController = polygonIdToController.get(polygonId);
    if (polygonController != null) {
      return polygonController.consumeTapEvents();
    }
    return false;
  }

  private void addJsonPolygon(Object polygon) {
    if (polygon == null) {
      return;
    }
    PolygonBuilder polygonBuilder = new PolygonBuilder(density);
    String polygonId = Convert.interpretPolygonOptions(polygon, polygonBuilder);
    PolygonOptions options = polygonBuilder.build();
    addPolygon(polygonId, options, polygonBuilder.consumeTapEvents());
  }

  private void addPolygon(
      String polygonId, PolygonOptions polygonOptions, boolean consumeTapEvents) {
    final Polygon polygon = googleMap.addPolygon(polygonOptions);
    PolygonController controller = new PolygonController(polygon, consumeTapEvents, density);
    polygonIdToController.put(polygonId, controller);
    googleMapsPolygonIdToDartPolygonId.put(polygon.getId(), polygonId);
  }

  private void changeJsonPolygon(Object polygon) {
    if (polygon == null) {
      return;
    }
    String polygonId = getPolygonId(polygon);
    PolygonController polygonController = polygonIdToController.get(polygonId);
    if (polygonController != null) {
      Convert.interpretPolygonOptions(polygon, polygonController);
    }
  }

  @SuppressWarnings("unchecked")
  private static String getPolygonId(Object polygon) {
    Map<String, Object> polygonMap = (Map<String, Object>) polygon;
    return (String) polygonMap.get("polygonId");
  }
}
