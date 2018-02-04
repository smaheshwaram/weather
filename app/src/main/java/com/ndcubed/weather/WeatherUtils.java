package com.ndcubed.weather;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Nathan on 5/10/2017.
 */

class WeatherUtils {

    static String millisecondsToDateString(long milliseconds) {

        SimpleDateFormat format = new SimpleDateFormat("h:mm a");
        return format.format(new Date(milliseconds));
    }

    static int getTemperature(JSONObject weather) {

        try {
            float kelvin = Float.parseFloat(getCategory(weather, "main").get("temp").toString());
            return Common.getKelvinToFahrenheit(kelvin);
        } catch (Exception err) {
            err.printStackTrace();
            Common.submitError(err);
        }

        return 0;
    }

    static long getUnixTime() {
        return System.currentTimeMillis() / 1000;
    }

    private static JSONObject getCategory(JSONObject json, String categoryName) throws JSONException {

        return json.getJSONObject(categoryName);
    }

    /*
    static long getSunriseFromUTC(String utc) {

        //2017-05-11T11:10:58+00:00
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date utcDate = simpleDateFormat.parse(utc);

            String timeZone = Calendar.getInstance().getTimeZone().getID();
            Date local = new Date(utcDate.getTime() + TimeZone.getTimeZone(timeZone).getOffset(utcDate.getTime()));

            System.out.println("SUNRISE: " + local.toString());
            return local.getTime();
        } catch (Exception err) {
            err.printStackTrace();
        }

        return 0;
    }
    */

    static long getUTCToLocal(long utc) {

        try {
            String timeZone = Calendar.getInstance().getTimeZone().getID();
            Calendar c = Calendar.getInstance();

            Date utcDate = new Date(utc * 1000L);
            Date local = new Date(utcDate.getTime() + TimeZone.getTimeZone(timeZone).getOffset(utcDate.getTime()));

            return utcDate.getTime();
        } catch (Exception err) {
            err.printStackTrace();
            Common.submitError(err);
        }

        return 0;
    }

    static long getUTCToLocal(String utc) {

        //2017-05-11T11:10:58+00:00
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date utcDate = simpleDateFormat.parse(utc);

            String timeZone = Calendar.getInstance().getTimeZone().getID();
            Date local = new Date(utcDate.getTime() + TimeZone.getTimeZone(timeZone).getOffset(utcDate.getTime()));

            System.out.println("SUNRISE: " + local.toString());
            return local.getTime();
        } catch (Exception err) {
            err.printStackTrace();
            Common.submitError(err);
        }

        return 0;
    }

    static Forecast getCurrentForecast(double lat, double lon) throws IOException, JSONException {

        System.out.println("LAT" + lat + "  " + lon);

        /** GET JSON WEATHER DATA FOR PROVIDED LAT LON**/

        Forecast forecast = new Forecast();

        URL url = new URL("https://api.darksky.net/forecast/6a8c62b617e1e4c2d919b7d0d915d30d/" + lat + "," + lon);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        JSONObject jsonWeather = new JSONObject(sb.toString());
        System.out.println("JSON WEATHER:" + jsonWeather.toString());


        /** GET CURRENT SUNRISE SUNSET TIMES FOR LAT LON **/

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        String date = simpleDateFormat.format(calendar.getTime());

        //System.out.println("DATE: " + date);

        url = new URL("https://api.sunrise-sunset.org/json?lat=" + lat + "&lng=" + lon + "&date=" + date + "&formatted=0");
        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);
        urlConnection.connect();

        br = new BufferedReader(new InputStreamReader(url.openStream()));

        sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        JSONObject sunriseSunsetJson = new JSONObject(sb.toString());



        /** EXTRACT JSON INFORMATION **/
      //  int temperature = getTemperature(jsonWeather);

        WeatherObject weatherObject = new WeatherObject();
        weatherObject.setSunrise(getUTCToLocal(sunriseSunsetJson.getJSONObject("results").get("sunrise").toString()));
        weatherObject.setSunset(getUTCToLocal(sunriseSunsetJson.getJSONObject("results").get("sunset").toString()));

        System.out.println("IS DARK: " + weatherObject.isDark());

        JSONObject currently = jsonWeather.getJSONObject("currently");
        JSONArray daily = jsonWeather.getJSONObject("daily").getJSONArray("data");

        if(jsonWeather.has("alerts")) {
            JSONArray alerts = jsonWeather.getJSONArray("alerts");
            weatherObject.setAlerts(alerts.toString());
        }

        weatherObject.setDescription(daily.getJSONObject(0).get("summary").toString() + " " + jsonWeather.getJSONObject("daily").get("summary").toString());
        weatherObject.setHigh((int)Float.parseFloat(daily.getJSONObject(0).get("temperatureMax").toString()));
        weatherObject.setLow((int)Float.parseFloat(daily.getJSONObject(0).get("temperatureMin").toString()));
        weatherObject.setDate(getUTCToLocal(Long.parseLong(currently.get("time").toString())));
        weatherObject.setTemperature((int)Float.parseFloat(currently.get("temperature").toString()));
        weatherObject.setUv(Integer.parseInt(currently.get("uvIndex").toString()));
        weatherObject.setWindSpeed((int)Float.parseFloat(currently.get("windSpeed").toString()));
        weatherObject.setPressure((int)Float.parseFloat(currently.get("pressure").toString()));
        weatherObject.setHumidity((int)(100 * Float.parseFloat(currently.get("humidity").toString())));
        weatherObject.setCloudCover(Float.parseFloat(currently.get("cloudCover").toString()));
        weatherObject.setWeatherIconString(currently.get("icon").toString());
        //weatherObject.setWeatherID(Integer.parseInt(jsonWeather.getJSONArray("weather").getJSONObject(0).get("id").toString()));
        weatherObject.setCondition(currently.get("summary").toString());
        weatherObject.setPrecipProbability(Float.parseFloat(currently.get("precipProbability").toString()));
        forecast.setCurrentConditions(weatherObject);

        System.out.println("WEATHER DATA: " + weatherObject.getHigh(true) + "  " + weatherObject.getTemperature() + "  " + new Date(weatherObject.getDate()));

        //HOURLY
        forecast.setRawHourlyJSON(jsonWeather.getJSONObject("hourly").toString());

        //DAILY
        forecast.setRawDailyJSON(jsonWeather.getJSONObject("daily").toString());

        return forecast;
    }
}
