package com.example.locationtracker2;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int RC_LOCATION_PERMISSION = 1;

    LocationTrackerService locationTracker;
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
                    locationBtn.setText("Stop tracking location");
                    setupLocationTracking();
                } else {
                    tracking = false;
                    locationBtn.setText("Track location");
                    stopLocationTracking();
                }
            }
        });
    }

    private void setupLocationTracking() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            locationTracker = new LocationTrackerService(this);
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
                            ActivityCompat.requestPermissions(MainActivity.this,
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
                locationTracker = new LocationTrackerService(this);
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

    private void stopLocationTracking() {
        locationTracker.stopUsingLocationService();
        locationTracker = null;
    }
}
