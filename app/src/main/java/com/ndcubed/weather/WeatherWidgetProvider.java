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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Nathan on 5/10/2017.
 */

public class WeatherWidgetProvider extends AppWidgetProvider implements LocationManager.LocationManagerListener {

    double lat, lon;

    final float MIN_WIDGET_HEIGHT = 100f;
    final float MAX_WIDGET_HEIGHT = 160f;
    final float MAX_WIDGET_WIDTH = 340f;
    final float MIN_WIDGET_WIDTH = 270f;

    int lastNotificationBuild = -1;
    int event = -1;
    int cachedEvent = -1;
    int thisWidgetID = -1;

    Context context;
    AppWidgetManager appWidgetManager;
    int[] appWidgetIds;

    boolean showHighLow = false;
    String test = "null";

    LocationManager locationManager;

    ArrayList<WidgetState> widgets = new ArrayList<>();
    ArrayList<WidgetState> waitingForLocationWidgets = new ArrayList<>();

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + appWidgetId, Context.MODE_PRIVATE);
        System.out.println("OPTIONS CHANGED..");
        //updateWeather(context, getWeatherFromCache(context), appWidgetId, appWidgetManager);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        System.out.println("ENABLED..");
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("RECEIVE..");

        ComponentName largeWeatherWidget = new ComponentName(context, WeatherWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(largeWeatherWidget);
        event = intent.getIntExtra("action", -1);
        lastNotificationBuild = intent.getIntExtra("build", -1);

        if(intent.getExtras() == null){ //Update all widgets.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        System.out.println("RESTORED..");
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        this.context = context;
        this.appWidgetIds = appWidgetIds;
        this.appWidgetManager = appWidgetManager;

        if(locationManager == null) {
            locationManager = new LocationManager(context);
            locationManager.connect(context);
            locationManager.setLocationManagerListener(this);
        }

        System.out.println("WIDGETEVENT: " + event);

        /* Check for updates if set to receive update notifications. */
        SharedPreferences appPreferences = context.getSharedPreferences("WeatherPreferences", Context.MODE_PRIVATE);
        if(appPreferences.getBoolean(Common.AppPreferenceKeys.KEY_RECEIVE_UPDATE_NOTIFICATIONS, true) && event != Common.WidgetActions.ACTION_UPDATE_NOTIFICATION_DISMISSED) {
            new CheckBuildTask(new WidgetState(context, appWidgetIds[0])).execute();
        }

        /* Add click intents to Widgets. */
        for(int widgetID : appWidgetIds) {
            WidgetState widget = new WidgetState(context, widgetID);
            initWidget(widget);

            widgets.add(widget);
        }

        /* Check to see if widgets need current location if so retrieve current location and reattempt update */
        boolean needsLocationUpdate = false;
        for(WidgetState widgetState : widgets) {
            if(doesWidgetRequireLocationUpdate(widgetState, event)) {
                needsLocationUpdate = true;
                waitingForLocationWidgets.add(widgetState);
                System.out.println("NEEDS UPDATE::");
            }
        }
        /* Check to see if a recent current location has been stored */
        if(needsLocationUpdate) {
            cachedEvent = event;
            event = Common.WidgetActions.ACTION_WAIT_FOR_LOCATION;
            locationManager.getCurrentLocation();
        }

        /* Handle events */
        switch(event) {

            case Common.WidgetActions.ACTION_WIDGET_CONFIGURED:
                for(WidgetState widgetState : widgets) {
                    updateWidget(appWidgetManager, widgetState, true);
                }
                break;
            case Common.WidgetActions.ACTION_WIDGET_CONFIGURED_FORCE_REFRESH:
                for(WidgetState widgetState : widgets) {
                    updateWidget(appWidgetManager, widgetState, false);
                }
                break;
            case Common.WidgetActions.ACTION_STOP_REFRESH:
                for(WidgetState widgetState : widgets) {
                    setRefreshing(false, widgetState);
                }
                break;
            case Common.WidgetActions.ACTION_REFRESH_REQUESTED:
                for(WidgetState widgetState : widgets) {
                    long currentTime = System.currentTimeMillis();
                    if(currentTime - widgetState.getLastUpdate() > 300000) { /* Update widget from server */
                        updateWidget(appWidgetManager, widgetState, false);
                    } else { /* Update widget from Cache */
                        updateWidget(appWidgetManager, widgetState, true);
                    }
                }
                break;
            case Common.WidgetActions.ACTION_VIEW_CHANGE_REQUESTED:
                for(WidgetState widgetState : widgets) {
                    int displayIndex = widgetState.getDisplayIndex() + 1;
                    if(displayIndex > 3) displayIndex = 0;
                    widgetState.setDisplayIndex(displayIndex);

                    updateWeather(context, getWeatherFromCache(widgetState), widgetState, appWidgetManager, true);
                }
                break;
            case Common.WidgetActions.ACTION_WAIT_FOR_LOCATION:
                for(WidgetState widgetState : waitingForLocationWidgets) {
                    setRefreshing(true, widgetState);
                }
                break;
            case Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED:
                for(WidgetState widgetState : widgets) {
                    RemoteViews views = new RemoteViews(widgetState.getContext().getPackageName(), R.layout.weather_widget_compact_layout);
                    int tutorialIndex = widgetState.getTutorialIndex();

                    System.out.println("TUTORIAL INDEX: " + tutorialIndex);

                    switch(tutorialIndex) {

                        case 0:
                            views.setViewVisibility(R.id.tutorialTextTitle, View.GONE);
                            views.setViewVisibility(R.id.tutorialTextView, View.VISIBLE);
                            views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText1));
                            break;
                        case 1:
                            views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText2));
                            break;
                        case 2:
                            views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText3));
                            break;
                        case 3:
                            views.setTextViewText(R.id.tutorialNextButton, context.getResources().getString(R.string.tutorialButtonFinishText));
                            views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText4));
                            break;
                        case 4:
                            views.setViewVisibility(R.id.tutorialView, View.GONE);
                            widgetState.setDidTutorial(true);

                            Intent intent = new Intent(context, WeatherWidgetProvider.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_FORCE_REFRESH);
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                            context.sendBroadcast(intent);
                            break;
                    }

                    tutorialIndex += 1;
                    widgetState.setTutorialIndex(tutorialIndex);
                    widgetState.saveWidgetState();

                    appWidgetManager.updateAppWidget(widgetState.getWidgetID(), views);
                }
                break;
            case Common.WidgetActions.ACTION_UPDATE_NOTIFICATION_DISMISSED:
                System.out.println("NOTIFICATION DISMISSED: " + lastNotificationBuild);
                SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();
                e.putInt(Common.AppPreferenceKeys.KEY_LAST_DISMISSED_NOTIFICATION_BUILD, lastNotificationBuild);
                e.apply();
                break;
            default:
                for(WidgetState widgetState : widgets) {
                    if(widgetState.wasConfigured() && widgetState.isAutoRefresh()) {
                        long currentTime = System.currentTimeMillis();
                        if(currentTime - widgetState.getLastUpdate() > 300000) { /* Update widget from server */
                            updateWidget(appWidgetManager, widgetState, false);
                        } else { /* Update widget from Cache */
                            updateWidget(appWidgetManager, widgetState, true);
                        }
                    }
                }
                break;
        }

        System.out.println(event + " TEST VAR");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        System.out.println("DELETED..");
        for(int id : appWidgetIds) {
            SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + id, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = prefs.edit();
            e.clear();
            e.apply();
        }
        super.onDeleted(context, appWidgetIds);
    }

    private boolean doesWidgetRequireLocationUpdate(WidgetState widgetState, int event) {

        boolean widgetRefreshTimeoutExpired = (System.currentTimeMillis() - widgetState.getLastUpdate() > 300000);

        if(widgetState.useCurrentLocation()) {
            /* Check to see if a recent current location has been stored */
            SharedPreferences prefs = widgetState.getContext().getSharedPreferences("WeatherPreferences", Context.MODE_PRIVATE);
            long lastLocationUpdate = prefs.getLong(Common.AppPreferenceKeys.KEY_LAST_LOCATION_UPDATE, 0);

            /* Use cached current location if received less than 3 min ago.
            *  Otherwise attempt to retrieve current location and delay widget update.*/
            if(System.currentTimeMillis() - lastLocationUpdate < 5000) {
                double lat = Double.parseDouble(prefs.getString(Common.AppPreferenceKeys.KEY_WIDGET_PROVIDER_LAT, "0"));
                double lon = Double.parseDouble(prefs.getString(Common.AppPreferenceKeys.KEY_WIDGET_PROVIDER_LON, "0"));

            /* Update the lat lon of widgets who need the current location. */
                widgetState.setLat(lat);
                widgetState.setLon(lon);
                widgetState.saveWidgetState();
                return false;
            } else {
                switch(event) {

                    case Common.WidgetActions.ACTION_WIDGET_CONFIGURED:
                    case Common.WidgetActions.ACTION_VIEW_CHANGE_REQUESTED:
                        return false;
                    case Common.WidgetActions.ACTION_WIDGET_CONFIGURED_FORCE_REFRESH:
                        return true;
                    case Common.WidgetActions.ACTION_REFRESH_REQUESTED:
                        return true;
                    default:
                        return widgetRefreshTimeoutExpired;
                }
            }
        } else {
            return false;
        }
    }

    private void updateWidget(AppWidgetManager appWidgetManager, WidgetState widgetState, boolean useCache) {
        setRefreshing(true, widgetState);

        if(useCache) {
            Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();
            updateWeather(widgetState.getContext(), getWeatherFromCache(widgetState), widgetState, appWidgetManager, true);
        } else {
            Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();

            /* Check to see if widget is required to use current location if so retrieve the current location
            *  before refreshing the widget.*/



            new UpdateTask(widgetState).execute();
        }
    }

    private void initWidget(WidgetState widgetState) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(widgetState.getContext());

        SharedPreferences prefs = widgetState.getContext().getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES + widgetState.getWidgetID(), Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();

        e.putInt(Common.WidgetActions.KEY_WIDGET_ACTION, event);
        e.commit();

        /** ADD LISTENERS **/
        RemoteViews views = new RemoteViews(widgetState.getContext().getPackageName(), R.layout.weather_widget_compact_layout);

        //TUTORIAL NEXT BUTTON INTENT
        Intent tutorialIntent = new Intent(widgetState.getContext(), WeatherWidgetProvider.class);
        tutorialIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        tutorialIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
        tutorialIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetState.getWidgetID());
        tutorialIntent.putExtra("action", Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED);
        PendingIntent tutorialPendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), tutorialIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.tutorialNextButton, tutorialPendingIntent);

        //SETTINGS INTENT
        Intent settingsIntent = new Intent(widgetState.getContext(), WeatherWidgetConfigure.class);
        settingsIntent.setData(Uri.parse(settingsIntent.toUri(Intent.URI_INTENT_SCHEME)));
        settingsIntent.putExtra("isUserConfigure", true);
        settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
        settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetState.getWidgetID());
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(context, getUniqueIntentID(), settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.settingsButton, settingsPendingIntent);

        //HOURLY TEMPERATURE TOGGLE INTENT
        Intent hourlyIntent = new Intent(widgetState.getContext(), WeatherWidgetProvider.class);
        hourlyIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        hourlyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
        hourlyIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetState.getWidgetID());
        hourlyIntent.putExtra("action", Common.WidgetActions.ACTION_VIEW_CHANGE_REQUESTED);
        PendingIntent hourlyPendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), hourlyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.detailsRootContainer, hourlyPendingIntent);

        //REFRESH INTENT
        Intent refreshIntent = new Intent(widgetState.getContext(), WeatherWidgetProvider.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshIntent.putExtra("action", Common.WidgetActions.ACTION_REFRESH_REQUESTED);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetState.getWidgetID());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.topHalfView, pendingIntent);

        /* Set Widget Background Transparency */
        views.setInt(R.id.topWidgetBackground, "setImageAlpha", widgetState.getWidgetTransparency());
        views.setInt(R.id.bottomWidgetBackground, "setImageAlpha", widgetState.getWidgetTransparency());

        /*Set Widget Text Colors */
        int[] currentStatLabels = new int[]{R.id.windTitle, R.id.humidityTitle, R.id.pressureTitle, R.id.uvTitle, R.id.windLabel, R.id.humidityLabel, R.id.pressureLabel, R.id.uvLabel, R.id.cloudCoverLabel, R.id.cloudCoverTitle};
        int[] timeViews = new int[]{R.id.time1, R.id.time2, R.id.time3, R.id.time4, R.id.time5};
        int[] labelViews = new int[]{R.id.label1, R.id.label2, R.id.label3, R.id.label4, R.id.label5};

        for(int id : currentStatLabels) {
            views.setTextColor(id, widgetState.getWidgetTextColor());
        }
        for(int id : timeViews) {
            views.setTextColor(id, widgetState.getWidgetTextColor());
        }
        for(int id : labelViews) {
            views.setTextColor(id, widgetState.getWidgetTextColor());
        }
        views.setTextColor(R.id.conditionText, widgetState.getWidgetTextColor());
        views.setTextColor(R.id.tempText, widgetState.getWidgetTextColor());

        if(!prefs.getBoolean("didTutorial", false)) {
            views.setViewVisibility(R.id.tutorialView, View.VISIBLE);

            //scale text
            Bundle options = appWidgetManager.getAppWidgetOptions(widgetState.getWidgetID());
            if(options != null) {
                int widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

                float textSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 16f), Common.dpToPx(context, 11f));
                float titleTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 25f), Common.dpToPx(context, 18f));
                views.setTextViewTextSize(R.id.tutorialTextView, TypedValue.COMPLEX_UNIT_PX, textSize);
                views.setTextViewTextSize(R.id.tutorialTextTitle, TypedValue.COMPLEX_UNIT_PX, titleTextSize);
                System.out.println("SCALE: ll");
            }
        }

        /*
        if(event == HOURLY_PRESSED) {
            displayIndex = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_DISPLAY_INDEX, 0);
            displayIndex += 1;
            if(displayIndex > 3) displayIndex = 0;

            e.putInt(Common.PreferenceKeys.KEY_WIDGET_DISPLAY_INDEX, displayIndex);
        } else if(event == CONDITION_PRESSED) {
            showHighLow = prefs.getBoolean("showHighLow", false);
            showHighLow = !showHighLow;
            e.putBoolean("showHighLow", showHighLow);
        } else if(event == Common.WidgetActions.ACTION_STOP_REFRESH) {
            System.out.println("STOP REFRESH");

            views.setViewVisibility(R.id.refreshProgressView, View.GONE);
            views.setViewVisibility(R.id.contentView, View.VISIBLE);
            views.setViewVisibility(R.id.settingsButton, View.VISIBLE);
        } else if(event == Common.WidgetActions.ACTION_TUTORIAL_BUTTON_PRESSED) {
            tutorialIndex = prefs.getInt("tutorialIndex", 0);
            System.out.println("TUTORIAL INDEX: " + tutorialIndex);

            switch(tutorialIndex) {

                case 0:
                    views.setViewVisibility(R.id.tutorialTextTitle, View.GONE);
                    views.setViewVisibility(R.id.tutorialTextView, View.VISIBLE);
                    views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText1));
                    break;
                case 1:
                    views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText2));
                    break;
                case 2:
                    views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText3));
                    break;
                case 3:
                    views.setTextViewText(R.id.tutorialNextButton, context.getResources().getString(R.string.tutorialButtonFinishText));
                    views.setTextViewText(R.id.tutorialTextView, context.getResources().getString(R.string.tutorialText4));
                    break;
                case 4:
                    views.setViewVisibility(R.id.tutorialView, View.GONE);
                    e.putBoolean("didTutorial", true);
                    e.putInt(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_FORCE_REFRESH);
                    break;
            }

            tutorialIndex += 1;
            e.putInt("tutorialIndex", tutorialIndex);
        }
        */

        (AppWidgetManager.getInstance(widgetState.getContext())).updateAppWidget(widgetState.getWidgetID(), views);
    }

    public int getUniqueIntentID() {
        return (int)(10000 * Math.random());
    }

    private class CheckBuildTask extends AsyncTask<String, Void, Integer> {

        WidgetState widgetState;

        public CheckBuildTask(WidgetState widgetState) {
            this.widgetState = widgetState;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            return Common.getCurrentBuild();
        }

        @Override
        protected void onPostExecute(Integer latestBuild) {

            System.out.println("BUILD LATEST: " + latestBuild);

            SharedPreferences prefs = widgetState.getContext().getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES, Context.MODE_PRIVATE);
            lastNotificationBuild = prefs.getInt(Common.AppPreferenceKeys.KEY_LAST_DISMISSED_NOTIFICATION_BUILD, -1);

            if(latestBuild != BuildConfig.VERSION_CODE && lastNotificationBuild != latestBuild) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(widgetState.getContext())
                                .setSmallIcon(R.drawable.basic_notification_icon)
                                .setAutoCancel(true)
                                .setStyle(new NotificationCompat.BigTextStyle())
                                .setContentTitle("Simply Weather Widget")
                                .setContentText("There's a new update for Simply Weather! To stop receiving these notifications just visit settings from your weather widget.");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.ndcubed.weather"));
                PendingIntent pendingIntent = PendingIntent.getActivity(widgetState.getContext(), 4059, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(pendingIntent);

                intent = new Intent(widgetState.getContext(), WeatherWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_UPDATE_NOTIFICATION_DISMISSED);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
                intent.putExtra("build", latestBuild);
                pendingIntent = PendingIntent.getBroadcast(widgetState.getContext(), 3409, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                mBuilder.setDeleteIntent(pendingIntent);

                Notification notification = mBuilder.build();
                NotificationManager mNotifyMgr = (NotificationManager)widgetState.getContext().getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(Common.UPDATE_NOTIFICATION_ID, notification);
            }
            super.onPostExecute(latestBuild);
        }
    }

    private class UpdateTask extends AsyncTask<String, Void, Forecast> {

        private WidgetState widgetState;

        public UpdateTask(WidgetState widgetState) {
            this.widgetState = widgetState;
            System.out.println("WIDGET LAT LON: " + widgetState.getLat() + "  " + widgetState.getLon());
        }

        @Override
        protected Forecast doInBackground(String... strings) {
            try {
                return WeatherUtils.getCurrentForecast(widgetState.getLat(), widgetState.getLon());
            } catch(Exception err) {
                err.printStackTrace();
                Common.submitError(err);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Forecast forecast) {

            forecast = forecast == null ? getWeatherFromCache(widgetState) : forecast;

            try {
                WeatherObject weatherObject = forecast.getCurrentConditions();

                SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + widgetState.getWidgetID(), Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();

                e.putLong("date", weatherObject.getDate());
                e.putString("weatherIconString", weatherObject.getWeatherIconString());
                e.putInt("temperature", weatherObject.getTemperature());
                e.putInt("high", weatherObject.getHigh(widgetState.isFahrenheit()));
                e.putInt("low", weatherObject.getLow(widgetState.isFahrenheit()));
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
                    NotificationManager mNotifyMgr = (NotificationManager)widgetState.getContext().getSystemService(NOTIFICATION_SERVICE);
                    JSONArray notifications = new JSONArray(prefs.getString("notifications", ""));

                    System.out.println("NNN: " + notifications);

                    for(int i = 0; i < notifications.length(); i++) {

                        JSONObject notification = notifications.getJSONObject(i);
                        mNotifyMgr.cancel(Integer.parseInt(notification.get("id").toString()));
                    }
                }

                if(widgetState.isWeatherNotificationsEnabled()) {
                    if(!weatherObject.getAlerts().equals("")) {

                        JSONArray alerts = new JSONArray(weatherObject.getAlerts());
                        if(alerts.length() > 0) {
                            String notificationString = "[";

                            for(int i = 0; i < alerts.length(); i++) {

                                JSONObject alert = alerts.getJSONObject(i);
                                String alertTitle = alert.get("title").toString();

                                if(alertTitle.toLowerCase().contains("watch") || alertTitle.toLowerCase().contains("warning")) {
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(widgetState.getContext())
                                                    .setSmallIcon(R.drawable.notification_icon)
                                                    .setContentTitle(alertTitle)
                                                    .setContentText("Until " + WeatherUtils.millisecondsToDateString(WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString()))));


                                    Notification notification = mBuilder.build();
                                   // notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

                                    int notificationID = (int)(Integer.MAX_VALUE * Math.random());

                                    // set up alarm to cancel notification
                                    AlarmManager alarmManager = (AlarmManager) widgetState.getContext().getSystemService(Context.ALARM_SERVICE);
                                    Intent intent = new Intent(context, WeatherWidgetProvider.class);
                                    intent.setAction(Common.CANCEL_NOTIFICATION_ACTION);
                                    intent.putExtra("notificationID", notificationID);
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(widgetState.getContext(), notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    alarmManager.set(AlarmManager.RTC, WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString())), pendingIntent);

                                    NotificationManager mNotifyMgr = (NotificationManager)widgetState.getContext().getSystemService(NOTIFICATION_SERVICE);
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

                updateWeather(widgetState.getContext(), forecast, widgetState, appWidgetManager, false);
            } catch(Exception err) {
                updateWeather(widgetState.getContext(), getWeatherFromCache(widgetState), widgetState, appWidgetManager, true);
                err.printStackTrace();
                Common.submitError(err, forecast.getRawDailyJSON());
            }
        }
    }

    public Forecast getWeatherFromCache(WidgetState widgetState) {

        SharedPreferences prefs = context.getSharedPreferences("WeatherPreferences" + widgetState.getWidgetID(), Context.MODE_PRIVATE);

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

        System.out.println("JSON: " + weatherIconString);

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

    public void updateWeather(Context context, Forecast forecast, WidgetState widgetState, AppWidgetManager appWidgetManager, boolean wasFromCache) {
        WeatherObject currently = forecast.getCurrentConditions();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_compact_layout);
        int[] timeViews = new int[]{R.id.time1, R.id.time2, R.id.time3, R.id.time4, R.id.time5};
        int[] labelViews = new int[]{R.id.label1, R.id.label2, R.id.label3, R.id.label4, R.id.label5};
        int[] hourlyImages = new int[]{R.id.weather1, R.id.weather2, R.id.weather3, R.id.weather4, R.id.weather5};
        int[] currentStatImages = new int[]{R.id.windIcon, R.id.cloudCoverIcon, R.id.pressureIcon, R.id.humidityIcon, R.id.uvIcon};
        int[] currentStatLabels = new int[]{R.id.windTitle, R.id.humidityTitle, R.id.pressureTitle, R.id.uvTitle, R.id.windLabel, R.id.humidityLabel, R.id.pressureLabel, R.id.uvLabel, R.id.cloudCoverLabel, R.id.cloudCoverTitle};

        //SCALE CONTENT
        Bundle options = appWidgetManager.getAppWidgetOptions(widgetState.getWidgetID());
        if(options != null) {
            int widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

            float hourlyTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 12f), Common.dpToPx(context, 9f));
            float temperatureTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 45f), Common.dpToPx(context, 25f));
            float conditionTextSize = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 25f), Common.dpToPx(context, 15f));
            float hourlyImagePadding = getSizeForWidgetSizeRange(widgetHeight, MIN_WIDGET_HEIGHT, MAX_WIDGET_HEIGHT, Common.dpToPx(context, 20f), Common.dpToPx(context, 17f));
            float currentStatsPadding = getSizeForWidgetSizeRange(widgetHeight, MIN_WIDGET_HEIGHT, MAX_WIDGET_HEIGHT, Common.dpToPx(context, 20f), Common.dpToPx(context, 17f));
            float contentViewPadding = getSizeForWidgetSizeRange(widgetWidth, MIN_WIDGET_WIDTH, MAX_WIDGET_WIDTH, Common.dpToPx(context, 10f), Common.dpToPx(context, 3f));
            float contentViewTopPadding = getSizeForWidgetSizeRange(widgetHeight, MIN_WIDGET_HEIGHT, MAX_WIDGET_HEIGHT, Common.dpToPx(context, 5f), Common.dpToPx(context, 1f));
            System.out.println("HOURLY PADDING" + contentViewTopPadding);

            /** SET HOURLY IMAGE PADDING **/
            for(int viewID : hourlyImages) {
                views.setViewPadding(viewID, 0, (int)hourlyImagePadding, 0, (int)hourlyImagePadding);
            }
            /** SET HOURLY AND CURRENT DAY STAT TEXT SIZES **/
            for(int index = 0; index < timeViews.length; index++) {
                views.setTextViewTextSize(timeViews[index], TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
                views.setTextViewTextSize(labelViews[index], TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
            }

            if(widgetState.getDisplayIndex() == 2) {
                for(int label : currentStatLabels) {
                    views.setTextViewTextSize(label, TypedValue.COMPLEX_UNIT_PX, hourlyTextSize);
                }
                for(int icon : currentStatImages) {
                    views.setViewPadding(icon, 0, (int)currentStatsPadding, 0, (int)currentStatsPadding);
                }
            }

            /** SET CONTENT VIEW PADDING **/
            views.setViewPadding(R.id.contentView, (int)contentViewPadding, (int)contentViewPadding, 0, (int)contentViewPadding);
            views.setViewPadding(R.id.detailsRootContainer, (int)contentViewPadding, (int)contentViewTopPadding, (int)contentViewPadding, (int)contentViewTopPadding);

            /** SET TEMPERATURE AND CONDITION TEXT SIZE **/
            views.setTextViewTextSize(R.id.tempText, TypedValue.COMPLEX_UNIT_PX, temperatureTextSize);
            views.setTextViewTextSize(R.id.conditionText, TypedValue.COMPLEX_UNIT_PX, conditionTextSize);
        }

        views.setTextViewText(R.id.tempText, currently.getTemperatureString(widgetState.isFahrenheit()));
        views.setTextViewText(R.id.conditionText, currently.getCondition());

        System.out.println("WIDGET DISPLAY INDEX: " + widgetState.getDisplayIndex());
        switch (widgetState.getDisplayIndex()) {
            case 0: /* SHOW HOURLY TEMPERATURE */
            System.out.println("SHOWHOUR");
                views.setViewVisibility(R.id.hourlyForecastContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.GONE);
                views.setViewVisibility(R.id.dropletIcon2, View.GONE);
                views.setViewVisibility(R.id.dropletIcon3, View.GONE);
                views.setViewVisibility(R.id.dropletIcon4, View.GONE);
                views.setViewVisibility(R.id.dropletIcon5, View.GONE);
                break;
            case 1: /* SHOW HOURLY RAIN */
                views.setViewVisibility(R.id.hourlyForecastContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon2, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon3, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon4, View.VISIBLE);
                views.setViewVisibility(R.id.dropletIcon5, View.VISIBLE);
                break;
            case 2: /* SHOW CURRENT STATS */
                views.setViewVisibility(R.id.hourlyForecastContainer, View.GONE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.VISIBLE);
                break;
            case 3: /* SHOW WEEKLY FORECAST */
                views.setViewVisibility(R.id.hourlyForecastContainer, View.VISIBLE);
                views.setViewVisibility(R.id.currentInfoRootContainer, View.GONE);

                views.setViewVisibility(R.id.dropletIcon1, View.GONE);
                views.setViewVisibility(R.id.dropletIcon2, View.GONE);
                views.setViewVisibility(R.id.dropletIcon3, View.GONE);
                views.setViewVisibility(R.id.dropletIcon4, View.GONE);
                views.setViewVisibility(R.id.dropletIcon5, View.GONE);
                break;
        }

        System.out.println("DISPLAY INDEX" + widgetState.getDisplayIndex());

        if(widgetState.getDisplayIndex() != 2) {
            /**UPDATE EXTRA WEATHER INFO HOURLY/WEEKLY**/
            System.out.println("SIZE: " + forecast.getHourlyForecast().size());
            for(int v = 0; v < forecast.getHourlyForecast().size(); v++) {
                System.out.println("HOUR: " + v);

                String data = "";
                String timeText = "";
                int weatherIconID = 0;

                switch(widgetState.getDisplayIndex()) {

                    case 0:
                        data = forecast.getHourlyForecast().get(v).getTemperatureString(widgetState.isFahrenheit());
                        timeText = forecast.getHourlyForecast().get(v).getHourString();
                        weatherIconID = forecast.getHourlyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, widgetState.isAlwaysDayIcons());
                        break;
                    case 1:
                        data = forecast.getHourlyForecast().get(v).getPrecipProbability() + "%";
                        timeText = forecast.getHourlyForecast().get(v).getHourString();
                        weatherIconID = forecast.getHourlyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, widgetState.isAlwaysDayIcons());
                        break;
                    case 3:
                        data = forecast.getDailyForecast().get(v).getWeekdayString();
                        timeText = forecast.getDailyForecast().get(v).getHigh(widgetState.isFahrenheit()) + "/" + forecast.getDailyForecast().get(v).getLow(widgetState.isFahrenheit()) + (widgetState.isFahrenheit() ? "°" : "°");
                        weatherIconID = forecast.getDailyForecast().get(v).getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL, widgetState.isAlwaysDayWeekIcons() || widgetState.isAlwaysDayIcons());
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

            if(!widgetState.isAlwaysDayIcons()) {
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
        views.setImageViewResource(R.id.weatherIcon, currently.getWeatherIconResourceID(WeatherObject.RESOLUTION_LARGE, widgetState.isAlwaysDayIcons()));

        /** APPLY CHANGES **/
        finalizeWidgetUpdate(views, widgetState, wasFromCache);
    }

    public void finalizeWidgetUpdate(RemoteViews views, WidgetState widgetState, boolean wasFromCache) {

        /* Finalize widget variables and last update time. */
        if(!wasFromCache) {
            widgetState.setLastUpdate(System.currentTimeMillis());
        }
        widgetState.saveWidgetState();

        /* APPLY VIEW CHANGES */
        (AppWidgetManager.getInstance(widgetState.getContext())).updateAppWidget(widgetState.getWidgetID(), views);

        /* set up alarm to stop refresh. */
        AlarmManager alarmManager = (AlarmManager) widgetState.getContext().getSystemService(Context.ALARM_SERVICE);
        Intent stopRefreshIntent = new Intent(widgetState.getContext(), WeatherWidgetProvider.class);
        stopRefreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        stopRefreshIntent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, Common.WidgetActions.ACTION_STOP_REFRESH);
        stopRefreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetState.getWidgetID());
        stopRefreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetState.getWidgetID()});
        PendingIntent pIntent = PendingIntent.getBroadcast(context, getUniqueIntentID(), stopRefreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pIntent);
    }

    public void setRefreshing(boolean b, WidgetState widgetState) {

        if(b) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_compact_layout);
            views.setViewVisibility(R.id.contentView, View.GONE);
            views.setViewVisibility(R.id.settingsButton, View.GONE);
            views.setViewVisibility(R.id.refreshProgressView, View.VISIBLE);

            (AppWidgetManager.getInstance(context)).partiallyUpdateAppWidget(widgetState.getWidgetID(), views);
        } else {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_compact_layout);
            views.setViewVisibility(R.id.refreshProgressView, View.GONE);
            views.setViewVisibility(R.id.contentView, View.VISIBLE);
            views.setViewVisibility(R.id.settingsButton, View.VISIBLE);

            (AppWidgetManager.getInstance(context)).updateAppWidget(widgetState.getWidgetID(), views);
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();

        e.putString(Common.AppPreferenceKeys.KEY_WIDGET_PROVIDER_LAT, Double.toString(location.getLatitude()));
        e.putString(Common.AppPreferenceKeys.KEY_WIDGET_PROVIDER_LON, Double.toString(location.getLongitude()));
        e.putLong(Common.AppPreferenceKeys.KEY_LAST_LOCATION_UPDATE, System.currentTimeMillis());
        e.commit();

        System.out.println("LOCATION AUTO UPDATED: " + lat + "   " + lon + "l " + location);

        int[] widgetIDs = new int[waitingForLocationWidgets.size()];
        for(int i = 0; i < widgetIDs.length; i++) {
            widgetIDs[i] = waitingForLocationWidgets.get(i).getWidgetID();
        }

        waitingForLocationWidgets.clear();

        Intent intent = new Intent(context, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, cachedEvent);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onSettingsChangeDenied() {
        //new UpdateTask().execute();

        SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();

        e.putLong(Common.AppPreferenceKeys.KEY_LAST_LOCATION_UPDATE, System.currentTimeMillis());
        e.commit();

        int[] widgetIDs = new int[waitingForLocationWidgets.size()];
        for(int i = 0; i < widgetIDs.length; i++) {
            widgetIDs[i] = waitingForLocationWidgets.get(i).getWidgetID();
        }

        waitingForLocationWidgets.clear();

        Intent intent = new Intent(context, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(Common.WidgetActions.KEY_WIDGET_ACTION, cachedEvent);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(intent);


        System.out.println("DENIED");
    }
}
