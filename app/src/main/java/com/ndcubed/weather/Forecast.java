package com.ndcubed.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import static com.ndcubed.weather.WeatherUtils.getUTCToLocal;

/**
 * Created by Nathan on 5/13/2017.
 */

class Forecast {

    private String rawHourlyJSON = "";
    private String rawDailyJSON = "";
    private ArrayList<WeatherObject> hourlyForecast = new ArrayList<>();
    private ArrayList<WeatherObject> dailyForecast = new ArrayList<>();

    private WeatherObject currentConditions;

    Forecast() {}

    Forecast(WeatherObject currentConditions) {
        this.currentConditions = currentConditions;
    }

    void addHourlyConditions(WeatherObject conditions) {
        hourlyForecast.add(conditions);
    }

    void addDailyConditions(WeatherObject conditions) {
        dailyForecast.add(conditions);
    }

    void clear() {
        hourlyForecast.clear();
        dailyForecast.clear();
    }

    ArrayList<WeatherObject> getHourlyForecast() {
        return hourlyForecast;
    }

    ArrayList<WeatherObject> getDailyForecast() {

        if(dailyForecast.size() > 0) {
            /** CHECK IF PARTLY CLOUDY RAIN SHOULD APPLY**/
            WeatherObject today = dailyForecast.get(0);
            if(today.getWeatherIconString().equals("rain")) {
                if(currentConditions.getCloudCover() < 30 || currentConditions.getPrecipProbability() < 30) {
                    today.setCloudCover(currentConditions.getCloudCoverFloat());
                    today.setPrecipProbability(currentConditions.getPrecipProbabilityFloat());

                    System.out.println(currentConditions.getPrecipProbabilityFloat() + "   CCC " + currentConditions.getCloudCoverFloat());
                }
            }
        }

        return dailyForecast;
    }

    WeatherObject getCurrentConditions() {
        return currentConditions;
    }

    void setCurrentConditions(WeatherObject currentConditions) {
        this.currentConditions = currentConditions;
    }

    String getRawHourlyJSON() {
        return rawHourlyJSON;
    }

    void setRawHourlyJSON(String rawHourlyJSON) {
        this.rawHourlyJSON = rawHourlyJSON;

        if(!rawHourlyJSON.equals("")) {

            try {
                JSONObject jsonWeather = new JSONObject(rawHourlyJSON);
                JSONArray hourly = jsonWeather.getJSONArray("data");

                for(int i = 0; i < hourly.length() && i < 5; i++) {

                    JSONObject json = hourly.getJSONObject(i);

                    int temp = (int)Float.parseFloat(json.get("temperature").toString());
                    float precip = Float.parseFloat(json.get("precipProbability").toString());
                    float cloudCover = Float.parseFloat(json.get("cloudCover").toString());
                    int humidity = (int)(Float.parseFloat(json.get("humidity").toString()) * 100f);
                    long dateLong = getUTCToLocal(Long.parseLong(json.get("time").toString()));
                    String weatherIconString = json.get("icon").toString();
                    String condition = json.get("summary").toString();

                    WeatherObject weatherObject = new WeatherObject();

                    weatherObject.setDate(dateLong);
                    weatherObject.setCloudCover(cloudCover);
                    weatherObject.setWeatherIconString(weatherIconString);
                    weatherObject.setTemperature(temp);
                    weatherObject.setHumidity(humidity);
                    weatherObject.setCondition(condition);
                    weatherObject.setSunrise(getCurrentConditions().getSunrise());
                    weatherObject.setSunset(getCurrentConditions().getSunset());
                    weatherObject.setIsForecast(true);
                    weatherObject.setUseCurrentSunriseSunset(false);
                    weatherObject.setPrecipProbability(precip);

                    addHourlyConditions(weatherObject);
                }
            } catch (Exception err) {
                err.printStackTrace();
                Common.submitError(err);
            }
        }
    }

    String getRawDailyJSON() {
        return rawDailyJSON;
    }

    void setRawDailyJSON(String rawDailyJSON) {
        this.rawDailyJSON = rawDailyJSON;

        if(!rawDailyJSON.equals("")) {

            try {
                JSONObject jsonWeather = new JSONObject(rawDailyJSON);
                JSONArray daily = jsonWeather.getJSONArray("data");

                for(int i = 0; i < daily.length() && i < 5; i++) {

                    JSONObject json = daily.getJSONObject(i);

                    //System.out.println(weatherMeasurements.get("dt").toString());
                    int tempHigh = (int)Float.parseFloat(json.get("temperatureMax").toString());
                    int tempLow = (int)Float.parseFloat(json.get("temperatureMin").toString());
                    int humidity = (int)(Float.parseFloat(json.get("humidity").toString()) * 100f);
                    float cloudCover = Float.parseFloat(json.get("cloudCover").toString());
                    float precip = Float.parseFloat(json.get("precipProbability").toString());
                    long dateLong = getUTCToLocal(Long.parseLong(json.get("time").toString()));
                    String weatherIconString = json.get("icon").toString();
                    String condition = json.get("summary").toString();

                    WeatherObject weatherObject = new WeatherObject();

                    weatherObject.setDate(dateLong);
                    weatherObject.setCloudCover(cloudCover);
                    weatherObject.setWeatherIconString(weatherIconString);
                    weatherObject.setHigh(tempHigh);
                    weatherObject.setLow(tempLow);
                    weatherObject.setHumidity(humidity);
                    weatherObject.setCondition(condition);
                    weatherObject.setSunrise(getCurrentConditions().getSunrise());
                    weatherObject.setSunset(getCurrentConditions().getSunset());
                    weatherObject.setIsForecast(true);
                    weatherObject.setUseCurrentSunriseSunset(true);
                    weatherObject.setPrecipProbability(precip);

                    addDailyConditions(weatherObject);

                    System.out.println("DAILY: " + weatherObject.getTemperature() + "  " + new Date(weatherObject.getDate()) + "   " + weatherObject.getWeatherIconString());

                }
            } catch (Exception err) {
                err.printStackTrace();
                Common.submitError(err);
            }
        }
    }
}
