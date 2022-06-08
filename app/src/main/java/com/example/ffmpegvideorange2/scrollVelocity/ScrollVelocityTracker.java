package com.example.ffmpegvideorange2.scrollVelocity;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ffmpegvideorange2.DisplayUtils;


/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

/**
 * 滚动速度监听
 */
public class ScrollVelocityTracker implements VelocityHandler {

    private static final int INVALID = Integer.MIN_VALUE;
    private static final int SAMPLE_COUNT = 2;

    /**
     * 当前采样次数。采样若干次后计算速度
     */
    private int mCount = 0;
    /**
     * 累计位移像素值
     */
    private int mDiff = 0;
    /**
     * 上次时间戳
     */
    private long mLastTs = 0;
    /**
     * 上次像素值
     */
    private int mLastPx = INVALID;
    /**
     * 速度
     */
    private int mVelocity = 0;
    /**
     * 速度增加超过此值时，变为快速滚动
     */
    private int mUpThreshold = INVALID;
    /**
     * 速度减小小于此值时，变为慢速滚动
     */
    private int mDownThreshold = INVALID;
    /**
     * 是否为快速滚动状态
     */
    private boolean mScrollFast = false;

    @NonNull
    private VelocityTrackListener mListener = VelocityTrackListener.EMPTY_LISTENER;

    public ScrollVelocityTracker(Context context) {
        setThresholdInDp(context, DEFAULT_UP_THRESHOLD, DEFAULT_DOWN_THRESHOLD);
    }

    @Override
    public void setThresholdInDp(Context context, int upThreshold, int downThreshold) {
        if (upThreshold < downThreshold || downThreshold <= 0) {
            throw new IllegalArgumentException("应该确保upThreshold >= downThreshold > 0");
        }
        setThreshold(DisplayUtils.dp2px(context, upThreshold), DisplayUtils.dp2px(context, downThreshold));
    }

    @Override
    public void setThreshold(int upThreshold, int downThreshold) {
        if (upThreshold < downThreshold || downThreshold <= 0) {
            throw new IllegalArgumentException("应该确保upThreshold >= downThreshold > 0");
        }
        mUpThreshold = upThreshold;
        mDownThreshold = downThreshold;
        Log.e("kzg","********************setThreshold   mUpThreshold:"+mUpThreshold + "  , mDownThreshold:"+mDownThreshold);
    }

    @Override
    public void setVelocityTrackerListener(VelocityTrackListener listener) {
        mListener = listener == null ? VelocityTrackListener.EMPTY_LISTENER : listener;
    }

    public void reset() {
        mCount = 0;
        mDiff = 0;
        mLastPx = INVALID;
        mLastTs = 0;
        setVelocity(0);
    }

    public final void clearCount() {
        mDiff = 0;
        mCount = 0;
        mLastTs = timestamp();
    }

    public final void onScrollTo(final int pixels) {
        onScrollBy(mLastPx == INVALID ? 0 : pixels - mLastPx);
        mLastPx = pixels;
    }

    /**
     * @param diff 滚动像素值
     */
    public final void onScrollBy(final int diff) {
        mDiff += diff;
        if (++mCount >= SAMPLE_COUNT) {
            final long ts = timestamp();
            if (mDiff != 0 && mLastTs > 0 && ts > mLastTs) {
                final long ms = ts - mLastTs;
                final int v = (int) (mDiff * 1000 / ms);

                setVelocity(v);
            }
            mDiff = 0;
            mCount = 0;
            mLastTs = ts;
        }
    }

    private long timestamp() {
        return SystemClock.uptimeMillis();
    }

    private void setVelocity(int velocity) {
        if (velocity != mVelocity) {
            mVelocity = velocity;
            mListener.onVelocityChanged(mVelocity);
            if (mUpThreshold != INVALID && mDownThreshold != INVALID) {
                if (mScrollFast) {
                    if (Math.abs(mVelocity) < mDownThreshold) {
                        mScrollFast = false;
                        mListener.onScrollSlow(mVelocity);
                    }
                } else {
                    if (Math.abs(mVelocity) > mUpThreshold) {
                        mScrollFast = true;
                        mListener.onScrollFast(mVelocity);
                    }
                }
            }
        }
    }
}
