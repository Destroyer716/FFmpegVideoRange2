package com.example.myplayer.bean;

import android.graphics.Bitmap;

/**
 * Created By Ele
 * on 2020/8/22
 **/
public class VideoBitmapBean {
    //视频帧图片
    private Bitmap bitmap;
    //此帧图片对应的事件戳
    private long presentationTimeUs;

    public VideoBitmapBean(Bitmap bitmap, long presentationTimeUs) {
        this.bitmap = bitmap;
        this.presentationTimeUs = presentationTimeUs;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public long getPresentationTimeUs() {
        return presentationTimeUs;
    }

    public void setPresentationTimeUs(long presentationTimeUs) {
        this.presentationTimeUs = presentationTimeUs;
    }

    @Override
    public String toString() {
        return "VideoBitmapBean{" +
                "bitmap=" + bitmap +
                ", presentationTimeUs=" + presentationTimeUs +
                '}';
    }
}
