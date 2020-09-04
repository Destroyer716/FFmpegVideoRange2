package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * Created By Ele
 * on 2020/8/26
 **/
public class VideoRangeHorizontalScrollView extends HorizontalScrollView {

    private OnScrollChangedListener onScrollChangedListener;
    //是否拦截触摸事件
    boolean isIntercept = true;

    public VideoRangeHorizontalScrollView(Context context) {
        super(context);
    }

    public VideoRangeHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoRangeHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollChangedListener != null){
            onScrollChangedListener.onScrollChanged(l,t,oldl,oldt);
        }
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

        VideoRangeView.isIntercept = !isIntercept;
        if (!isIntercept){
            return isIntercept;
        }
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




    public OnScrollChangedListener getOnScrollChangedListener() {
        return onScrollChangedListener;
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }

    public interface OnScrollChangedListener{
        void onScrollChanged(int x, int y, int oldx, int oldy);
    }
    public void setIntercept(boolean intercept) {
        isIntercept = intercept;
    }
}
