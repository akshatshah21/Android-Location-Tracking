package com.example.locationtracker2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class LocationTrackerService extends Service implements LocationListener {
  private final Context mContext;
  boolean isGPSEnabled = false;
  boolean isNetworkEnabled = false;

  Location location;
  double latitude, longitude;
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
  private static final long MIN_TIME_BW_UPDATES = 1000 * 6;

  protected LocationManager locationManager;

  private static AsyncHttpClient client;
  private static final String url = "http://192.168.29.2:5000";

  private Socket socket;
  Timer timer;
  TimerTask timerTask;
  final Handler handler = new Handler();

  @SuppressLint("MissingPermission")
  public LocationTrackerService(Context context) {
    this.mContext = context;
    client = new AsyncHttpClient();
    try {
      locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
      isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!isGPSEnabled && !isNetworkEnabled) {
        //     no location provider enabled
        Log.i("myTag", "No location provider enabled");
        Toast.makeText(context, "No location permissions", Toast.LENGTH_SHORT).show();
        showSettingsAlert(); // check
      } else {
        //    First get location from Network Provider
        try {
          if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(
              LocationManager.NETWORK_PROVIDER,
              MIN_TIME_BW_UPDATES,
              MIN_DISTANCE_CHANGE_FOR_UPDATES,
              this
            );
          }
        } catch (SecurityException secExp) {
          secExp.printStackTrace();
        }

        //    Get location from GPS
        try {
          if (isGPSEnabled) {
            locationManager.requestLocationUpdates(
              LocationManager.GPS_PROVIDER,
              0,
              0,
              this
            );
          }
        } catch (SecurityException secExp) {
          secExp.printStackTrace();
        }

        pushLocationUpdates();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void showSettingsAlert() {
    AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
    alertDialog.setTitle("Location Settings");
    alertDialog.setMessage("Location is not enabled. Do you want to go to settings menu?");
    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        mContext.startActivity(intent);
      }
    });
    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        dialogInterface.cancel();
      }
    });
    alertDialog.show();
  }

  private void pushLocationUpdates() {
    initSocketConn();
    timer = new Timer();
    initializeTimerTask();
    timer.schedule(timerTask, 1000, 5000);
  }

  public void initSocketConn() {
    try {
      socket = IO.socket(url);

      socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

        @Override
        public void call(Object... args) {
          Log.d("myTag", "Socket.EVENT_CONNECT");
        }
      });

      socket.connect();


    } catch (URISyntaxException e) {
      Log.d("myTag", e.getMessage().toString());
    }
  }

  private void initializeTimerTask() {
    timerTask = new TimerTask() {
      @Override
      public void run() {
        handler.post(new Runnable() {
          @Override
          public void run() {
            JSONObject locationJson = new JSONObject();
            try {
              locationJson.put("longitude", longitude);
              locationJson.put("latitude", latitude);
              socket.emit("location-update", locationJson);
            } catch (JSONException e) {
              e.printStackTrace();
            }

          }
        });
      }
    };
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
      Log.i("myTag",
        "Latitude:" + location.getLatitude() +
          " Longitude:" + location.getLongitude());
      latitude = location.getLatitude();
      longitude = location.getLongitude();
    }
  }

  @Override
  public void onStatusChanged(String s, int i, Bundle bundle) {

  }

  public void stopUsingLocationService() {
    socket.disconnect();
    if (locationManager != null) {
      locationManager.removeUpdates(LocationTrackerService.this);
    }
  }

  @Override
  public void onProviderEnabled(String s) {
    Log.i("myTag", "Provider enabled: " + s);
    Toast.makeText(getApplicationContext(), "Provider enabled: " + s, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onProviderDisabled(String s) {
    Log.i("myTag", "Provider disabled: " + s);
    Toast.makeText(getApplicationContext(), "Provider disabled: " + s, Toast.LENGTH_SHORT).show();
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
