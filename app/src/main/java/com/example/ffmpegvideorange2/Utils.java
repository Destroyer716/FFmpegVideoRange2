package com.example.ffmpegvideorange2;

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




}
