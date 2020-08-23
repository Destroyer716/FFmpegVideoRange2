package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created By Ele
 * on 2020/8/17
 **/
public class VideoRangeRecyclerView extends RecyclerView {
    //是否拦截触摸事件
    boolean isIntercept = true;


    public VideoRangeRecyclerView(@NonNull Context context) {
        super(context);
    }

    public VideoRangeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoRangeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void setIntercept(boolean intercept) {
        isIntercept = intercept;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = true;
                break;
        }

        /*VideoRangeView.isIntercept = !isIntercept;
        if (!isIntercept){
            return isIntercept;
        }*/
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = true;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        if (!isIntercept){
            return false;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return super.onInterceptTouchEvent(e);
    }

}
