package com.ndcubed.weather;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.ndcubed.nappsupport.views.RoundedSwitch;
import com.ndcubed.nappsupport.views.SimpleColorButton;
import com.ndcubed.nappsupport.views.WhiteDialog;

/**
 * Created by Nathan on 5/30/2017.
 */

public class WeatherWidgetConfigure extends Activity implements LocationManager.LocationManagerListener {

    boolean noConfigure = false;
    int appWidgetID;
    String provider = "";

    LocationManager locationManager;

    boolean forceRefresh = false;
    boolean isUserConfigure = false;
    boolean isFahrenheit, alwaysDayIcons, alwaysDayWeekIcons, autoRefresh, useCurrentLocation, weatherNotifications;
    boolean updateNotifications = true;

    SharedPreferences prefs, appWidePreferences;

    SeekBar transparencySeekBar, textColorSeekBar;
    View widgetTransparencyPreview, textColorTransparencyPreview;
    int widgetBackgroundTransparency = 255;
    int[] textViewIDs;
    int textColor = Color.argb(242, 84, 93, 112);
    final int textColorLight = Color.argb(242, 255, 255, 255);
    final int textDarkColor = Color.argb(242, 84, 93, 112);
    int textColorProgress = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.widget_configure_settings_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            //window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Common.getAttributeColor(this, R.attr.colorPrimaryDark));
        }

        transparencySeekBar = (SeekBar)findViewById(R.id.transparencySeekBar);
        textColorSeekBar = (SeekBar)findViewById(R.id.textColorSeekBar);
        widgetTransparencyPreview = findViewById(R.id.widgetTransparencyPreview);
        textColorTransparencyPreview =  findViewById(R.id.textColorTransparencyPreview);

        textViewIDs = new int[]{R.id.tempPrevLabel1, R.id.timePrevLabel1, R.id.tempPrevLabel2, R.id.timePrevLabel2};

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if(extras != null) {
            isUserConfigure = extras.getBoolean("isUserConfigure", false);

            appWidgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetID);

            System.out.println("WIDGET ID SET:" + appWidgetID);

            prefs = getSharedPreferences("WeatherPreferences" + appWidgetID, MODE_PRIVATE);
            appWidePreferences = getSharedPreferences("WeatherPreferences", MODE_PRIVATE);
            isFahrenheit = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, true);
            alwaysDayIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, false);
            alwaysDayWeekIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, false);
            autoRefresh = prefs.getBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, true);
            useCurrentLocation = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, false);
            weatherNotifications = prefs.getBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, false);
            widgetBackgroundTransparency = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TRANSPARENCY, 255);
            textColorProgress = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR_PROGRESS, 0);
            textColor = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR, Color.argb(242, 84, 93, 112));
            updateNotifications = appWidePreferences.getBoolean(Common.AppPreferenceKeys.KEY_RECEIVE_UPDATE_NOTIFICATIONS, true);

            System.out.println("APP WIDGET ID CONFIG" + appWidgetID);

            if(info.provider != null) {
                provider = info.provider.getClassName();
            }

            if(!isUserConfigure) {
                WhiteDialog welcomeDialog = new WhiteDialog(this);
                welcomeDialog.setMessageText("Hi! Lets get started. Let's get your weather for your current location or enter pick a desired location.");
                welcomeDialog.setIcon(R.drawable.dialog_icon);
                welcomeDialog.setIconVisible(true);
                welcomeDialog.setPositiveButtonText("Pick Location");
                welcomeDialog.setDismissButtonText("Current Location");
                welcomeDialog.addDialogListener(new WhiteDialog.DialogListener() {
                    @Override
                    public void dismissButtonClicked() {
                        if(Common.IS_DEV_MODE) {
                            finishConfigure(null);
                        } else {
                            locationManager.getCurrentLocation();
                        }
                    }

                    @Override
                    public void positiveButtonClicked() {
                        noConfigure = true;
                        locationManager.getLocationWithPlacePicker();
                    }
                });
                welcomeDialog.show();
            }
        }

        System.out.println("STANDARD COLOR: " + textColor);

        transparencySeekBar.setProgress(widgetBackgroundTransparency);
        textColorSeekBar.setProgress(textColorProgress);
        widgetTransparencyPreview.setAlpha(1f * ((float)widgetBackgroundTransparency / 255f));
        textColorTransparencyPreview.setAlpha(1f * ((float)widgetBackgroundTransparency / 255f));
        ((TextView)findViewById(R.id.locationLabel)).setText(useCurrentLocation ? "Using current location." : prefs.getString("pickedAddress", "Using static location."));
        ((RoundedSwitch)findViewById(R.id.fahrenheitToggle)).setSwitchStateWithoutAnimation(isFahrenheit ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.alwaysDayIconToggle)).setSwitchStateWithoutAnimation(alwaysDayIcons ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.weekDayIconToggle)).setSwitchStateWithoutAnimation(alwaysDayWeekIcons ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.refreshToggle)).setSwitchStateWithoutAnimation(autoRefresh ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.useLocationToggle)).setSwitchStateWithoutAnimation(useCurrentLocation ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.weatherNotificationToggle)).setSwitchStateWithoutAnimation(weatherNotifications ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);
        ((RoundedSwitch)findViewById(R.id.updateNotificationsSwitch)).setSwitchStateWithoutAnimation(updateNotifications ? RoundedSwitch.STATE_ON : RoundedSwitch.STATE_OFF);

        for(int id : textViewIDs) {
            ((TextView)findViewById(id)).setTextColor(Color.argb(Color.alpha(textColor), Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
        }

        transparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                widgetBackgroundTransparency = i;
                widgetTransparencyPreview.setAlpha(1f * ((float)i / 255f));
                textColorTransparencyPreview.setAlpha(1f * ((float)i / 255f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor e = prefs.edit();
                e.putInt(Common.PreferenceKeys.KEY_WIDGET_TRANSPARENCY, widgetBackgroundTransparency);
                e.apply();
            }
        });

        textColorSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ArgbEvaluator evaluator = new ArgbEvaluator();
                textColor = (Integer)evaluator.evaluate(1f * ((float)i / 255f), textDarkColor, textColorLight);
                textColorProgress = i;

                for(int id : textViewIDs) {
                    ((TextView)findViewById(id)).setTextColor(Color.argb(Color.alpha(textColor), Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
                }

                System.out.println("COLLLLOR" + Color.alpha(textColor) + "  " + Color.red(textColor) + "  " + Color.green(textColor) + "  " + Color.blue(textColor));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor e = prefs.edit();
                e.putInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR_PROGRESS, textColorProgress);
                e.putInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR, textColor);
                e.apply();
            }
        });

        ((RoundedSwitch)findViewById(R.id.updateNotificationsSwitch)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                SharedPreferences.Editor e = appWidePreferences.edit();
                e.putBoolean(Common.AppPreferenceKeys.KEY_RECEIVE_UPDATE_NOTIFICATIONS, switchState == RoundedSwitch.STATE_ON);
                e.apply();
            }
        });

        ((RoundedSwitch)findViewById(R.id.useLocationToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                forceRefresh = true;
                noConfigure = false;
                useCurrentLocation = true;
                SharedPreferences.Editor e = prefs.edit();

                if(locationManager.hasPermission() || switchState == RoundedSwitch.STATE_OFF) {
                    e.putBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, switchState == RoundedSwitch.STATE_ON);
                } else if(!locationManager.hasPermission()) {
                    noConfigure = true;
                }

                if(switchState == RoundedSwitch.STATE_ON) {
                    ((TextView)findViewById(R.id.locationLabel)).setText("Using current location.");
                    locationManager.getCurrentLocation();
                } else {
                    ((TextView)findViewById(R.id.locationLabel)).setText(prefs.getString("pickedAddress", "Pick a Location..."));
                    double lat = Double.parseDouble(prefs.getString("pickedLat", "-1"));
                    double lon = Double.parseDouble(prefs.getString("pickedLon", "-1"));

                    if(lat != -1 && lon != -1) {
                        e.putString("lat", Double.toString(lat));
                        e.putString("lon", Double.toString(lon));
                    }
                }

                e.commit();
            }
        });
        ((RoundedSwitch)findViewById(R.id.fahrenheitToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {

                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, switchState == RoundedSwitch.STATE_ON);
                e.commit();
            }
        });
        ((RoundedSwitch)findViewById(R.id.weekDayIconToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, switchState == RoundedSwitch.STATE_ON);
                e.apply();
            }
        });
        ((RoundedSwitch)findViewById(R.id.refreshToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, switchState == RoundedSwitch.STATE_ON);
                e.apply();
            }
        });
        ((RoundedSwitch)findViewById(R.id.weatherNotificationToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                forceRefresh = true;
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, switchState == RoundedSwitch.STATE_ON);
                e.apply();
            }
        });
        ((RoundedSwitch)findViewById(R.id.alwaysDayIconToggle)).setRoundedSwitchListener(new RoundedSwitch.RoundedSwitchListener() {
            @Override
            public void onPress() {

            }

            @Override
            public void onRelease() {

            }

            @Override
            public void onStateChange(int switchState) {
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, switchState == RoundedSwitch.STATE_ON);
                e.apply();
            }
        });


        locationManager = new LocationManager(this, Manifest.permission.ACCESS_FINE_LOCATION);
        locationManager.setLocationManagerListener(this);
        findViewById(R.id.pickLocationButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                noConfigure = true;
                locationManager.getLocationWithPlacePicker();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode) {

            case LocationManager.LOCATION_PERMISSION_REQUEST:
                if(locationManager.hasPermission()) {

                    SharedPreferences prefs = getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES + appWidgetID, Context.MODE_PRIVATE);
                    SharedPreferences.Editor e = prefs.edit();
                    e.putBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, true);
                    e.apply();

                    locationManager.getCurrentLocation();
                } else {
                    //explain can't function without location permission.
                    if(!isUserConfigure) {
                        locationManager.getLocationWithPlacePicker();
                    } else {
                        ((RoundedSwitch)findViewById(R.id.useLocationToggle)).setSwitchState(RoundedSwitch.STATE_OFF);
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode) {

            case LocationManager.SETTINGS_CHANGE_REQUEST:
                locationManager.getCurrentLocation();
                break;
            case LocationManager.PLACE_PICKER_REQUEST:
                if(resultCode == RESULT_OK) {
                    forceRefresh = true;
                    noConfigure = false;

                    Place place = PlacePicker.getPlace(WeatherWidgetConfigure.this, data);
                    LatLng location = place.getLatLng();
                    System.out.println("LAT LON : " + location);

                    useCurrentLocation = false;
                    ((RoundedSwitch)findViewById(R.id.useLocationToggle)).setSwitchStateWithoutAnimation(RoundedSwitch.STATE_OFF);

                    SharedPreferences prefs = getSharedPreferences("WeatherPreferences" + appWidgetID, MODE_PRIVATE);
                    SharedPreferences.Editor e = prefs.edit();
                    e.putBoolean("didFirstUpdate", false);
                    e.putString("lat", Common.IS_DEV_MODE ? "37.323" : Double.toString(location.latitude));
                    e.putString("lon", Common.IS_DEV_MODE ? "-122.053" : Double.toString(location.longitude));
                    e.putString("pickedLat", Double.toString(location.latitude));
                    e.putString("pickedLon", Double.toString(location.longitude));
                    e.putString("pickedAddress", place.getAddress().toString());
                    e.putBoolean(Common.PreferenceKeys.KEY_WAS_CONFIGURED, true);
                    e.putBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, false);
                    e.commit();

                    ((TextView)findViewById(R.id.locationLabel)).setText(place.getAddress());

                    if(!isUserConfigure) finishConfigure(null);
                }
                break;
        }
    }

    public void finishConfigure(Location location) {
        noConfigure = true;

        if(location != null) {
            SharedPreferences prefs = getSharedPreferences("WeatherPreferences" + appWidgetID, MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();

            e.putBoolean("didFirstUpdate", false);
            e.putInt("tutorialIndex", 0);
            e.putBoolean(Common.PreferenceKeys.KEY_WAS_CONFIGURED, true);
            e.putString("lat", Common.IS_DEV_MODE ? "37.323" : Double.toString(location.getLatitude()));
            e.putString("lon", Common.IS_DEV_MODE ? "-122.053" : Double.toString(location.getLongitude()));
            e.putString("pickedLat", Common.IS_DEV_MODE ? "-122.053" : Double.toString(location.getLongitude()));
            e.putString("pickedLon", Common.IS_DEV_MODE ? "-122.053" : Double.toString(location.getLongitude()));
            e.commit();
        }

        if(!provider.contains("SmallWeatherWidgetProvider")) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, WeatherWidgetProvider.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetID});
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
            intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_WIDGET_CONFIGURED_FORCE_REFRESH);
            sendBroadcast(intent);
        } else {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, SmallWeatherWidgetProvider.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetID});
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
            intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_WIDGET_CONFIGURED);
            sendBroadcast(intent);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
        setResult(RESULT_OK, resultValue);

        locationManager.disconnect();

        finish();
    }

    public void finishUserConfigure(Location location) {
        noConfigure = true;

        if(location != null) {
            SharedPreferences prefs = getSharedPreferences("WeatherPreferences" + appWidgetID, MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();

            e.putString("lat", Common.IS_DEV_MODE ? "37.323" : Double.toString(location.getLatitude()));
            e.putString("lon", Common.IS_DEV_MODE ? "-122.053" : Double.toString(location.getLongitude()));
            e.commit();
        }

        if(!provider.contains("SmallWeatherWidgetProvider")) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, WeatherWidgetProvider.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetID});
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
            intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, forceRefresh ? Common.WidgetActions.ACTION_WIDGET_CONFIGURED_FORCE_REFRESH : Common.WidgetActions.ACTION_WIDGET_CONFIGURED);
            sendBroadcast(intent);
        } else {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, SmallWeatherWidgetProvider.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetID});
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetID);
            intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, forceRefresh ? Common.WidgetActions.ACTION_WIDGET_CONFIGURED_FORCE_REFRESH : Common.WidgetActions.ACTION_WIDGET_CONFIGURED);
            sendBroadcast(intent);
        }

        forceRefresh = false;
    }

    @Override
    public void onLocationReceived(Location location) {
        forceRefresh = true;
        System.out.println("GET LOCATION");

        if(!isUserConfigure) {
            finishConfigure(location);
        }
    }

    @Override
    public void onSettingsChangeDenied() {
        ((RoundedSwitch)findViewById(R.id.useLocationToggle)).setSwitchState(RoundedSwitch.STATE_OFF);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager.connect(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!noConfigure) finishUserConfigure(null);
        noConfigure = false;
        locationManager.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
