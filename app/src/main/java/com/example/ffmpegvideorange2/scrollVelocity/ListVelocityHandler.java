package com.example.ffmpegvideorange2.scrollVelocity;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.AbsListView;

import androidx.annotation.CallSuper;

/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

/**
 * ListView滚动速度监听
 */
public class ListVelocityHandler implements AbsListView.OnScrollListener, VelocityHandler {

    private static final int INVALID = Integer.MIN_VALUE;

    private final ScrollVelocityTracker mTracker;

    private int mPrevFirstItem = INVALID;
    private int mPrevFirstTop = INVALID;
    private int mPrevSecondTop = INVALID;

    public ListVelocityHandler(Context context) {
        mTracker = new ScrollVelocityTracker(context);
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
    public void setThresholdInDp(Context context, int upThreshold, int downThreshold) {
        mTracker.setThresholdInDp(context, upThreshold, downThreshold);
    }

    @CallSuper
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            reset();
        }
    }

    private void reset() {
        mTracker.reset();
        mPrevFirstItem = INVALID;
        mPrevFirstTop = INVALID;
        mPrevSecondTop = INVALID;
    }

    @CallSuper
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        final int firstTop = getChildTop(view, 0);
        final int secondTop = getChildTop(view, 1);

        int diff = INVALID;

        if (mPrevFirstItem != INVALID) {
            if (firstVisibleItem == mPrevFirstItem) { // 同一个Item
                diff = sub(mPrevFirstTop, firstTop);
            } else if (firstVisibleItem == mPrevFirstItem + 1) { // 滚动到下一个Item
                diff = sub(mPrevSecondTop, firstTop);
            } else if (firstVisibleItem == mPrevFirstItem - 1) { // 滚动到上一个Item
                diff = sub(mPrevFirstTop, secondTop);
            }
        }

        if (diff != INVALID) {
            mTracker.onScrollBy(diff);
        } else { // 不处理一帧滚动超过1个Item的情况，直接丢弃数据
            mTracker.clearCount();
        }

        mPrevFirstItem = firstVisibleItem;
        mPrevFirstTop = firstTop;
        mPrevSecondTop = secondTop;
    }

    private int getChildTop(ViewGroup view, int index) {
        return (index >= 0 && index < view.getChildCount()) ? view.getChildAt(index).getTop() : INVALID;
    }

    private int sub(int a, int b) {
        return a == INVALID || b == INVALID ? INVALID : a - b;
    }
}
