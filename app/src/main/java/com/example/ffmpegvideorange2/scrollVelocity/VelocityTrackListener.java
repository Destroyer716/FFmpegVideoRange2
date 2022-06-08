package com.example.ffmpegvideorange2.scrollVelocity;/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

/**
 * 滚动速度监听器
 */
public interface VelocityTrackListener {

    VelocityTrackListener EMPTY_LISTENER = new VelocityTrackListener() {
        @Override
        public void onVelocityChanged(int velocity) {

        }

        @Override
        public void onScrollFast(int velocity) {

        }

        @Override
        public void onScrollSlow(int velocity) {

        }
    };

    /**
     * 速度发生变化
     */
    void onVelocityChanged(int velocity);

    /**
     * 快速滚动
     */
    void onScrollFast(int velocity);

    /**
     * 慢速滚动或停止滚动
     */
    void onScrollSlow(int velocity);
}
