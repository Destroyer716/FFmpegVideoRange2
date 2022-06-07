package com.sam.video.timeline.helper;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class IFrameSearch implements Runnable {

    private MediaExtractor mediaExtractor;
    private long duration;
    public static CopyOnWriteArrayList<Long> IframeUs = new CopyOnWriteArrayList();
    private Thread thread;
    private boolean isStop = false;
    //当前播放到的第几个gop
    private int currentIFrameIndex = 0;

    public IFrameSearch(String path) {
        IframeUs.clear();
        mediaExtractor = new MediaExtractor();
        thread = new Thread(this,"IFrameSearch");
        try {
            mediaExtractor.setDataSource(path);
            int count = mediaExtractor.getTrackCount(); //获取轨道数
            for (int i = 0; i < count; i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) { // mp4为“video/avc”
                    mediaExtractor.selectTrack(i);
                    duration = format.getLong(MediaFormat.KEY_DURATION);
                    break;
                }
            }
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
            if (mediaExtractor != null){
                mediaExtractor.release();
                mediaExtractor = null;
            }
        }
    }

    private CopyOnWriteArrayList<Long> get_key_frames_time() {
        long startTime = System.currentTimeMillis();
        long step = 1_000_000; //遍历步长
        mediaExtractor.seekTo(duration/3, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        long start = mediaExtractor.getSampleTime();
        mediaExtractor.advance();
        while(true) { //获取遍历步长
            if (mediaExtractor.getSampleFlags()==MediaExtractor.SAMPLE_FLAG_SYNC) {
                step = Math.min(mediaExtractor.getSampleTime()-start, step);
                break;
            }
            mediaExtractor.advance();
        }
        IframeUs.add(0L);
        mediaExtractor.seekTo(step, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        long time = mediaExtractor.getSampleTime();
        while(time<duration && !isStop) { //获取关键帧时间戳列表
            long time_temp = mediaExtractor.getSampleTime();
            if (time_temp>IframeUs.get(IframeUs.size()-1)) {
                IframeUs.add(time_temp);
                time = time_temp;
            } else {
                time += step;
            }
            mediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        }
        Log.e("kzg","获取所有关键帧所在时间点耗时："+(System.currentTimeMillis() - startTime));
        return IframeUs;
    }

    /**
     * 通过当前播放的时间点 获取所属的gop
     * @param timeUs 微秒
     * @return
     */
    public int getCurrentGopFromTimeUs(long timeUs){
        for (int i=currentIFrameIndex;i<IframeUs.size();i++){
            if (i == 1){
                if (timeUs >= IframeUs.get(i-1) && timeUs < IframeUs.get(i)){
                    currentIFrameIndex = i-1;
                    break;
                }
            }else if (i > 1){
                if (timeUs >= IframeUs.get(i-1) && timeUs < IframeUs.get(i)){
                    currentIFrameIndex = i-1;
                    break;
                }
            }
        }
        return currentIFrameIndex;
    }

    public long getCurrentIFrameTimeUs(){
        return IframeUs.get(currentIFrameIndex);
    }

    @Override
    public void run() {
        for (Long aLong : get_key_frames_time()) {
            Log.e("kzg","get_key_frames_time:"+aLong);
            if (isStop){
                break;
            }
        }
        if (mediaExtractor != null){
            mediaExtractor.release();
            mediaExtractor = null;
        }
    }

    public void release(){
        isStop = true;
        IframeUs.clear();
    }
}
