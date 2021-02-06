package com.illusory.fileexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

public class CircleCutLayout extends LinearLayout {

    public CircleCutLayout(Context context) {
        super(context);
    }

    public CircleCutLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleCutLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CircleCutLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);  // low 16bits
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int w_mode = MeasureSpec.getMode(widthMeasureSpec);  // high 16bits MeasureSpec.EXACTLY MeasureSpec.AT_MOST MeasureSpec.UNSPECIFIED
        int h_mode = MeasureSpec.getMode(heightMeasureSpec);
        Log.d("CircleCutLayout", "onMeasure " + w + " " + h);
        int radius = Math.min(w, h);
        int side = (int) (radius * Math.cos(Math.toRadians(45))) - 4;
        int w_meaSpec = MeasureSpec.makeMeasureSpec(side, w_mode);
        int h_meaSpec = MeasureSpec.makeMeasureSpec(side, h_mode);

        super.onMeasure(w_meaSpec, h_meaSpec);
    }
}
