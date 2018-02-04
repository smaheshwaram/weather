package com.ndcubed.weather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by Nathan on 5/21/2017.
 */

public class AnimatedWeatherContainer extends RelativeLayout {

    static final int COLOR_DAY = Color.rgb(123, 154, 198);
    static final int COLOR_NIGHT = Color.rgb(88, 102, 122);
    private int color = COLOR_DAY;

    private WeatherObject weatherObject;

    public AnimatedWeatherContainer(Context context) {
        super(context);
        init();
    }

    public AnimatedWeatherContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedWeatherContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AnimatedWeatherContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    public WeatherObject getWeatherObject() {
        return weatherObject;
    }

    public void setWeatherObject(WeatherObject weatherObject) {
        this.weatherObject = weatherObject;
        color = weatherObject.isDark() ? COLOR_NIGHT : COLOR_DAY;
        ((ImageView)findViewById(R.id.weatherIcon)).setImageResource(weatherObject.getWeatherIconResourceID(WeatherObject.RESOLUTION_LARGE));

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0, 0, getWidth() / 2, getHeight(), paint);
    }
}
