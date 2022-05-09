package com.example.ffmpegvideorange2;

import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.sam.video.timeline.bean.TargetBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * Created By Ele
 * on 2020/1/15
 **/
public class Utils {

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
     * 将存放帧时间信息的map进行排序  升序
     * @param hashMap
     * @return
     */
    public static ArrayList<Map.Entry<ImageView,TargetBean>> sortHashMap(Hashtable hashMap){
        ArrayList<Map.Entry<ImageView,TargetBean>> list = new ArrayList(hashMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<ImageView, TargetBean>>() {

            @Override
            public int compare(Map.Entry<ImageView, TargetBean> t0, Map.Entry<ImageView, TargetBean> t1) {
                return (int) (t0.getValue().getTimeUs() - t1.getValue().getTimeUs());
            }

        });

        return list;
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        Log.i("calculateInSampleSize", "calculateInSampleSize: out width and height is " + width + " height " + height);
        int inSampleWidth = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // 采样率设置为2的指数
            while ((halfHeight / inSampleWidth) >= reqHeight && (halfWidth / inSampleWidth) >= reqWidth) {
                inSampleWidth *= 2;
            }
        }
        return inSampleWidth;
    }



}
