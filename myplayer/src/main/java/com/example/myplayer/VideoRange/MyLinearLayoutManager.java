package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * Created By Ele
 * on 2020/8/23
 **/
public class MyLinearLayoutManager extends LinearLayoutManager {

    private int maxWidth = 0;

    public MyLinearLayoutManager(Context context) {
        super(context);
    }

    public MyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public MyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
        if (maxWidth > 0){
            super.setMeasuredDimension(childrenBounds, View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST), hSpec);
        }else {
            super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
        }
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }
}
