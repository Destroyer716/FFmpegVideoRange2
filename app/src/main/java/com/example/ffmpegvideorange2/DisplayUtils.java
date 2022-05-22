package com.example.ffmpegvideorange2;

import android.content.Context;

/*
 * Copyright (c) 2020, jzj
 * Author: jzj
 * Website: www.paincker.com
 */

public class DisplayUtils {

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dip2px(Context context, float dpValue) {
        return dp2px(context, dpValue);
    }

    public static int px2dip(Context context, float pxValue) {
        return px2dp(context, pxValue);
    }

    public static int getScreenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context c) {
        return c.getResources().getDisplayMetrics().heightPixels;
    }
}
