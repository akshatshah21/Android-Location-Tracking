package com.example.locationtracker2;

import android.annotation.SuppressLint;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class FusedLocationTracker extends Service {

  private FusedLocationProviderClient fusedLocationClient;
  private LocationRequest locationRequest;
  private LocationCallback locationCallback;
  private Context context;
  private Location mLocation;

  private static final String url = "http://192.168.29.2:5000";
  private Socket socket;

  @SuppressLint("MissingPermission")
  public FusedLocationTracker(Context context) {
    this.context = context;
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

    fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
      @Override
      public void onComplete(@NonNull Task<Location> task) {
        if(task.isSuccessful()) {
          Log.d("myTag", "getLastLocation() task successful");
          if(task.getResult() != null) {
            Location location = task.getResult();
            if(location != null) {
              Log.d("myTag", "Location success");
              Log.d("myTag", location.getLatitude() + " " + location.getLongitude());
              Toast.makeText(context, location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
              mLocation = location;
            } else {
              Log.d("myTag", "location null");
            }
          } else {
            Log.d("myTag", "getLastLocation() task not successful");
         }
        }
      }
    });

    locationRequest = new LocationRequest();
    locationRequest.setInterval(2000);
    locationRequest.setFastestInterval(1000);
    locationRequest.setMaxWaitTime(5000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    locationCallback = new LocationCallback() {
      @Override
      public void onLocationAvailability(LocationAvailability locationAvailability) {
        super.onLocationAvailability(locationAvailability);
        if(locationAvailability.isLocationAvailable()) {
          Log.d("myTag", "Location available");
          if(socket == null) {
            initSocketConn();
          }
        }
      }

      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        if(locationResult.getLastLocation() != null) {
          mLocation = locationResult.getLastLocation();
          JSONObject locationJson = new JSONObject();
          try {
            locationJson.put("latitude", mLocation.getLatitude());
            locationJson.put("longitude", mLocation.getLongitude());
            // can add more info, like bearing
            socket.emit("location-update", locationJson);
          } catch (JSONException e) {
            e.printStackTrace();
          }
          Log.d("myTag", mLocation.getLatitude() + " " + mLocation.getLongitude());
          Toast.makeText(context, mLocation.getLatitude() + " " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
        }
      }
    };

    try {
      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    } catch (SecurityException securityException) {
      Log.e("myTag", "Lost location permissions. Couldn't remove updates");
      //Create a function to request necessary permissions from the app.

      // checkAndStartLocationUpdates();
      // Handled by TripActivity?
    }
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

  public void stopUsingLocationService() {
    socket.disconnect();
    fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(new OnCompleteListener<Void>() {
      @Override
      public void onComplete(@NonNull Task<Void> task) {
        if(task.isSuccessful()) {
          Log.d("myTag", "Location callback removed");
        } else {
          Log.d("myTag", "Failed to remove location callback");
        }
      }
    });
  }


  private void checkAndStartLocationUpdates() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }
}