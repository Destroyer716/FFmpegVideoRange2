package com.example.ffmpegvideorange2.scrollVelocity;

import java.lang.ref.WeakReference;

/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

/**
 * 弱引用辅助类
 */
public class WeakRefWrapper<T> {

    private WeakReference<T> mRef;

    public T get() {
        return mRef != null ? mRef.get() : null;
    }

    public void set(T target) {
        mRef = target == null ? null : new WeakReference<T>(target);
    }
}
