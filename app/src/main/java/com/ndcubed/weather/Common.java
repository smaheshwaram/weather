package com.ndcubed.weather;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Nathan on 5/10/2017.
 */

public class Common {

    static final String CANCEL_NOTIFICATION_ACTION = "com.ndcubed.weather.action.CANCEL_NOTIFICATION";

    static final boolean IS_DEV_MODE = false;
    static final int UPDATE_NOTIFICATION_ID = 392;

    public static void submitError(final Exception err) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://ndcubed.com/SimplyWeather/php/");
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(true);

                    PrintStream out = new PrintStream(connection.getOutputStream());
                    out.print("query=submitError");
                    out.print("&stackTrace=" + Log.getStackTraceString(err));
                    out.print("&versionCode=" + BuildConfig.VERSION_CODE);

                    connection.getInputStream();
                    out.close();
                } catch(Exception err) {
                    err.printStackTrace();
                }
            }
        }).start();
    }

    public static void submitError(final Exception err, final String extraData) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://ndcubed.com/SimplyWeather/php/");
                    URLConnection connection = url.openConnection();
                    connection.setDoOutput(true);

                    PrintStream out = new PrintStream(connection.getOutputStream());
                    out.print("query=submitError");
                    out.print("&stackTrace=" + Log.getStackTraceString(err));
                    out.print("&versionCode=" + BuildConfig.VERSION_CODE);
                    out.print("&extraData=" + extraData);

                    connection.getInputStream();
                    out.close();
                } catch(Exception err) {
                    err.printStackTrace();
                }
            }
        }).start();
    }

    public static int getAttributeColor(Context context, int attribute) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

    static int getKelvinToFahrenheit(float kelvin) {

        double f = (kelvin * 1.8) - 459.67;
        return (int)(f);
    }

    static int getNumDaysAhead(long currentDate, long futureDate) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("d");
        Date d = new Date(currentDate);

        final long halfHourInMilliseconds = 1800000L;
        int daysAhead = 0;
        int day = Integer.parseInt(dateFormat.format(d));

        for(long date = currentDate; date < futureDate; date += halfHourInMilliseconds) {

            d = new Date(date);
            int newDay = Integer.parseInt(dateFormat.format(d));

            if(newDay != day) {
                daysAhead++;
                day = newDay;
            }
        }

        return daysAhead;
    }

    public static float dpToPx(Context context, float dp){

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public static int getCurrentBuild() {

        try {
            URL url = new URL("http://ndcubed.com/SimplyWeather/build.txt");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);

            urlConnection.setDoOutput(true);
            urlConnection.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            int build = Integer.parseInt(br.readLine());
            br.close();

            return build;
        } catch(Exception err) {
            err.printStackTrace();
            Common.submitError(err);
        }

        return BuildConfig.VERSION_CODE;
    }

    static class AppPreferenceKeys {
        static final String KEY_WIDGET_PROVIDER_LAT = "appLat";
        static final String KEY_WIDGET_PROVIDER_LON = "appLon";
        static final String KEY_LAST_LOCATION_UPDATE = "lastAppLocationUpdate";
        static final String KEY_RECEIVE_UPDATE_NOTIFICATIONS = "updateNotifications";
        static final String KEY_LAST_DISMISSED_NOTIFICATION_BUILD = "lastDismissedNotificationBuild";

    }

    static class PreferenceKeys {

        static final String KEY_USE_FAHRENHEIT = "isFahrenheit";
        static final String KEY_USE_DAY_WEEK_ICONS = "useDayWeekIcons";
        static final String KEY_AUTO_UPDATE = "autoUpdate";
        static final String KEY_AUTO_LOCATION_UPDATE = "autoLocationUpdate";
        static final String KEY_USE_ONLY_DAY_ICONS = "useOnlyDayIcons";
        static final String KEY_USE_CURRENT_LOCATION = "useCurrentLocation";
        static final String KEY_WEATHER_NOTIFICATIONS_ENABLED = "useWeatherNotifications";
        static final String KEY_WAS_CONFIGURED = "wasConfigured";
        static final String KEY_WIDGET_TRANSPARENCY = "backgroundTransparency";
        static final String KEY_WIDGET_TEXT_COLOR = "widgetTextColorInt";
        static final String KEY_WIDGET_TEXT_COLOR_PROGRESS = "textColorProgress";
        static final String KEY_WIDGET_TUTORIAL_INDEX = "tutorialIndex";
        static final String KEY_WIDGET_DID_TUTORIAL = "didTutorial";

        static final String DID_FIRST_UPDATE = "didFirstUpdate";
        static final String KEY_WEATHER_PREFERENCES = "WeatherPreferences";
        static final String KEY_WIDGET_DISPLAY_INDEX = "displayIndex";
        static final String KEY_LAT = "lat";
        static final String KEY_LON = "lon";
        static final String KEY_LAST_UPDATE = "lastUpdate";
    }

    static class WidgetActions {
        static final String KEY_WIDGET_ACTION = "action";
        static final int ACTION_WIDGET_CONFIGURED = 5;
        static final int ACTION_FORCE_REFRESH = 6;
        static final int ACTION_STOP_REFRESH = 7;
        static final int ACTION_TUTORIAL_BUTTON_PRESSED = 8;
        static final int ACTION_REFRESH_REQUESTED = 9;
        static final int ACTION_VIEW_CHANGE_REQUESTED = 10;
        static final int ACTION_WIDGET_CONFIGURED_FORCE_REFRESH = 11;
        static final int ACTION_NONE = -1;
        static final int ACTION_DO_NOTHING = -2;
        static final int ACTION_WAIT_FOR_LOCATION = 12;
        static final int ACTION_UPDATE_NOTIFICATION_DISMISSED = 13;
    }
}
