package com.example.ffmpegvideorange2;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created By Ele
 * on 2020/1/19
 **/
public class MyTestVideoPlayer implements SurfaceHolder.Callback {
    static {
        System.loadLibrary("native-lib");
    }

    private SurfaceHolder surfaceHolder;

    public void setSurfaceView(SurfaceView surfaceView){
        if (this.surfaceHolder != null){
            this.surfaceHolder.removeCallback(this);
        }
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.surfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void start(String absolutePath) {

        native_start(absolutePath,surfaceHolder.getSurface());
        //native_play(absolutePath,surfaceHolder.getSurface());
    }










    public  native void native_start(String path , Surface surface);
    public  native void native_play(String path , Surface surface);
}
