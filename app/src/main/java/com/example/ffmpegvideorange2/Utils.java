package com.example.ffmpegvideorange2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.sam.video.timeline.bean.TargetBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
        //long second = Math.round((float)seconds/1000) ;
        long second = seconds/1000 ;
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


    /**
     *  计算缩放到指定宽高，的缩放比
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
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


    public static void saveBitmap(String targetPath,String name, Bitmap bm) {
        Log.d("Save Bitmap", "Ready to save picture");
        //判断指定文件夹的路径是否存在
        if (!new File(targetPath).exists()) {
            Log.d("Save Bitmap", "TargetPath isn't exist");
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(targetPath, name);

            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bm.compress(Bitmap.CompressFormat.JPEG, 20, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                Log.d("Save Bitmap", "The picture is save to your phone!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


}
