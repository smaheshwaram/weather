package com.ndcubed.weather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * Created by Nathan on 5/21/2017.
 */

public class WeatherScrollView extends ScrollView {

    boolean isScrolling = false;

    MotionEvent downEvent;

    private GestureDetector gestureDetector;

    public WeatherScrollView(Context context) {
        super(context);
        init(context);
    }

    public WeatherScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeatherScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public WeatherScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {

        setFadingEdgeLength(0);
        gestureDetector = new GestureDetector(context, new VerticalScrollListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) {
            super.onTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    private class VerticalScrollListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return Math.abs(distanceY) > Math.abs(distanceX);
        }
    }
}
