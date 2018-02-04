package com.ndcubed.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Created by Nathan on 6/27/2017.
 */

public class WidgetState {

    private int widgetID, displayIndex, widgetTransparency, widgetTextColor, tutorialIndex;
    private Context context;
    private boolean isFahrenheit, alwaysDayIcons, alwaysDayWeekIcons, autoRefresh, weatherNotifications, useCurrentLocation, wasConfigured, didTutorial;
    private double lat, lon;
    private long lastUpdate;

    public WidgetState(Context context, int widgetID) {
        this.context = context;
        this.widgetID = widgetID;

        SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES + widgetID, Context.MODE_PRIVATE);

        displayIndex = prefs.getInt("displayIndex", 0);
        isFahrenheit = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, true);
        alwaysDayIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, false);
        alwaysDayWeekIcons = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, false);
        autoRefresh = prefs.getBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, true);
        weatherNotifications = prefs.getBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, false);
        useCurrentLocation = prefs.getBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, false);
        wasConfigured = prefs.getBoolean(Common.PreferenceKeys.KEY_WAS_CONFIGURED, false);
        lat = Double.parseDouble(prefs.getString(Common.PreferenceKeys.KEY_LAT, "0"));
        lon = Double.parseDouble(prefs.getString(Common.PreferenceKeys.KEY_LON, "0"));
        lastUpdate = prefs.getLong(Common.PreferenceKeys.KEY_LAST_UPDATE, 0);
        widgetTransparency = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TRANSPARENCY, 255);
        widgetTextColor = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR, Color.argb(242, 84, 93, 112));
        tutorialIndex = prefs.getInt(Common.PreferenceKeys.KEY_WIDGET_TUTORIAL_INDEX, 0);
        didTutorial = prefs.getBoolean(Common.PreferenceKeys.KEY_WIDGET_DID_TUTORIAL, false);
    }

    public void saveWidgetState() {
        SharedPreferences prefs = context.getSharedPreferences(Common.PreferenceKeys.KEY_WEATHER_PREFERENCES + widgetID, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();

        e.putInt("displayIndex", displayIndex);
        e.putBoolean(Common.PreferenceKeys.KEY_USE_DAY_WEEK_ICONS, alwaysDayWeekIcons);
        e.putBoolean(Common.PreferenceKeys.KEY_USE_ONLY_DAY_ICONS, alwaysDayIcons);
        e.putBoolean(Common.PreferenceKeys.KEY_USE_CURRENT_LOCATION, useCurrentLocation);
        e.putBoolean(Common.PreferenceKeys.KEY_USE_FAHRENHEIT, isFahrenheit);
        e.putBoolean(Common.PreferenceKeys.KEY_AUTO_UPDATE, autoRefresh);
        e.putBoolean(Common.PreferenceKeys.KEY_WEATHER_NOTIFICATIONS_ENABLED, weatherNotifications);
        e.putBoolean(Common.PreferenceKeys.KEY_WAS_CONFIGURED, wasConfigured);
        e.putString(Common.PreferenceKeys.KEY_LAT, Double.toString(lat));
        e.putString(Common.PreferenceKeys.KEY_LON, Double.toString(lon));
        e.putLong(Common.PreferenceKeys.KEY_LAST_UPDATE, lastUpdate);
        e.putInt(Common.PreferenceKeys.KEY_WIDGET_TRANSPARENCY, widgetTransparency);
        e.putInt(Common.PreferenceKeys.KEY_WIDGET_TEXT_COLOR, widgetTextColor);
        e.putInt(Common.PreferenceKeys.KEY_WIDGET_TUTORIAL_INDEX, tutorialIndex);
        e.putBoolean(Common.PreferenceKeys.KEY_WIDGET_DID_TUTORIAL, didTutorial);

        e.apply();
    }

    public boolean didTutorial() {
        return didTutorial;
    }

    public void setDidTutorial(boolean didTutorial) {
        this.didTutorial = didTutorial;
    }

    public int getTutorialIndex() {
        return tutorialIndex;
    }

    public void setTutorialIndex(int tutorialIndex) {
        this.tutorialIndex = tutorialIndex;
    }

    public int getWidgetTextColor() {
        return widgetTextColor;
    }

    public void setWidgetTextColor(int widgetTextColor) {
        this.widgetTextColor = widgetTextColor;
    }

    public int getWidgetTransparency() {
        return widgetTransparency;
    }

    public void setWidgetTransparency(int widgetTransparency) {
        this.widgetTransparency = widgetTransparency;
    }

    public boolean wasConfigured() {
        return wasConfigured;
    }

    public void setWasConfigured(boolean wasConfigured) {
        this.wasConfigured = wasConfigured;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Context getContext() {
        return context;
    }

    public int getWidgetID() {
        return widgetID;
    }

    public int getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(int displayIndex) {
        this.displayIndex = displayIndex;
    }

    public boolean isFahrenheit() {
        return isFahrenheit;
    }

    public void setFahrenheit(boolean fahrenheit) {
        isFahrenheit = fahrenheit;
    }

    public boolean isAlwaysDayIcons() {
        return alwaysDayIcons;
    }

    public void setAlwaysDayIcons(boolean alwaysDayIcons) {
        this.alwaysDayIcons = alwaysDayIcons;
    }

    public boolean isAlwaysDayWeekIcons() {
        return alwaysDayWeekIcons;
    }

    public void setAlwaysDayWeekIcons(boolean alwaysDayWeekIcons) {
        this.alwaysDayWeekIcons = alwaysDayWeekIcons;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public boolean isWeatherNotificationsEnabled() {
        return weatherNotifications;
    }

    public void setWeatherNotificationsEnabled(boolean weatherNotifications) {
        this.weatherNotifications = weatherNotifications;
    }

    public boolean useCurrentLocation() {
        return useCurrentLocation;
    }

    public void setUseCurrentLocation(boolean useCurrentLocation) {
        this.useCurrentLocation = useCurrentLocation;
    }
}
