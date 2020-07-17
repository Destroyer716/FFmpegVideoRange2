package com.example.myplayer.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created By Ele
 * on 2020/5/30
 **/
public class KzgGLSurfaceView extends GLSurfaceView {

    private KzgGlRender kzgGlRender;

    public KzgGLSurfaceView(Context context) {
        this(context,null);
    }

    public KzgGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        kzgGlRender = new KzgGlRender(context);
        setRenderer(kzgGlRender);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        kzgGlRender.setOnRenderListener(new KzgGlRender.OnRenderListener() {
            @Override
            public void onRender() {
                //主动调用请求刷新数据
                requestRender();
            }
        });
    }


    public void setYUV(int width,int height,byte[] y,byte[] u,byte[] v){
        if (kzgGlRender != null){
            kzgGlRender.setYUVRenderData(width,height,y,u,v);
            //主动调用请求刷新数据
            requestRender();
        }
    }

    public KzgGlRender getKzgGlRender() {
        return kzgGlRender;
    }
}
