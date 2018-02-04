package com.ndcubed.weather;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Nathan on 5/25/2017.
 */

public class FixedAspectImageView extends ImageView {

    public FixedAspectImageView(Context context) {
        super(context);
    }

    public FixedAspectImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedAspectImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FixedAspectImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.max(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(size, size);
    }
}
