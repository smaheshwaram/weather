package com.ndcubed.weather;

import android.content.Context;

/**
 * Created by Nathan on 6/26/2017.
 */

public class WidgetStateManger {

    private Context context;
    private int widgetID;

    public WidgetStateManger(Context context, int widgetID) {
        this.context = context;
        this.widgetID = widgetID;
    }
}
