package com.example.locationtracker2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class NavActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

  private final String SOURCE_ID = "destination-source-id";
  private final String ICON_ID = "destination-icon-id";
  private final String LAYER_ID = "destination-symbol-layer-id";
  SharedPreferences sharedPref;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private Point destinationPoint, originPoint;
  private DirectionsRoute currentRoute;
  private NavigationMapRoute navigationMapRoute;
  // variables for adding location layer
  private PermissionsManager permissionsManager;
  private LocationComponent locationComponent;
  private Button button;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

    setContentView(R.layout.activity_nav);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    sharedPref = getSharedPreferences("com.example.locationtracker2.TRANSFER_DETAILS", Context.MODE_PRIVATE);
    float destinationLatitude = sharedPref.getFloat("destinationLatitude", 0);
    float destinationLongitude = sharedPref.getFloat("destinationLongitude", 0);
    destinationPoint = Point.fromLngLat(destinationLongitude, destinationLatitude);
  }

  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        enableLocationComponent(style);

        addDestinationIconSymbolLayer(style);

        button = findViewById(R.id.startButton);
        button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            boolean simulateRoute = false;
            NavigationLauncherOptions options = NavigationLauncherOptions.builder().directionsRoute(currentRoute).shouldSimulateRoute(simulateRoute).build();
            NavigationLauncher.startNavigation(NavActivity.this, options);
          }
        });
      }
    });
  }


  private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
    loadedMapStyle.addImage(ICON_ID, BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));

    GeoJsonSource geoJsonSource = new GeoJsonSource(SOURCE_ID);
    geoJsonSource.setGeoJson(Feature.fromGeometry(destinationPoint));

    loadedMapStyle.addSource(geoJsonSource);
    SymbolLayer destinationSymbolLayer = new SymbolLayer(LAYER_ID, SOURCE_ID);
    destinationSymbolLayer.withProperties(
      iconImage(ICON_ID),
      iconAllowOverlap(true),
      iconIgnorePlacement(true)
    );

    loadedMapStyle.addLayer(destinationSymbolLayer);
    Log.d("myTag", "Destination symbol layer added");


  }

  private void getRoute(Point origin, Point destination) {
    NavigationRoute.builder(this)
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination)
      .build()
      .getRoute(new Callback<DirectionsResponse>() {
        @Override
        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
          Log.d("myTag", "getRoute: Response Code: " + response.code());
          if (response.body() == null) {
            Log.e("myTag", "getRoute: No routes found, make sure you set the right user and access token.");
            return;
          } else if (response.body().routes().size() < 1) {
            Log.e("myTag", "getRoute: No routes found");
            return;
          }

          currentRoute = response.body().routes().get(0);

          // Draw the route on the map
          if (navigationMapRoute != null) {
            navigationMapRoute.removeRoute();
          } else {
            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
          }
          navigationMapRoute.addRoute(currentRoute);
          button.setEnabled(true);
          button.setBackgroundResource(R.color.mapboxBlue);
        }

        @Override
        public void onFailure(Call<DirectionsResponse> call, Throwable t) {
          Log.e("myTag", "getRoute: Error: " + t.getMessage());
        }
      });
  }

  @SuppressLint("MissingPermission")
  @SuppressWarnings({"MissingPermissions"})
  private void enableLocationComponent(@NonNull Style loadedMapStyle) {
    // Check if permissions are enabled and if not requested
    if (PermissionsManager.areLocationPermissionsGranted(this)) {
      // Activate the MapboxMap LocationComponent to show user location
      // Adding in LocationComponentOptions is also an optional param
      locationComponent = mapboxMap.getLocationComponent();
      locationComponent.activateLocationComponent(this, loadedMapStyle);
      locationComponent.setLocationComponentEnabled(true);
      locationComponent.setCameraMode(CameraMode.TRACKING);

      locationComponent.getLocationEngine().getLastLocation(new LocationEngineCallback<LocationEngineResult>() {
        @Override
        public void onSuccess(LocationEngineResult result) {
          Location location = result.getLastLocation();
          if (location != null) {
            originPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
            getRoute(originPoint, destinationPoint);
          }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
          Log.d("myTag", "location engine getLastLocation onFailure");
          Log.e("myTag", exception.getMessage());
        }
      });
    } else {
      permissionsManager = new PermissionsManager(this);
      permissionsManager.requestLocationPermissions(this);
    }
  }


  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      enableLocationComponent(mapboxMap.getStyle());
    } else {
      Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
      finish();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}