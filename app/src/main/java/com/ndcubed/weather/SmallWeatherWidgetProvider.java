package com.ndcubed.weather;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Nathan on 5/10/2017.
 */

public class SmallWeatherWidgetProvider extends AppWidgetProvider implements LocationManager.LocationManagerListener {

    double lat, lon;

    final float MIN_WIDGET_HEIGHT = 100f;
    final float MAX_WIDGET_HEIGHT = 160f;
    final float MAX_WIDGET_WIDTH = 340f;
    final float MIN_WIDGET_WIDTH = 270f;
    final float CURRENT_CONDITIONS_CONTAINER_MIN_WIDTH = 300f;

    final int NOTIFICATION_ID_PREFIX = 30492;
    final int HOURLY_PRESSED = 1;
    final int CONDITION_PRESSED = 2;
    final int CONFIG_PENDING_INTENT = 3;
    final int EVENT_REFRESH_PRESSED = 4;
    int event = -1;
    int displayIndex = 0;
    int tutorialIndex = 0;
    int thisWidgetID = -1;

    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;

    boolean showHighLow = false;

    boolean alwaysDayIcons, alwaysDayWeekIcons, isFahrenheit, autoRefresh, weatherNotifications, useCurrentLocation;

    LocationManager locationManager;


    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + appWidgetId, Context.MODE_PRIVATE);
        displayIndex = prefs.getInt("displayIndex", 0);
        isFahrenheit = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, true);
        alwaysDayIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, false);
        alwaysDayWeekIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, false);
        autoRefresh = prefs.getBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, true);
        weatherNotifications = prefs.getBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, true);
        useCurrentLocation = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, true);

        updateWeather(context, getWeatherFromCache(context), new int[]{appWidgetId}, appWidgetManager, false);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public boolean isWidgetExpanded(Context context, AppWidgetManager appWidgetManager, int appWidgetID) {
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        ComponentName largeWeatherWidget = new ComponentName(context, SmallWeatherWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(largeWeatherWidget);

        if(intent.getExtras() != null) {
            thisWidgetID = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            System.out.println("ID OF WIDGET: " + thisWidgetID);
        }

        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + thisWidgetID, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        event = intent.getIntExtra("action", -1);

        System.out.println("EVENT: " + event);

        if(appWidgetIds.length > 0 && thisWidgetID != AppWidgetManager.INVALID_APPWIDGET_ID) {
            /** ADD LISTENERS **/
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_small_layout);

            //TUTORIAL NEXT BUTTON INTENT
            Intent tutorialIntent = new Intent(context, SmallWeatherWidgetProvider.class);
            tutorialIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            tutorialIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{thisWidgetID});
            tutorialIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, thisWidgetID);
            tutorialIntent.putExtra("action", Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED);
            PendingIntent tutorialPendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), tutorialIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.tutorialNextButton, tutorialPendingIntent);

            //SETTINGS INTENT
            Intent settingsIntent = new Intent(context, WeatherWidgetConfigure.class);
            settingsIntent.setData(Uri.parse(settingsIntent.toUri(Intent.URI_INTENT_SCHEME)));
            settingsIntent.putExtra("isUserConfigure", true);
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{thisWidgetID});
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, thisWidgetID);
            PendingIntent settingsPendingIntent = PendingIntent.getActivity(context, getUniqueIntentID(), settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.settingsButton, settingsPendingIntent);

            //HOURLY TEMPERATURE TOGGLE INTENT
            Intent hourlyIntent = new Intent(context, SmallWeatherWidgetProvider.class);
            hourlyIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            hourlyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{thisWidgetID});
            hourlyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, thisWidgetID);
            hourlyIntent.putExtra("action", HOURLY_PRESSED);
            PendingIntent hourlyPendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), hourlyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.detailsContentView, hourlyPendingIntent);
            views.setOnClickPendingIntent(R.id.contentView, hourlyPendingIntent);

            //REFRESH INTENT
            Intent refreshIntent = new Intent(context, SmallWeatherWidgetProvider.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra("action", EVENT_REFRESH_PRESSED);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{thisWidgetID});
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, thisWidgetID);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.refreshButton, pendingIntent);

            if(!prefs.getBoolean("didTutorial", false)) {
                views.setViewVisibility(R.id.tutorialView, View.VISIBLE);
            }

            if(event == HOURLY_PRESSED) {
                displayIndex = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_DISPLAY_INDEX, 0);
                displayIndex += 1;
                if(displayIndex > 4) displayIndex = 0;

                e.putInt(Common.PreferenceKeys.KEY_WIDGET_DISPLAY_INDEX, displayIndex);
            } else if(event == CONDITION_PRESSED) {
                showHighLow = prefs.getBoolean("showHighLow", false);
                showHighLow = !showHighLow;
                e.putBoolean("showHighLow", showHighLow);
            } else if(event == Common.WidgetActions.ACTION_STOP_REFRESH) {
                System.out.println("STOP REFRESH");

                views.setViewVisibility(R.id.rootContentView, View.VISIBLE);
                views.setViewVisibility(R.id.settingsButton, View.VISIBLE);
                views.setViewVisibility(R.id.refreshButton, View.VISIBLE);
                views.setViewVisibility(R.id.refreshProgressView, View.GONE);
            } else if(event == Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED) {
                tutorialIndex = prefs.getInt("tutorialIndex", 0);
                System.out.println("TUTORIAL INDEX: " + tutorialIndex);

                switch(tutorialIndex) {

                    case 0:
                        views.setViewVisibility(R.id.tutorialView, View.GONE);
                        e.putBoolean("didTutorial", true);
                        break;
                }

                tutorialIndex += 1;
                e.putInt("tutorialIndex", tutorialIndex);
            }

            e.putInt("action", event);
            e.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, thisWidgetID);
            e.commit();

            (AppWidgetManager.getInstance(context)).updateAppWidget(thisWidgetID, views);
        }

        if(intent.getAction().equals(Common.CANCEL_NOTIFICATION_ACTION)) {
            NotificationManager mNotifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(intent.getIntExtra("notificationID", 0));
        } else {
            System.out.println("UPDATE NOW");
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        System.out.println("APP WIDGET ID:");

        for(int id : appWidgetIds) {
            System.out.println("iddd: " + id);
        }
        thisWidgetID = appWidgetIds[0];

        this.context = context;
        this.appWidgetIds = appWidgetIds;
        this.appWidgetManager = appWidgetManager;

        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + thisWidgetID, Context.MODE_PRIVATE);
        event = prefs.getInt("action", -1);
        displayIndex = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_DISPLAY_INDEX, 0);
        showHighLow = prefs.getBoolean("showHighLow", false);
        lat = Double.parseDouble(prefs.getString(Common.PreferenceKeys.KEY_LAT, "0"));
        lon = Double.parseDouble(prefs.getString(Common.PreferenceKeys.KEY_LON, "0"));

        isFahrenheit = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, true);
        alwaysDayIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, false);
        alwaysDayWeekIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, false);
        autoRefresh = prefs.getBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, true);
        weatherNotifications = prefs.getBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, true);
        useCurrentLocation = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, true);

        long lastUpdate = prefs.getLong("lastUpdate", System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        boolean didFirstUpdate = prefs.getBoolean("didFirstUpdate", false);

        if(locationManager == null && useCurrentLocation) {
            locationManager = new LocationManager(context);
            locationManager.connect(context);
            locationManager.setLocationManagerListener(this);
        }

        if(!weatherNotifications) {
            System.out.println("FALSE WEATHER");
            if(!prefs.getString("notifications", "").equals("")) {

                try {
                    //CANCEL PREVIOUS NOTIFICATIONS
                    NotificationManager mNotifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
                    JSONArray notifications = new JSONArray(prefs.getString("notifications", ""));

                    System.out.println("NNN: " + notifications);

                    for(int i = 0; i < notifications.length(); i++) {

                        JSONObject notification = notifications.getJSONObject(i);
                        mNotifyMgr.cancel(Integer.parseInt(notification.get("id").toString()));
                        System.out.println("NOTIFICATION ID " + notification.get("id"));
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }

        System.out.println("SWITCH EVENT: " + event);
        switch(event) {

            case EVENT_REFRESH_PRESSED:
            case Common.WidgetActions.ACTION_WIDGET_CONFIGURED:
                setRefreshing(true, appWidgetIds);

                //ONLY UPDATE IF LAST UPDATE IS GREATER THAN 5 MIN AGO.
                Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();
                if(currentTime - lastUpdate > 300000 || !didFirstUpdate) {
                    if(!useCurrentLocation) {
                        new UpdateTask().execute();
                    } else {
                        locationManager.getCurrentLocation();
                    }
                } else {
                    updateWeather(context, getWeatherFromCache(context), appWidgetIds, appWidgetManager, true);
                }
                break;
            case HOURLY_PRESSED:
            case CONDITION_PRESSED:
                updateWeather(context, getWeatherFromCache(context), appWidgetIds, appWidgetManager, false);
                break;
            case Common.WidgetActions.ACTION_FORCE_REFRESH:
                setRefreshing(true, appWidgetIds);

                //FORCE REFRESH
                Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();
                if(!useCurrentLocation) {
                    new UpdateTask().execute();
                } else {
                    locationManager.getCurrentLocation();
                }
                break;
            case Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED:
            case Common.WidgetActions.ACTION_STOP_REFRESH:
                break;
            default:
                if(autoRefresh) {
                    setRefreshing(true, appWidgetIds);

                    //ONLY UPDATE IF LAST UPDATE IS GREATER THAN 5 MIN AGO.
                    Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();
                    if(currentTime - lastUpdate > 300000 || !didFirstUpdate) {
                        if(!useCurrentLocation) {
                            new UpdateTask().execute();
                        } else {
                            locationManager.getCurrentLocation();
                        }
                    } else {
                        updateWeather(context, getWeatherFromCache(context), appWidgetIds, appWidgetManager, true);
                    }
                }
                break;
        }
    }

    public int getUniqueIntentID() {
        return (int)(10000 * Math.random());
    }

    private class UpdateTask extends AsyncTask<String, Void, Forecast> {

        @Override
        protected Forecast doInBackground(String... strings) {
            try {
                return WeatherUtils.getCurrentForecast(lat, lon);
            } catch(Exception err) {
                err.printStackTrace();
                Common.submitError(err);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Forecast forecast) {

            forecast = forecast == null ? getWeatherFromCache(context) : forecast;

            try {
                WeatherObject weatherObject = forecast.getCurrentConditions();

                SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + thisWidgetID, Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();

                e.putLong("date", weatherObject.getDate());
                e.putString("weatherIconString", weatherObject.getWeatherIconString());
                e.putInt("temperature", weatherObject.getTemperature());
                e.putInt("high", weatherObject.getHigh(isFahrenheit));
                e.putInt("low", weatherObject.getLow(isFahrenheit));
                e.putLong("lastUpdate", weatherObject.getCurrentTime());
                e.putBoolean("didFirstUpdate", true);
                e.putLong("sunset",weatherObject.getSunset());
                e.putLong("sunrise", weatherObject.getSunrise());
                e.putInt("weatherID", weatherObject.getWeatherID());
                e.putString("condition", weatherObject.getCondition());
                e.putString("rawHourlyJSON", forecast.getRawHourlyJSON());
                e.putString("rawDailyJSON", forecast.getRawDailyJSON());
                e.putString("alerts", weatherObject.getAlerts());
                e.putFloat("precipProbability", weatherObject.getPrecipProbabilityFloat());
                e.putFloat("cloudCover", weatherObject.getCloudCoverFloat());
                e.putInt("windSpeed", weatherObject.getWindSpeed());
                e.putInt("uvIndex", weatherObject.getUv());
                e.putInt("humidity", weatherObject.getHumidity());
                e.putInt("pressure", weatherObject.getPressure());

                if(!prefs.getString("notifications", "").equals("")) {

                    //CANCEL PREVIOUS NOTIFICATIONS
                    NotificationManager mNotifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
                    JSONArray notifications = new JSONArray(prefs.getString("notifications", ""));

                    System.out.println("NNN: " + notifications);

                    for(int i = 0; i < notifications.length(); i++) {

                        JSONObject notification = notifications.getJSONObject(i);
                        mNotifyMgr.cancel(Integer.parseInt(notification.get("id").toString()));
                    }
                }

                if(weatherNotifications) {
                    if(!weatherObject.getAlerts().equals("")) {

                        JSONArray alerts = new JSONArray(weatherObject.getAlerts());
                        if(alerts.length() > 0) {
                            String notificationString = "[";

                            for(int i = 0; i < alerts.length(); i++) {

                                JSONObject alert = alerts.getJSONObject(i);
                                String alertTitle = alert.get("title").toString();

                                if(alertTitle.toLowerCase().contains("watch") || alertTitle.toLowerCase().contains("warning")) {
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(context)
                                                    .setSmallIcon(R.drawable.notification_icon)
                                                    .setContentTitle(alertTitle)
                                                    .setContentText("Until " + WeatherUtils.millisecondsToDateString(WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString()))));


                                    Notification notification = mBuilder.build();
                                   // notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

                                    int notificationID = (int)(Integer.MAX_VALUE * Math.random());

                                    // set up alarm to cancel notification
                                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                    Intent intent = new Intent(context, SmallWeatherWidgetProvider.class);
                                    intent.setAction(Common.CANCEL_NOTIFICATION_ACTION);
                                    intent.putExtra("notificationID", notificationID);
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    alarmManager.set(AlarmManager.RTC, WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString())), pendingIntent);

                                    NotificationManager mNotifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
                                    mNotifyMgr.notify(notificationID, notification);

                                    notificationString += (i > 0 ? "," : "") + "{id=" + notificationID + ", expires=" + Long.parseLong(alert.get("expires").toString()) + "}";
                                }

                            }

                            notificationString += "]";
                            e.putString("notifications", notificationString);

                            System.out.println("NOTIFICATIONS: " + notificationString);
                        } else {
                            e.putString("notifications", "");
                        }
                    } else {
                        e.putString("notifications", "");
                    }
                }
                e.commit();
                updateWeather(context, forecast, appWidgetIds, appWidgetManager, true);
            } catch(Exception err) {
                updateWeather(context, getWeatherFromCache(context), appWidgetIds, appWidgetManager, true);
                err.printStackTrace();
            }
        }
    }

    public Forecast getWeatherFromCache(Context context) {

        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + thisWidgetID, Context.MODE_PRIVATE);

        System.out.println("LAT" + lat + "  " + lon + "  CACHE");

        long currentTime = System.currentTimeMillis();
        int temperature = prefs.getInt("temperature", 72);
        int high = prefs.getInt("high", 72);
        int low = prefs.getInt("low", 72);
        int humidity = prefs.getInt("humidity", 0);
        int windSpeed = prefs.getInt("windSpeed", 0);
        int uvIndex = prefs.getInt("uvIndex", 0);
        int pressure = prefs.getInt("pressure", 0);
        long sunset = prefs.getLong("sunset", 0);
        long sunrise = prefs.getLong("sunrise", 0);
        long date = prefs.getLong("date", currentTime);
        float cloudCover = prefs.getFloat("cloudCover", 0);
        float precipProbability = prefs.getFloat("precipProbability", 0);
        String condition = prefs.getString("condition", "Clear");
        String rawHourlyJSON = prefs.getString("rawHourlyJSON", "");
        String rawDailyJSON = prefs.getString("rawDailyJSON", "");
        String weatherIconString = prefs.getString("weatherIconString", "clear-day");

        System.out.println("JSON: " + rawHourlyJSON);

        WeatherObject currentConditions = new WeatherObject(date, temperature, sunrise, sunset, weatherIconString, condition);
        currentConditions.setHigh(high);
        currentConditions.setLow(low);
        currentConditions.setPrecipProbability(precipProbability);
        currentConditions.setCloudCover(cloudCover);
        currentConditions.setWindSpeed(windSpeed);
        currentConditions.setUv(uvIndex);
        currentConditions.setHumidity(humidity);
        currentConditions.setPressure(pressure);

        Forecast forecast = new Forecast(currentConditions);
        forecast.setRawHourlyJSON(rawHourlyJSON);
        forecast.setRawDailyJSON(rawDailyJSON);

        return forecast;
    }

    public float getSizeForWidgetHeight(float widgetHeight, float minWidgetHeight, float defaultSize, float minSize) {

        float scale = widgetHeight / minWidgetHeight;
        scale = Math.min(1f, scale);

        return Math.max(minSize, (defaultSize * scale));
    }

    public float getSizeForWidgetSizeRange(float widgetSize, float minWidgeSize, float maxWidgetSize, float defaultViewSize, float minViewSize) {

        /** minWidgetHeight = 50 maxWidgetHeight = 100 widgetHeight = 99 **/

        float max = maxWidgetSize - minWidgeSize;
        float difference = max - Math.min(max, maxWidgetSize - widgetSize);

        float scale = difference / max;
        scale = Math.min(1f, scale);

        return Math.max(minViewSize, (defaultViewSize * scale));
    }

    public void updateWeather(Context context, Forecast forecast, int[] appWidgetIds, AppWidgetManager appWidgetManager, boolean refresh) {

        WeatherObject currently = forecast.getCurrentConditions();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_small_layout);
        int[] timeViews = new int[]{R.id.time1, R.id.time2, R.id.time3, R.id.time4, R.id.time5};
        int[] labelViews = new int[]{R.id.label1, R.id.label2, R.id.label3, R.id.label4, R.id.label5};
        int[] hourlyImages = new int[]{R.id.weather1, R.id.weather2, R.id.weather3, R.id.weather4, R.id.weather5};
        int[] currentStatImages = new int[]{R.id.windIcon, R.id.cloudCoverIcon, R.id.pressureIcon, R.id.humidityIcon, R.id.uvIcon};
        int[] currentStatLabels = new int[]{R.id.windTitle, R.id.humidityTitle, R.id.pressureTitle, R.id.uvTitle, R.id.windLabel, R.id.humidityLabel, R.id.pressureLabel, R.id.uvLabel, R.id.cloudCoverLabel, R.id.cloudCoverTitle};

        //SCALE CONTENT
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetIds[0]);
        if(options != null) {
            int widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

            float hourlyTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 12f), Common.dpToPx(context, 9f));
            float temperatureTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 45f), Common.dpToPx(context, 25f));
            float conditionTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 25f), Common.dpToPx(context, 15f));
            //float hourlyImagePadding = getSizeForWidgetSizeRange(widgetHeight, MIN_WIDGET_HEIGHT, MAX_WIDGET_HEIGHT, Common.dpToPx(context, 20f), Common.dpToPx(context, 15f));
           // float currentStatsPadding = getSizeForWidgetSizeRange(widgetHeight, MIN_WIDGET_HEIGHT, MAX_WIDGET_HEIGHT, Common.dpToPx(context, 23f), Common.dpToPx(context, 15f));
            float contentViewPadding = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 10f), Common.dpToPx(context, 3f));
            float hourlyViewPadding = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 10f), Common.dpToPx(context, 3f));
            System.out.println("HOURLY PADDING" + hourlyTextSize);

            /** SET HOURLY IMAGE PADDING **/
            for(int viewID : hourlyImages) {
                //views.setViewPadding(viewID, 0, (int)hourlyImagePadding, 0, (int)hourlyImagePadding);
            }
            /** SET HOURLY AND CURRENT DAY STAT TEXT SIZES **/
            for(int index = 0; index < timeViews.length; index++) {
                views.setTextViewTextSize(timeViews[index], TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
                views.setTextViewTextSize(labelViews[index], TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
            }
            views.setTextViewTextSize(R.id.time1, TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);

            if(displayIndex == 2) {
                for(int label : currentStatLabels) {
                    views.setTextViewTextSize(label, TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
                }
                for(int icon : currentStatImages) {
                    //views.setViewPadding(icon, 0, (int)currentStatsPadding, 0, (int)currentStatsPadding);
                }
            }

            /** SET CONTENT VIEW PADDING **/
            views.setViewPadding(R.id.contentView, (int)contentViewPadding, (int)contentViewPadding, 0, (int)contentViewPadding);
            views.setViewPadding(R.id.hourlyForecastContainer, (int)hourlyViewPadding, (int)Common.dpToPx(context, 5f), (int)hourlyViewPadding, (int)Common.dpToPx(context, 5f));
            views.setViewPadding(R.id.currentInfoContainer, (int)hourlyViewPadding, (int)Common.dpToPx(context, 5f), (int)hourlyViewPadding, (int)Common.dpToPx(context, 5f));

            /** SET TEMPERATURE AND CONDITION TEXT SIZE **/
            views.setTextViewTextSize(R.id.tempText, TypedValue.COMPLEX_UNIT_PX, temperatureTextSize);
            views.setTextViewTextSize(R.id.conditionText, TypedValue.COMPLEX_UNIT_PX, conditionTextSize);
        }

        views.setTextViewText(R.id.tempText, currently.getTemperatureString(isFahrenheit));
        views.setTextViewText(R.id.conditionText, currently.getCondition());

        switch (displayIndex) {
            case 0: /* SHOW MAIN WEATHER SCREEN */
                views.setViewVisibility(R.id.detailsContentView, View.GONE);
                views.setViewVisibility(R.id.contentView, View.VISIBLE);
                views.setViewVisibility(R.id.hourlyRootContainer, View.GONE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);
                break;
            case 1: /* SHOW HOURLY TEMPERATURE */
                views.setViewVisibility(R.id.detailsContentView, View.VISIBLE);
                views.setViewVisibility(R.id.contentView, View.GONE);
                views.setViewVisibility(R.id.hourlyRootContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.GONE);
                views.setViewVisibility(R.id.dropletIcon2, View.GONE);
                views.setViewVisibility(R.id.dropletIcon3, View.GONE);
                views.setViewVisibility(R.id.dropletIcon4, View.GONE);
                views.setViewVisibility(R.id.dropletIcon5, View.GONE);
                break;
            case 2: /* SHOW HOURLY RAIN */
                views.setViewVisibility(R.id.detailsContentView, View.VISIBLE);
                views.setViewVisibility(R.id.contentView, View.GONE);
                views.setViewVisibility(R.id.hourlyRootContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon2, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon3, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon4, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon5, View.VISIBLE);
                break;
            case 3: /* SHOW CURRENT STATS */
                views.setViewVisibility(R.id.detailsContentView, View.VISIBLE);
                views.setViewVisibility(R.id.contentView, View.GONE);
                views.setViewVisibility(R.id.hourlyRootContainer, View.GONE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.VISIBLE);
                break;
            case 4: /* SHOW WEEKLY FORECAST */
                views.setViewVisibility(R.id.detailsContentView, View.VISIBLE);
                views.setViewVisibility(R.id.contentView, View.GONE);
                views.setViewVisibility(R.id.hourlyRootContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.GONE);
                views.setViewVisibility(R.id.dropletIcon2, View.GONE);
                views.setViewVisibility(R.id.dropletIcon3, View.GONE);
                views.setViewVisibility(R.id.dropletIcon4, View.GONE);
                views.setViewVisibility(R.id.dropletIcon5, View.GONE);
                break;
        }

        System.out.println("DISPLAY INDEX" + displayIndex);

        if(displayIndex != 3) {
            /**UPDATE EXTRA WEATHER INFO HOURLY/WEEKLY**/
            for(int v = 0; v < forecast.getHourlyForecast().size(); v++) {

                String data = "";
                String timeText = "";
                int weatherIconID = 0;

                switch(displayIndex) {

                    case 1:
                        data = forecast.getHourlyForecast().get(v).getTemperatureString(isFahrenheit);
                        timeText = forecast.getHourlyForecast().get(v).getHourString();
                        weatherIconID = forecast.getHourlyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, alwaysDayIcons);
                        break;
                    case 2:
                        data = forecast.getHourlyForecast().get(v).getPrecipProbability() + "%";
                        timeText = forecast.getHourlyForecast().get(v).getHourString();
                        weatherIconID = forecast.getHourlyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, alwaysDayIcons);
                        break;
                    case 4:
                        data = forecast.getDailyForecast().get(v).getWeekdayString();
                        timeText = forecast.getDailyForecast().get(v).getHigh(isFahrenheit) + "/" + forecast.getDailyForecast().get(v).getLow(isFahrenheit) + (isFahrenheit ? "°" : "°");
                        weatherIconID = forecast.getDailyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, alwaysDayWeekIcons || alwaysDayIcons);
                        break;
                }

                switch(v) {
                    case 0:
                        views.setImageViewResource(R.id.weather1, weatherIconID);
                        views.setTextViewText(R.id.time1, timeText);
                        views.setTextViewText(R.id.label1, data);
                        break;
                    case 1:
                        views.setImageViewResource(R.id.weather2, weatherIconID);
                        views.setTextViewText(R.id.time2, timeText);
                        views.setTextViewText(R.id.label2, data);
                        break;
                    case 2:
                        views.setImageViewResource(R.id.weather3, weatherIconID);
                        views.setTextViewText(R.id.time3, timeText);
                        views.setTextViewText(R.id.label3, data);
                        break;
                    case 3:
                        views.setImageViewResource(R.id.weather4, weatherIconID);
                        views.setTextViewText(R.id.time4, timeText);
                        views.setTextViewText(R.id.label4, data);
                        break;
                    case 4:
                        views.setImageViewResource(R.id.weather5, weatherIconID);
                        views.setTextViewText(R.id.time5, timeText);
                        views.setTextViewText(R.id.label5, data);
                        break;
                }
            }
        } else { /* UPDATE CURRENT DAY STATS */
            views.setTextViewText(R.id.windLabel, currently.getWindSpeed() + " Mph");
            views.setTextViewText(R.id.humidityLabel, currently.getHumidity() + "%");
            views.setTextViewText(R.id.cloudCoverLabel, currently.getCloudCover() + "%");
            views.setTextViewText(R.id.uvLabel, Integer.toString(currently.getUv()));
            views.setTextViewText(R.id.pressureLabel, Integer.toString(currently.getPressure()));

            if(!alwaysDayIcons) {
                views.setImageViewResource(R.id.windIcon, currently.isDark() ? R.drawable.wind_icon_night : R.drawable.wind_icon);
                views.setImageViewResource(R.id.uvIcon, currently.isDark() ? R.drawable.simple_clear_night : R.drawable.simple_sunny);
                views.setImageViewResource(R.id.cloudCoverIcon, currently.isDark() ? R.drawable.cloud_cover_icon_night : R.drawable.simple_overcast);
                views.setImageViewResource(R.id.humidityIcon, currently.isDark() ? R.drawable.humidity_icon_night : R.drawable.humidity_icon);
            } else {
                views.setImageViewResource(R.id.windIcon, R.drawable.wind_icon);
                views.setImageViewResource(R.id.uvIcon, R.drawable.simple_sunny);
                views.setImageViewResource(R.id.cloudCoverIcon, R.drawable.simple_overcast);
                views.setImageViewResource(R.id.humidityIcon, R.drawable.humidity_icon);
            }
        }

        //Update MAIN weather icon
        views.setImageViewResource(R.id.weatherIcon, currently.getWeatherIconResourceID(WeatherObject.RESOLUTION_LARGE, alwaysDayIcons));

        /** APPLY CHANGES **/
        (AppWidgetManager.getInstance(context)).updateAppWidget(appWidgetIds, views);

        if(refresh) {
            // set up alarm to stop refresh.
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent stopRefreshIntent = new Intent(context, SmallWeatherWidgetProvider.class);
            stopRefreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            stopRefreshIntent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_STOP_REFRESH);
            stopRefreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[0]);
            stopRefreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), stopRefreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pIntent);
        }
    }

    public void setRefreshing(boolean b, int[] appWidgetIDs) {

        if(b) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_small_layout);
            views.setViewVisibility(R.id.rootContentView, View.GONE);
            views.setViewVisibility(R.id.settingsButton, View.GONE);
            views.setViewVisibility(R.id.refreshButton, View.GONE);
            views.setViewVisibility(R.id.refreshProgressView, View.VISIBLE);

            (AppWidgetManager.getInstance(context)).updateAppWidget(appWidgetIDs, views);
        } else {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_small_layout);
            views.setViewVisibility(R.id.rootContentView, View.VISIBLE);
            views.setViewVisibility(R.id.settingsButton, View.VISIBLE);
            views.setViewVisibility(R.id.refreshButton, View.VISIBLE);
            views.setViewVisibility(R.id.refreshProgressView, View.GONE);

            (AppWidgetManager.getInstance(context)).updateAppWidget(appWidgetIDs, views);
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString("lat", Double.toString(location.getLatitude()));
        e.putString("lon", Double.toString(location.getLongitude()));
        e.commit();

        lat = location.getLatitude();
        lon = location.getLongitude();

        System.out.println("LOCATION AUTO UPDATED: " + lat + "   " + lon + "l " + location);

        new UpdateTask().execute();
    }

    @Override
    public void onSettingsChangeDenied() {
        new UpdateTask().execute();
        System.out.println("DENIED");
    }
}
