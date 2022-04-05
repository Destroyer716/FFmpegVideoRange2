package com.sam.video.util;


import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.sam.video.TimeLineApp;


public class ScreenUtil {

    public static final String TAG = ScreenUtil.class.getSimpleName();

    private int mRealSizeWidth;
    private int mRealSizeHeight;

    private ScreenUtil() {
        initRealSize();
    }

    private static final class InstanceHolder {
        private static final ScreenUtil INSTANCE = new ScreenUtil();
    }

    /**
     * 获取实例
     *
     * @return ScreenUtils
     */
    public static ScreenUtil getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private void initRealSize() {
        final WindowManager windowManager =
                (WindowManager) TimeLineApp.Companion.getInstance().getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        if (Build.VERSION.SDK_INT >= 19) {
            // 包含虚拟按键
            display.getRealSize(outPoint);
        } else {
            // 不含虚拟按键
            display.getSize(outPoint);
        }
        if (outPoint.y > outPoint.x) {
            mRealSizeHeight = outPoint.y;
            mRealSizeWidth = outPoint.x;
        } else {
            mRealSizeHeight = outPoint.x;
            mRealSizeWidth = outPoint.y;
        }
    }

    public int getRealSizeWidth() {
        return mRealSizeWidth;
    }

    public int getRealSizeHeight() {
        return mRealSizeHeight;
    }

}
