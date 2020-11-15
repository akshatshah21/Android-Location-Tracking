package com.example.locationtracker2;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TripActivity extends AppCompatActivity {

  private static final int RC_LOCATION_PERMISSION = 1;
  private static final int RC_END_TRIP = 2;

  FusedLocationTracker fusedLocationTracker;
  Button locationBtn;
  boolean tracking = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    locationBtn = (Button) findViewById(R.id.btn_location);
    locationBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(!tracking) {
          tracking = true;
          locationBtn.setText("Stop trip");
          setupLocationTracking();
        } else {
          AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TripActivity.this);
          alertBuilder.setTitle("End trip?");
          alertBuilder.setMessage("This will also stop tracking your location. Please end trip only when you have reached the destination.");
          alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              Intent intent = new Intent(TripActivity.this, CodeActivity.class);
              intent.putExtra("isBegin", false);
              startActivityForResult(intent, RC_END_TRIP);
            }
          });
          alertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              dialogInterface.cancel();
            }
          });
          alertBuilder.create();
          alertBuilder.show();
        }
      }
    });
  }

  private void setupLocationTracking() {
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      requestLocationPermission();
    } else {
      // locationTracker = new LocationTrackerService(this);
      fusedLocationTracker = new FusedLocationTracker(this);

    }
  }

  private void requestLocationPermission() {
    if(ActivityCompat.shouldShowRequestPermissionRationale(this,
      Manifest.permission.ACCESS_FINE_LOCATION)) {
      new AlertDialog.Builder(this)
        .setTitle("Permission for location")
        .setMessage("Please allow location access.")
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface,
                              int which) {
            ActivityCompat.requestPermissions(TripActivity.this,
              new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, RC_LOCATION_PERMISSION);
          }
        });
    } else {
      ActivityCompat.requestPermissions(this,
        new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION}, RC_LOCATION_PERMISSION);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if(requestCode == RC_LOCATION_PERMISSION) {
      if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i("myTag", "Location permission granted");
        Toast.makeText(this, "Thank you. Your location is being " +
            "tracked",
          Toast.LENGTH_SHORT).show();
        fusedLocationTracker = new FusedLocationTracker(this);
      } else {
        Log.i("myTag", "Location permission denied");
        Toast.makeText(this, "Location permission not given!",
          Toast.LENGTH_SHORT).show();
        locationBtn.setText("Cannot track location, please give " +
          "permissions from settings");
        locationBtn.setEnabled(false);
      }
    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == RC_END_TRIP) {
      if(data.getBooleanExtra("end", false)) {
        tracking = false;
        stopLocationTracking();
        finish();
      }
    }
  }


  private void stopLocationTracking() {
    fusedLocationTracker.stopUsingLocationService();
  }
}
