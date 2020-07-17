package com.example.myplayer.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import com.example.myplayer.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created By Ele
 * on 2020/5/30
 **/
public class KzgGlRender implements GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener{
    public static final int RENDER_YUV = 1;
    public static final int RENDER_MEDIACODEC = 2;

    private Context context;

    private final float[] vertexData = {
            -1f,-1f,
            1f,-1f,
            -1f,1f,
            1f,1f
    };

    private final float[] textureData = {
            0f,1f,
            1f,1f,
            0f,0f,
            1f,0f
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private int renderType = RENDER_YUV;

    //使用ffmpeg解码来播放yuv
    private int program_yuv;
    private int avPosition_yuv;
    private int afPosition_yuv;

    private int sampler_y;
    private int sampler_u;
    private int sampler_v;
    private int[] textureIds_yuv = new int[3];
    private int width_yuv;
    private int height_yuv;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;


    //使用MediaCodec来解码播放
    private int program_mediaCodec;
    private int avPosition_mediaCodec;
    private int afPosition_mediaCodec;
    private int samplerOES_mediacodec;
    private int textureId_mediacodec;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private OnSurfaceCreateListener onSurfaceCreateListener;
    private OnRenderListener onRenderListener;




    public KzgGlRender(Context context) {
        this.context = context;
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }

    public void setRenderType(int renderType) {
        this.renderType = renderType;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRenderYUV();
        initRenderMediaCodec();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.0f,0f,0f,1);
        if (renderType == RENDER_YUV){
            renderYUV();
        }else if (renderType == RENDER_MEDIACODEC){
            renderMediaCodec();
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //每当解码出数据就回调给GlSurfaceView 刷新
        if (onRenderListener != null){
            onRenderListener.onRender();
        }
    }


    private void initRenderYUV(){
        String vertexSource = KzgShaderUtil.readRawTxt(context, R.raw.vertex_shader);
        String fragmenSource = KzgShaderUtil.readRawTxt(context,R.raw.fragment_shader);

        program_yuv = KzgShaderUtil.createProgram(vertexSource,fragmenSource);

        if (program_yuv > 0){
            avPosition_yuv = GLES20.glGetAttribLocation(program_yuv,"av_Position");
            afPosition_yuv = GLES20.glGetAttribLocation(program_yuv,"af_Position");

            sampler_y = GLES20.glGetUniformLocation(program_yuv,"sampler_y");
            sampler_u = GLES20.glGetUniformLocation(program_yuv,"sampler_u");
            sampler_v = GLES20.glGetUniformLocation(program_yuv,"sampler_v");

            //创建纹理，并获取纹理id
            GLES20.glGenTextures(3,textureIds_yuv,0);
            for (int i=0;i<textureIds_yuv.length;i++){
                //绑定纹理
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureIds_yuv[i]);
                //设置环绕，即，超出纹理坐标范围的，进行重复绘制
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);//表示x轴
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);//表示y轴
                //设置过滤 即，纹理像素映射到坐标点后是进行放大还是缩小等一些处理，这里 是进行一个线性操作
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            }

        }
    }

    public void setYUVRenderData(int width,int height,byte[] y,byte[] u,byte[] v){
        this.width_yuv = width;
        this.height_yuv = height;
        this.y = ByteBuffer.wrap(y);
        this.u = ByteBuffer.wrap(u);
        this.v = ByteBuffer.wrap(v);
    }

    private void renderYUV(){
        if (width_yuv > 0 && height_yuv > 0 && y != null && u != null && v != null){

            GLES20.glUseProgram(program_yuv);

            GLES20.glEnableVertexAttribArray(avPosition_yuv);
            GLES20.glVertexAttribPointer(avPosition_yuv,2,GLES20.GL_FLOAT,false,8,vertexBuffer);

            GLES20.glEnableVertexAttribArray(afPosition_yuv);
            GLES20.glVertexAttribPointer(afPosition_yuv,2,GLES20.GL_FLOAT,false,8,textureBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureIds_yuv[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv,height_yuv,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,y);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureIds_yuv[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv/2,height_yuv/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,u);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureIds_yuv[2]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,width_yuv/2,height_yuv/2,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,v);

            GLES20.glUniform1i(sampler_y,0);
            GLES20.glUniform1i(sampler_u,1);
            GLES20.glUniform1i(sampler_v,2);

            y.clear();
            u.clear();
            v.clear();
        }
    }


    private void initRenderMediaCodec(){

        String vertexSource = KzgShaderUtil.readRawTxt(context, R.raw.vertex_shader);
        String fragmenSource = KzgShaderUtil.readRawTxt(context,R.raw.fragment_mediacodec);

        program_mediaCodec = KzgShaderUtil.createProgram(vertexSource,fragmenSource);

        avPosition_mediaCodec = GLES20.glGetAttribLocation(program_mediaCodec,"av_Position");
        afPosition_mediaCodec = GLES20.glGetAttribLocation(program_mediaCodec,"af_Position");

        samplerOES_mediacodec = GLES20.glGetUniformLocation(program_mediaCodec,"sTexture");

        //创建纹理，并获取纹理id
        int[] textureMediaId = new int[1];
        GLES20.glGenTextures(1,textureMediaId,0);
        textureId_mediacodec = textureMediaId[0];

        //设置环绕，即，超出纹理坐标范围的，进行重复绘制
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT);//表示x轴
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT);//表示y轴
        //设置过滤 即，纹理像素映射到坐标点后是进行放大还是缩小等一些处理，这里 是进行一个线性操作
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);

        //创建并绑定纹理ID
        surfaceTexture = new SurfaceTexture(textureId_mediacodec);
        surface = new Surface(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(this);

        //创建好surface需要将其回调给KzgPlayer
        if (onSurfaceCreateListener != null){
            onSurfaceCreateListener.onSurfaceCreate(surface);
        }
    }










    private void renderMediaCodec(){
        surfaceTexture.updateTexImage();
        GLES20.glUseProgram(program_mediaCodec);

        GLES20.glEnableVertexAttribArray(avPosition_mediaCodec);
        GLES20.glVertexAttribPointer(avPosition_mediaCodec, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        GLES20.glEnableVertexAttribArray(afPosition_mediaCodec);
        GLES20.glVertexAttribPointer(afPosition_mediaCodec, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId_mediacodec);
        GLES20.glUniform1i(samplerOES_mediacodec, 0);

    }


    public void setOnSurfaceCreateListener(OnSurfaceCreateListener onSurfaceCreateListener) {
        this.onSurfaceCreateListener = onSurfaceCreateListener;
    }

    public interface OnSurfaceCreateListener{
        void onSurfaceCreate(Surface surface);
    }


    public void setOnRenderListener(OnRenderListener onRenderListener) {
        this.onRenderListener = onRenderListener;
    }

    public interface OnRenderListener{
        void onRender();
    }

}
