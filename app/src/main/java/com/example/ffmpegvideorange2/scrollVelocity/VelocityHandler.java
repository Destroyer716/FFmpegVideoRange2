package com.example.ffmpegvideorange2.scrollVelocity;

import android.content.Context;

/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

public interface VelocityHandler {

    int DEFAULT_UP_THRESHOLD = 600;
    int DEFAULT_DOWN_THRESHOLD = 400;

    /**
     * 设置速度阈值。应该确保upThreshold >= downThreshold > 0
     *
     * @param upThreshold   速度增加超过此值时reach->true
     * @param downThreshold 速度减小小于此值时reach->false
     */
    void setThresholdInDp(Context context, int upThreshold, int downThreshold);

    /**
     * 设置速度阈值。应该确保upThreshold >= downThreshold > 0
     *
     * @param upThreshold   速度增加超过此值时reach->true
     * @param downThreshold 速度减小小于此值时reach->false
     */
    void setThreshold(int upThreshold, int downThreshold);

    /**
     * 设置速度监听器
     *
     * @param listener
     */
    void setVelocityTrackerListener(VelocityTrackListener listener);
}
