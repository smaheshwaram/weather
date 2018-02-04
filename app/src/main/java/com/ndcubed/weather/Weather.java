package com.ndcubed.weather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.IntentCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Nathan on 5/10/2017.
 */

public class Weather extends Activity {

    final int NOTIFICATION_ID_PREFIX = 30492;
    final int HOURLY_PRESSED = 1;
    final int CONDITION_PRESSED = 2;
    double lat = 38.960323;
    double lon = -95.263223;


    boolean useDarkTheme = false;
    AnimatedWeatherContainer animatedWeatherContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("WeatherAppPreferences", MODE_PRIVATE);
        useDarkTheme = prefs.getBoolean("useDarkTheme", false);
        setTheme(useDarkTheme ? R.style.NightTheme : R.style.DayTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.weather_app_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            //window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Common.getAttributeColor(this, R.attr.actionBarColor));
        }

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.cloud_hover_animation);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);

        animatedWeatherContainer = (AnimatedWeatherContainer)findViewById(R.id.animatedWeatherContainer);

        //findViewById(R.id.weatherIconOverlay).startAnimation(animation);
        animatedWeatherContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateWeather();
            }
        });

        updateWeather();
    }

    public void updateWeather() {

        SharedPreferences prefs = getSharedPreferences("WeatherAppPreferences", Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong("lastUpdate", System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        boolean didFirstUpdate = prefs.getBoolean("didFirstUpdate", false);

        if(!didFirstUpdate || (currentTime - lastUpdate) > 300000) {
            new UpdateTask().execute();
        } else {
            System.out.println("CACHE UPDATE");
            updateWeather(this, getForecastFromCache(this));
        }
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

            forecast = forecast == null ? getForecastFromCache(Weather.this) : forecast;

            try {
                WeatherObject weatherObject = forecast.getCurrentConditions();

                SharedPreferences prefs = getSharedPreferences("WeatherAppPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor e = prefs.edit();

                e.putLong("date", weatherObject.getDate());
                e.putString("weatherIconString", weatherObject.getWeatherIconString());
                e.putInt("temperature", weatherObject.getTemperature());
                e.putInt("high", weatherObject.getHigh(true));
                e.putInt("low", weatherObject.getLow(true));
                e.putLong("lastUpdate", weatherObject.getCurrentTime());
                e.putBoolean("didFirstUpdate", true);
                e.putLong("sunset",weatherObject.getSunset());
                e.putLong("sunrise", weatherObject.getSunrise());
                e.putInt("weatherID", weatherObject.getWeatherID());
                e.putString("condition", weatherObject.getCondition());
                e.putString("rawHourlyJSON", forecast.getRawHourlyJSON());
                e.putString("rawDailyJSON", forecast.getRawDailyJSON());
                e.putString("alerts", weatherObject.getAlerts());
                e.putString("currentDescription", weatherObject.getDescription());

                if(!prefs.getString("notifications", "").equals("")) {

                    //CANCEL PREVIOUS NOTIFICATIONS
                    NotificationManager mNotifyMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    JSONArray notifications = new JSONArray(prefs.getString("notifications", ""));

                    System.out.println("NNN: " + notifications);

                    for(int i = 0; i < notifications.length(); i++) {

                        JSONObject notification = notifications.getJSONObject(i);
                        mNotifyMgr.cancel(Integer.parseInt(notification.get("id").toString()));
                    }
                }

                if(!weatherObject.getAlerts().equals("")) {

                    JSONArray alerts = new JSONArray(weatherObject.getAlerts());
                    if(alerts.length() > 0) {
                        String notificationString = "[";

                        for(int i = 0; i < alerts.length(); i++) {

                            JSONObject alert = alerts.getJSONObject(i);
                            String alertTitle = alert.get("title").toString();

                            if(alertTitle.toLowerCase().contains("watch") || alertTitle.toLowerCase().contains("warning")) {
                                NotificationCompat.Builder mBuilder =
                                        new NotificationCompat.Builder(Weather.this)
                                                .setSmallIcon(R.drawable.notification_icon)
                                                .setContentTitle(alertTitle)
                                                .setContentText("Until " + WeatherUtils.millisecondsToDateString(WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString()))));


                                Notification notification = mBuilder.build();
                                notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

                                // set up alarm to cancel notification
                                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                Intent intent = new Intent(Weather.this, SmallWeatherWidgetProvider.class);
                                intent.setAction(Common.CANCEL_NOTIFICATION_ACTION);
                                intent.putExtra("notificationID", NOTIFICATION_ID_PREFIX + i);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(Weather.this, i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                alarmManager.set(AlarmManager.RTC, WeatherUtils.getUTCToLocal(Long.parseLong(alert.get("expires").toString())), pendingIntent);

                                NotificationManager mNotifyMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                                mNotifyMgr.notify(NOTIFICATION_ID_PREFIX + i, notification);

                                notificationString += (i > 0 ? "," : "") + "{id= " + NOTIFICATION_ID_PREFIX + i + ", expires= " + Long.parseLong(alert.get("expires").toString()) + "}";
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
                e.commit();


                updateWeather(Weather.this, forecast);
            } catch(Exception err) {
                err.printStackTrace();
                Common.submitError(err);
            }
        }
    }

    public Forecast getForecastFromCache(Context context) {

        SharedPreferences prefs = context.getSharedPreferences("WeatherAppPreferences", Context.MODE_PRIVATE);

        long currentTime = System.currentTimeMillis();
        int temperature = prefs.getInt("temperature", 72);
        int high = prefs.getInt("high", 72);
        int low = prefs.getInt("low", 72);
        long sunset = prefs.getLong("sunset", 0);
        long sunrise = prefs.getLong("sunrise", 0);
        long date = prefs.getLong("date", currentTime);
        String condition = prefs.getString("condition", "Clear");
        String rawHourlyJSON = prefs.getString("rawHourlyJSON", "");
        String rawDailyJSON = prefs.getString("rawDailyJSON", "");
        String weatherIconString = prefs.getString("weatherIconString", "clear-day");
        String currentDescription = prefs.getString("currentDescription", " ");

        System.out.println("JSON: " + rawHourlyJSON);

        WeatherObject currentConditions = new WeatherObject(date, temperature, sunrise, sunset, weatherIconString, condition);
        currentConditions.setHigh(high);
        currentConditions.setLow(low);
        currentConditions.setDescription(currentDescription);

        Forecast forecast = new Forecast(currentConditions);
        forecast.setRawHourlyJSON(rawHourlyJSON);
        forecast.setRawDailyJSON(rawDailyJSON);

        return forecast;
    }

    public void updateWeather(Context context, Forecast forecast) {

        WeatherObject currently = forecast.getCurrentConditions();

        animatedWeatherContainer.setWeatherObject(currently);

        ((TextView)findViewById(R.id.currentTemperature)).setText(currently.getTemperature() + "°");
        ((TextView)findViewById(R.id.currentCondition)).setText(currently.getCondition());
        ((TextView)findViewById(R.id.currentConditionDesc)).setText(currently.getDescription());

        LinearLayout hourlyContainer = ((LinearLayout)findViewById(R.id.hourlyForecastContainer));
        LinearLayout dailyContainer = ((LinearLayout)findViewById(R.id.dailyForecastContainer));
        hourlyContainer.removeAllViews();
        dailyContainer.removeAllViews();

        for(WeatherObject weatherObject : forecast.getHourlyForecast()) {
            View weatherSection = getLayoutInflater().inflate(R.layout.weather_section_layout, hourlyContainer, false);

            ((ImageView)weatherSection.findViewById(R.id.weatherIcon)).setImageResource(weatherObject.getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL));
            ((TextView)weatherSection.findViewById(R.id.temperature)).setText(weatherObject.getTemperature() + "°");
            ((TextView)weatherSection.findViewById(R.id.time)).setText(weatherObject.getHourString());

            if(weatherObject.getPrecipProbability() > 0) {
                weatherSection.findViewById(R.id.temperatureIcon).setVisibility(View.VISIBLE);
                weatherSection.findViewById(R.id.precipIcon).setVisibility(View.VISIBLE);
                weatherSection.findViewById(R.id.precip).setVisibility(View.VISIBLE);
                ((TextView)weatherSection.findViewById(R.id.precip)).setText(weatherObject.getPrecipProbability() + "%");
                ((TextView)weatherSection.findViewById(R.id.temperature)).setText(weatherObject.getTemperature() + "°");
            }

            hourlyContainer.addView(weatherSection);
        }

        //DAILY
        for(WeatherObject weatherObject : forecast.getDailyForecast()) {
            View weatherSection = getLayoutInflater().inflate(R.layout.weather_section_layout, dailyContainer, false);

            ((ImageView)weatherSection.findViewById(R.id.weatherIcon)).setImageResource(weatherObject.getWeatherIconResourceID(WeatherObject.RESOLUTION_SMALL));
            ((TextView)weatherSection.findViewById(R.id.time)).setText(weatherObject.getWeekdayString());

            if(weatherObject.getPrecipProbability() > 0) {
                weatherSection.findViewById(R.id.temperatureIcon).setVisibility(View.VISIBLE);
                weatherSection.findViewById(R.id.precipIcon).setVisibility(View.VISIBLE);
                weatherSection.findViewById(R.id.precip).setVisibility(View.VISIBLE);
                ((TextView)weatherSection.findViewById(R.id.precip)).setText(weatherObject.getPrecipProbability() + "%");
                ((TextView)weatherSection.findViewById(R.id.temperature)).setText(weatherObject.getHigh(true) + "°");
            } else {
                ((TextView)weatherSection.findViewById(R.id.temperature)).setText(weatherObject.getHigh(true) + "°" + " / " + weatherObject.getLow(true) + "°");
            }

            dailyContainer.addView(weatherSection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("WeatherAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();

        long sunset = prefs.getLong("sunset", -1);
        long sunrise = prefs.getLong("sunrise", -1);

        if(sunrise > 0 && sunrise > 0) {
            boolean isDark = System.currentTimeMillis() > sunset && System.currentTimeMillis() > sunrise || System.currentTimeMillis() < sunrise && System.currentTimeMillis() < sunset;;

            if(useDarkTheme != isDark) {
                e.putBoolean("useDarkTheme", isDark);
                e.commit();
                finish();

                final Intent intent = getIntent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("useDarkTheme", isDark);
                startActivity(intent);
            } else {
                updateWeather(this, getForecastFromCache(this));
            }
        }
    }
}
