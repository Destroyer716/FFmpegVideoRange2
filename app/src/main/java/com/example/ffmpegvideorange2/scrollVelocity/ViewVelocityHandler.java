package com.example.ffmpegvideorange2.scrollVelocity;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.annotation.RequiresApi;


/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class ViewVelocityHandler implements View.OnScrollChangeListener, VelocityHandler {

    private static final int MSG_CHECK_SCROLL = 1;
    private static final int CHECK_SCROLL_STOP_DELAY_MILLIS = 1;

    private final ScrollVelocityTracker mTracker;
    private final WeakRefWrapper<View> mViewRef = new WeakRefWrapper<>();

    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {

        private int INVALID = Integer.MIN_VALUE;
        private int mLastX = INVALID;

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_CHECK_SCROLL) {
                View view = mViewRef.get();
                if (view != null) {
                    // 连续两次检查到scrollY相同，则视为滚动停止
                    final int scrollY = view.getScrollY();
                    if (mLastX == scrollY) {
                        // scroll stopped
                        mLastX = INVALID;
                        mTracker.reset();
                    } else {
                        // scrolling
                        mLastX = scrollY;
                        restartCheckStopTiming();
                    }
                    return true;
                }
            }
            return false;
        }
    });

    public ViewVelocityHandler(Context context) {
        mTracker = new ScrollVelocityTracker(context);
    }

    @Override
    public void setThresholdInDp(Context context, int upThreshold, int downThreshold) {
        mTracker.setThresholdInDp(context, upThreshold, downThreshold);
    }

    @Override
    public void setThreshold(int upThreshold, int downThreshold) {
        mTracker.setThreshold(upThreshold, downThreshold);
    }

    @Override
    public void setVelocityTrackerListener(VelocityTrackListener listener) {
        mTracker.setVelocityTrackerListener(listener);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        mViewRef.set(v);
        mTracker.onScrollBy(scrollX - oldScrollX);
        restartCheckStopTiming();
    }

    private void restartCheckStopTiming() {
        mHandler.removeMessages(MSG_CHECK_SCROLL);
        mHandler.sendEmptyMessageDelayed(MSG_CHECK_SCROLL, CHECK_SCROLL_STOP_DELAY_MILLIS);
    }
}
