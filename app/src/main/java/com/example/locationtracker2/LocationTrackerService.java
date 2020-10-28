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
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;


public class LocationTrackerService extends Service implements LocationListener {
    private final Context mContext;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    Location location;
    double latitude, longitude;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 6;

    protected LocationManager locationManager;

    private static AsyncHttpClient client;
    private static final String url = "http://192.168.29.2:5000/"; // my local
    // server

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
                // showSettingsAlert(); // check
            } else {
                this.canGetLocation = true;
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
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                this
                        );
                    }
                } catch (SecurityException secExp) {
                    secExp.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Location getLocation() {
        return location;
    }

    public void stopUsingLocationService() {
        if (locationManager != null) {
            locationManager.removeUpdates(LocationTrackerService.this);
        }
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
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

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.i("myTag",
                  "Latitude:" + location.getLatitude() +
                  " Longitude:" + location.getLongitude());
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            pushLocationUpdate(location);
        }
    }

    public void pushLocationUpdate(Location location) {
        // Push lat, long to the backend. Probably a POST request
        if(location != null) {

            RequestParams params = new RequestParams();
            params.put("latitude", location.getLatitude());
            params.put("longitude", location.getLongitude());
            client.post(url, params, new AsyncHttpResponseHandler() {
                @Override
                public void onStart() {
                    // called before requesting
                    Log.i("myTag", "Before sending POST req");
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    // called when response HTTP status is "200 OK"
                    Log.i("myTag", "POST req successful");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] response, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    Log.i("myTag", "POST req failed. Error code: " + statusCode);
                    Log.i("myTag", "Exception:" + e.getMessage());
                }

                @Override
                public void onRetry(int retryNo) {
                    // called when request is retried
                }
            });
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
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
