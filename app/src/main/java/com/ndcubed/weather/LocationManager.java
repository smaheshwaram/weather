package com.ndcubed.weather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by Nathan on 6/7/2017.
 */

public class LocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    static final int PLACE_PICKER_REQUEST = 4039;
    static final int LOCATION_PERMISSION_REQUEST = 3094;
    static final int SETTINGS_CHANGE_REQUEST = 9430;

    private Activity activity;
    private Context context;
    private boolean connected = false;

    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private String locationPermission = "android.permission.ACCESS_FINE_LOCATION";

    private LocationManagerListener listener;

    private boolean locationSettingsDenied = false;
    private boolean checkLocationOnConnect = false;

    public LocationManager(Activity activity, String locationPermission) {
        this.activity = activity;
        this.locationRequest = getLocationRequest();
        this.locationPermission = locationPermission;
    }

    public LocationManager(Context context) {
        this.context = context;
        this.locationRequest = getLocationRequest();
    }

    public void setLocationManagerListener(LocationManagerListener listener) {
        this.listener = listener;
    }

    public void connect(Activity activity) {

        if(client == null || this.activity == null) {
            this.activity = activity;
            client = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
            client.connect();
        } else if(!client.isConnected()) {
            client.connect();
        }
    }

    public void connect(Context context) {
        if(client == null || this.context == null) {
            this.context = context;
            client = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
            client.connect();
        } else if(!client.isConnected()) {
            client.connect();
        }
    }

    void getLocationWithPlacePicker() {
        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            activity.startActivityForResult(builder.build(activity), PLACE_PICKER_REQUEST);
        } catch (Exception err) {
            err.printStackTrace();
            Common.submitError(err);
        }
    }

    public void getCurrentLocation() {
        System.out.println("GET CURRENT");

        if(connected) {
            System.out.println("GET CURRENTTTT");
            if(isPermissionGranted()) {
                checkLocationSettings();
            } else {
                if(activity != null) {
                    requestPermission();
                } else if(listener != null) {
                    listener.onSettingsChangeDenied();
                }
            }
        } else {
            checkLocationOnConnect = true;
        }
    }

    public LocationRequest getLocationRequest() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        return locationRequest;
    }

    private void startLocationUpdates() {
        int permissionCheck = ContextCompat.checkSelfPermission(activity == null ? context : activity, locationPermission);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            System.out.println("START" + client + "  " + locationRequest);
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
    }

    private void stopLocationUpdates() {
        if(client != null && client.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }

    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(client, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                System.out.println("RESULT");
                Status status = locationSettingsResult.getStatus();

                switch(status.getStatusCode()) {

                    case LocationSettingsStatusCodes.SUCCESS:
                        System.out.println("YES");
                        locationSettingsDenied = false;
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        if(activity != null) {
                            try {
                                if(!locationSettingsDenied) {
                                    locationSettingsDenied = true;
                                    status.startResolutionForResult(activity, SETTINGS_CHANGE_REQUEST);
                                } else if(listener != null) {
                                    listener.onSettingsChangeDenied();
                                }
                            } catch(Exception err) {
                                err.printStackTrace();
                                Common.submitError(err);
                            }
                        } else if(listener != null) {
                            listener.onSettingsChangeDenied();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        if(listener != null) listener.onSettingsChangeDenied();
                        break;
                }
            }
        });
    }

    private boolean isPermissionGranted() {
        int permissionCheck = ContextCompat.checkSelfPermission(activity == null ? context : activity, locationPermission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{locationPermission}, LOCATION_PERMISSION_REQUEST);
    }

    public boolean hasPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(activity == null ? context : activity, locationPermission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public void disconnect() {
        if(client != null) {
            client.disconnect();
            activity = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connected = true;

        if(checkLocationOnConnect) {
            checkLocationOnConnect = false;
            getCurrentLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        connected = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        connected = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("LOCATION: " + location);
        stopLocationUpdates();
        if(listener != null) listener.onLocationReceived(location);
    }

    interface LocationManagerListener {

        void onLocationReceived(Location location);
        void onSettingsChangeDenied();
    }
}
