package com.sam.video.util;

import android.graphics.BitmapFactory;

/**
 * Created By Ele
 * on 2020/1/15
 **/
public class FUtils {

    /**
     * 将毫秒数转成00：00 格式
     * @param duration
     * @return
     */
    public static String MilliToMinuteTime(long duration){
        String time = "" ;
        long minute = duration / 60000 ;
        long seconds = duration % 60000 ;
        long second = Math.round((float)seconds/1000) ;
        if( minute < 10 ){
            time += "0" ;
        }
        time += minute+":" ;
        if( second < 10 ){
            time += "0" ;
        }
        time += second ;
        return time ;
    }


    /**
     * 比较两个数与目标数差距较小的那个
     * @param a
     * @param b
     * @param c  对比目标数
     * @return
     */
    public static long minDifferenceValue(long a,long b,long c) {
        if (a == b) {
            return Math.min(a, c);
        }
        long f_a = Math.abs(a - c);
        long f_b = Math.abs(b - c);
        if (f_a == f_b) {
            return Math.min(a, b);
        }
        return  f_a < f_b?a:b;
    }


    /**
     * 计算Bitmap最大的压缩比
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // 宽和高比需要的宽高大的前提下最大的inSampleSize
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
