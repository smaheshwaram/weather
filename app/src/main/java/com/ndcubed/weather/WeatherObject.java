package com.ndcubed.weather;

import android.text.Html;
import android.text.Spanned;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Nathan on 5/10/2017.
 */

public class WeatherObject {

    private int temperature, high, low, humidity;
    private long sunrise, sunset;
    private int weatherID = 800;
    private String condition = "Clear";
    private long date = 0;
    private boolean isForecast = false;
    private String weatherIconString = "clear-day";
    private String alerts = "";
    private String description = "";
    private boolean useCurrentSunriseSunset = true;
    private boolean useDayIconsOnly = false;
    private boolean useNightIconsOnly = false;
    private float precipProbability = 0.0f;
    private float cloudCover = 0.0f;
    private int windSpeed = 0;
    private int uv = 0;
    private int pressure = 0;

    static final int RESOLUTION_SMALL = 1;
    static final int RESOLUTION_LARGE = 2;

    WeatherObject(long date, int temperature, long sunrise, long sunset, String weatherIconString, String condition) {

        this.date = date;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.temperature = temperature;
        this.weatherIconString = weatherIconString;
        this.condition = condition;

        if(condition.equals("Breezy and Partly Cloudy")) {
            this.condition = "Partly Cloudy";
        }
    }

    WeatherObject() {}

    public void setUseDayIconsOnly(boolean useDayIconsOnly) {
        this.useDayIconsOnly = useDayIconsOnly;
        if(useDayIconsOnly) useNightIconsOnly = false;
    }

    public void setUseNightIconsOnly(boolean useNightIconsOnly) {
        this.useNightIconsOnly = useNightIconsOnly;
        if(useNightIconsOnly) useNightIconsOnly = false;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getUv() {
        return uv;
    }

    public void setUv(int uv) {
        this.uv = uv;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(int windSpeed) {
        this.windSpeed = windSpeed;
    }

    float getPrecipProbabilityFloat() {
        return precipProbability;
    }

    int getPrecipProbability() {
        return (int)(precipProbability * 100f);
    }

    void setPrecipProbability(float precipProbability) {
        this.precipProbability = precipProbability;
    }

    int getCloudCover() {
        return (int)(cloudCover * 100f);
    }

    float getCloudCoverFloat() {
        return cloudCover;
    }

    void setCloudCover(float cloudCover) {
        this.cloudCover = cloudCover;
    }

    String getAlerts() {
        return alerts;
    }

    void setAlerts(String alerts) {
        this.alerts = alerts;
    }

    String getCondition() {
        return condition;
    }

    boolean isForecast() {
        return isForecast;
    }

    void setForecast(boolean forecast) {
        isForecast = forecast;
    }

    String getWeatherIconString() {
        return weatherIconString;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setWeatherIconString(String weatherIconString) {
        this.weatherIconString = weatherIconString;

        switch(weatherIconString) {
            case "wind":
                if(getCloudCover() > 20) {
                    this.weatherIconString = "partly-cloudy-day";
                } else {
                    this.weatherIconString = "clear-day";
                }
                break;
        }
    }

    void setIsForecast(boolean forecast) {
        isForecast = forecast;
    }

    long getCurrentTime() {
        return System.currentTimeMillis();
    }

    void setCondition(String condition) {
        this.condition = condition;

        if(condition.equals("Breezy and Partly Cloudy")) {
            this.condition = "Partly Cloudy";
        }
    }

    void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    @SuppressWarnings("deprecation")
    String getTemperatureString(boolean useFahrenheit) {
        return getTemperature(useFahrenheit) + "°";

        /*
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {

            if(useFahrenheit) {
                return Html.fromHtml(getTemperature(useFahrenheit) + "°", Html.FROM_HTML_MODE_LEGACY);
            } else {
                return Html.fromHtml(getTemperature(useFahrenheit) + "<sup><sup><sup><small><small><small><small>C</small></small></small></small><sup></sup></sup>", Html.FROM_HTML_MODE_LEGACY);
            }
        } else {
            if(useFahrenheit) {
                return Html.fromHtml(getTemperature(useFahrenheit) + "°");
            } else {
                return Html.fromHtml(getTemperature(useFahrenheit) + "<sup><sup><sup><small><small><small><small>C</small></small></small></small><sup></sup></sup>");
            }
        }
        */
    }

    long getDate() {
        return date;
    }

    void setDate(long date) {
        this.date = date;
    }

    void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    void setSunset(long sunset) {
        this.sunset = sunset;
    }

    void setWeatherID(int weatherID) {
        this.weatherID = weatherID;
    }

    int getWeatherID() {
        return weatherID;
    }

    long getSunset() {
        return sunset;
    }

    long getSunrise() {
        return sunrise;
    }

    int getTemperature() {
        return temperature;
    }

    int getTemperature(boolean useFahrenheit) {
        return useFahrenheit ? temperature : getCelsius(temperature);
    }

    int getHigh(boolean useFahrenheit) {
        return useFahrenheit ? high : getCelsius(high);
    }

    void setHigh(int high) {
        this.high = high;
    }

    int getLow(boolean useFahrenheit) {
        return useFahrenheit ? low : getCelsius(low);
    }

    void setLow(int low) {
        this.low = low;
    }

    int getHumidity() {
        return humidity;
    }

    void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public boolean useCurrentSunriseSunset() {
        return useCurrentSunriseSunset;
    }

    public void setUseCurrentSunriseSunset(boolean useCurrentSunriseSunset) {
        this.useCurrentSunriseSunset = useCurrentSunriseSunset;
    }

    private int getCelsius(int fahrenheit) {
        return (int)((fahrenheit - 32) / 1.8f);
    }

    String getHourString() {

        SimpleDateFormat sdf = new SimpleDateFormat("h a");

        return sdf.format(new Date(getDate()));
    }

    String getWeekdayString() {

        SimpleDateFormat sdf = new SimpleDateFormat("EEE");

        return sdf.format(new Date(getDate()));
    }

    boolean isDark() {

        System.out.println("IS DARK: " + new Date(getDate()) + "   " + new Date(getSunset()) + "   " + new Date(getSunrise()));

        if(!useNightIconsOnly && !useDayIconsOnly) {
            if(isForecast() && !useCurrentSunriseSunset()) {

                long daysAhead = Common.getNumDaysAhead(getSunset(), getDate());
                long newSunset = getSunset() + (daysAhead * 86400000L);
                long newSunrise = getSunrise() + (daysAhead * 86400000L);

                System.out.println("IS NIGHT: " + new Date(getDate()) + "    " + (getDate() > newSunset && getDate() > newSunrise || getDate() < newSunrise && getDate() < newSunset));

                return getDate() > newSunset && getDate() > newSunrise || getDate() < newSunrise && getDate() < newSunset;
            } else {

                return getCurrentTime() > getSunset() && getCurrentTime() > getSunrise() || getCurrentTime() < getSunrise() && getCurrentTime() < getSunset();
            }
        } else {
            return useNightIconsOnly;
        }
    }

    int getWeatherIconResourceID(int resolution, boolean overrideSunsetSunrise) {

        boolean dark = overrideSunsetSunrise ? false : isDark();

        if(resolution == RESOLUTION_SMALL) {
            if(getWeatherID() >= 200 && weatherID <= 232) {
                /** THUNDERSTORM **/
                return dark ? R.drawable.drizzle_night : R.drawable.drizzle;
            } else if(getWeatherIconString().equals("rain")) {
                /** RAIN **/

                System.out.println("CLOUD RAIN: " + getPrecipProbability() + "   " + getCloudCover());
                //CHECK IF PARTLY CLOUDY RAIN
                if(getPrecipProbability() < 45 || getCloudCover() < 45) {
                    return dark ? R.drawable.simple_rain_moon_night : R.drawable.simple_rain_sun;
                } else {
                    return dark ? R.drawable.simple_rain_night : R.drawable.simple_rain;
                }
            } else if(getWeatherID() >= 600 && getWeatherID() <= 622) {
                /** SNOW **/
                return dark ? R.drawable.simple_snow : R.drawable.simple_snow;
            } else if(getWeatherIconString().equals("fog")) {
                /** FOG **/
                return dark ? R.drawable.haze_night : R.drawable.haze_day;
            } else if(getWeatherIconString().equals("clear-day") || getWeatherIconString().equals("clear-night")) {
                /** CLEAR **/
                return dark ? R.drawable.simple_clear_night : R.drawable.simple_sunny;

            } else if((getWeatherIconString().equals("partly-cloudy-day") || getWeatherIconString().equals("partly-cloudy-night")) && !getCondition().equals("Mostly Cloudy")) {
                /** SOME CLOUDS **/
                return dark ? R.drawable.simple_partly_cloudy_night : R.drawable.simple_partly_cloudy;

            } else if(getCondition().equals("Mostly Cloudy")) {
                /** MOSTLY CLOUDS **/
                return dark ? R.drawable.simple_mostly_cloudy_night : R.drawable.simple_mostly_cloudy;
            } else if(getWeatherIconString().equals("cloudy")) {
                /** OVERCAST **/
                return dark ? R.drawable.simple_overcast_night : R.drawable.simple_overcast;
            } else if(getWeatherIconString().equals("wind")) {
                return dark ? R.drawable.simple_clear_night : R.drawable.simple_sunny;
            }
        } else if(resolution == RESOLUTION_LARGE) {
            if(getWeatherID() >= 200 && weatherID <= 232) {
                /** THUNDERSTORM **/
                return dark ? R.drawable.drizzle_night_large : R.drawable.drizzle_large;
            } else if(getWeatherIconString().equals("rain")) {
                /** RAIN **/
                //CHECK IF PARTLY CLOUDY RAIN
                if(getPrecipProbability() < 45 || getCloudCover() < 45) {
                    return dark ? R.drawable.simple_rain_moon_night_large : R.drawable.simple_rain_sun_large;
                } else {
                    return dark ? R.drawable.simple_rain_night_large : R.drawable.simple_rain_large;
                }
            } else if(getWeatherID() >= 600 && getWeatherID() <= 622) {
                /** SNOW **/
                return dark ? R.drawable.snow_night_large : R.drawable.simple_snow_large;
            } else if(getWeatherIconString().equals("fog")) {
                /** FOG **/
                return dark ? R.drawable.haze_night : R.drawable.haze_day;
            } else if(getWeatherIconString().equals("clear-day") || getWeatherIconString().equals("clear-night")) {
                /** CLEAR **/
                return dark ? R.drawable.simple_clear_night_large : R.drawable.simple_sunny_large;

            } else if((getWeatherIconString().equals("partly-cloudy-day") || getWeatherIconString().equals("partly-cloudy-night")) && !getCondition().equals("Mostly Cloudy")) {
                /** SOME CLOUDS **/
                return dark ? R.drawable.simple_partly_cloudy_night_large : R.drawable.simple_partly_cloudy_large;

            } else if(getCondition().equals("Mostly Cloudy")) {
                /** MOSTLY CLOUDS **/
                return dark ? R.drawable.simple_mostly_cloudy_night_large : R.drawable.simple_mostly_cloudy_large;
            } else if(getWeatherIconString().equals("cloudy")) {
                /** OVERCAST **/
                return dark ? R.drawable.simple_overcast_night_large : R.drawable.simple_overcast_large;
            } else if(getWeatherIconString().equals("wind")) {
                return dark ? R.drawable.simple_clear_night_large : R.drawable.simple_sunny_large;
            }
        }

        return dark ? R.drawable.simple_clear_night : R.drawable.simple_sunny;
    }

    int getWeatherIconResourceID(int resolution) {

        if(resolution == RESOLUTION_SMALL) {
            if(getWeatherID() >= 200 && weatherID <= 232) {
                /** THUNDERSTORM **/
                return isDark() ? R.drawable.drizzle_night : R.drawable.drizzle;
            } else if(getWeatherIconString().equals("rain")) {
                /** RAIN **/
                return isDark() ? R.drawable.drizzle_night : R.drawable.drizzle;
            } else if(getWeatherID() >= 600 && getWeatherID() <= 622) {
                /** SNOW **/
                return isDark() ? R.drawable.snow_night : R.drawable.snow;
            } else if(getWeatherIconString().equals("fog")) {
                /** FOG **/
                return isDark() ? R.drawable.haze_night : R.drawable.haze_day;
            } else if(getWeatherIconString().equals("clear-day") || getWeatherIconString().equals("clear-night")) {
                /** CLEAR **/
                return isDark() ? R.drawable.clear_night : R.drawable.sunny;

            } else if((getWeatherIconString().equals("partly-cloudy-day") || getWeatherIconString().equals("partly-cloudy-night")) && !getCondition().equals("Mostly Cloudy")) {
                /** SOME CLOUDS **/
                return isDark() ? R.drawable.partly_cloudy_night : R.drawable.partly_cloudy;

            } else if(getCondition().equals("Mostly Cloudy")) {
                /** MOSTLY CLOUDS **/
                return isDark() ? R.drawable.simple_mostly_cloudy_night : R.drawable.simple_mostly_cloudy;
            } else if(getWeatherIconString().equals("cloudy")) {
                /** OVERCAST **/
                return isDark() ? R.drawable.simple_overcast_night : R.drawable.simple_overcast;
            } else if(getWeatherIconString().equals("wind")) {
                return isDark() ? R.drawable.wind_night : R.drawable.wind;
            }
        } else if(resolution == RESOLUTION_LARGE) {
            if(getWeatherID() >= 200 && weatherID <= 232) {
                /** THUNDERSTORM **/
                return isDark() ? R.drawable.drizzle_night_large : R.drawable.drizzle_large;
            } else if(getWeatherIconString().equals("rain")) {
                /** RAIN **/
                return isDark() ? R.drawable.drizzle_night_large : R.drawable.drizzle_large;
            } else if(getWeatherID() >= 600 && getWeatherID() <= 622) {
                /** SNOW **/
                return isDark() ? R.drawable.snow_night_large : R.drawable.snow_large;
            } else if(getWeatherIconString().equals("fog")) {
                /** FOG **/
                return isDark() ? R.drawable.haze_night : R.drawable.haze_day;
            } else if(getWeatherIconString().equals("clear-day") || getWeatherIconString().equals("clear-night")) {
                /** CLEAR **/
                return isDark() ? R.drawable.clear_night_large : R.drawable.sunny_large;

            } else if((getWeatherIconString().equals("partly-cloudy-day") || getWeatherIconString().equals("partly-cloudy-night")) && !getCondition().equals("Mostly Cloudy")) {
                /** SOME CLOUDS **/
                return isDark() ? R.drawable.partly_cloudy_night_large : R.drawable.partly_cloudy_large;

            } else if(getCondition().equals("Mostly Cloudy")) {
                /** MOSTLY CLOUDS **/
                return isDark() ? R.drawable.mostly_cloudy_night_large : R.drawable.mostly_cloudy_large;
            } else if(getWeatherIconString().equals("cloudy")) {
                /** OVERCAST **/
                return isDark() ? R.drawable.overcast_night_large : R.drawable.overcast_large;
            } else if(getWeatherIconString().equals("wind")) {
                return isDark() ? R.drawable.wind_night_large : R.drawable.wind_large;
            }
        }

        return isDark() ? R.drawable.clear_night : R.drawable.sunny;
    }
}
