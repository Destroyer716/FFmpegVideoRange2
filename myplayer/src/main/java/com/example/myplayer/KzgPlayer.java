package com.example.myplayer;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.example.myplayer.bean.YUVBean;
import com.example.myplayer.mediacodec.KzglVideoSupportUtil;
import com.example.myplayer.opengl.KzgGLSurfaceView;
import com.example.myplayer.opengl.KzgGlRender;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created By Ele
 * on 2020/5/9
 **/
public class KzgPlayer {

    static {
        System.loadLibrary("native-lib");
    }


    //播放模式，正常播放
    public static final int PLAY_MODEL_DEFAULT = 1;
    //播放模式，逐帧播放
    public static final int PLAY_MODEL_FRAME_PREVIEW = 2;
    //回退
    public static final int seek_back = 0;
    //前进
    public static final int seek_advance = 1;

    private int seekType = seek_advance;

    private PlayerListener playerListener;
    private GetFrameListener getFrameListener;
    private String source;
    public boolean isPlaying = false;
    public boolean isPause = false;
    //是否可以播放，主要是为了避免未处理完seek 等操作时播放产生BUG
    public boolean enablePlay = false;
    private TimeInfoBean timeInfoBean;
    private boolean isPlayNext = false;
    private static int duration = -1;
    private static int volume = 50;
    private static float pitch = 1.0f;
    private static float speed = 1.0f;
    private static float amplifyVol = 5.0f;
    private KzgGLSurfaceView kzgGLSurfaceView;
    public static int playModel = PLAY_MODEL_DEFAULT;

    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private Surface surface;
    private MediaCodec.BufferInfo videoDecodeInfo;
    private MyQueue yUVQueue;
    private PacketQueue packetQueue;



    public KzgPlayer() {

    }

    public void setSource(String source) {
        this.source = source;
    }

    public void parpared(){
        if (TextUtils.isEmpty(source)){
            Log.e("kzg","source 为空");
            return;
        }
        n_parpared(source);
    }


    public void setKzgGLSurfaceView(KzgGLSurfaceView kzgGLSurfaceView) {
        this.kzgGLSurfaceView = kzgGLSurfaceView;
        kzgGLSurfaceView.getKzgGlRender().setOnSurfaceCreateListener(new KzgGlRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(Surface sf) {
                surface = sf;
            }
        });
    }


    public void showFrame(double timestamp,int seekType){
        //Log.e("kzg","**********************showFrame:"+timestamp   + "     ,type :" + seekType);
        this.seekType = seekType;
        if (seekType == seek_back){
            //后退时的逐帧显示
            n_seek((int) (timestamp*1000));
        }else {
            //前进时的逐帧显示
            n_showframe(timestamp);
        }

        //n_showframeFromSeek(timestamp);
        //showFrameByYUV(timestamp);
    }

    public void start(){
        if (isPlaying || isPause){
            return;
        }
        if (TextUtils.isEmpty(source)){
            Log.e("kzg","source 为空");
            return;
        }
        isPlaying = true;
        setPlayModel(playModel);
        setVolume(volume);
        setPitch(pitch);
        setSpeed(speed);
        setAmplifyVol(amplifyVol);
        n_play();
    }

    public void pause(){
        if (isPlaying){
            n_pause();
            isPlaying = false;
            isPause = true;
        }
    }

    public void resume(){
        if (isPause){
            n_resume();
            isPlaying = true;
            isPause = false;
        }
    }

    public void stop() {
        if (!isPlaying){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_stop();
                release();
            }
        }).start();
        isPause = false;
        isPlaying = false;

    }

    public void seek(int sec){
        n_seek(sec);
    }

    public void playNext(String url){
        source = url;
        isPlayNext = true;
        stop();
    }

    public int getDuration(){
        if (duration < 0){
            duration = n_duration();
        }
        return duration;
    }

    public void setVolume(int vol){
        if (vol <= 0){
            vol = 0;
        }else if (vol > 100){
            vol = 100;
        }
        volume = vol;
        n_volume(vol);
    }

    public void setSpeed(float speed) {
        KzgPlayer.speed = speed;
        n_speed(speed);
    }

    public void setPitch(float pitch) {
        KzgPlayer.pitch = pitch;
        n_pitch(pitch);
    }

    public void setAmplifyVol(float vol){
        amplifyVol = vol;
        n_amplifyVol(vol);
    }

    public void setPlayModel(int model){
        playModel = model;
        n_playmodel(playModel);
    }

    public int getPlayModel(){
        return playModel;
    }

    public int getsSeekType(){
        return seekType;
    }

    public void startRecord(File outFile){
        if (n_sampleRate() > 0 && outFile != null){
            initMediaCodec(n_sampleRate(),outFile);
        }
    }


    public void initGetFrame(String source){
        n_init_frame(source);
    }

    public void startGetFrame(){
        n_start_get_frame();
    }



/************************播放视频或者逐帧预览*********************************/
    private native void n_parpared(String source);
    private native void n_play();
    private native void n_pause();
    private native void n_resume();
    private native void n_stop();
    private native void n_seek(int sec);
    private native int n_duration();
    private native void n_volume(int volume);
    private native void n_pitch(float pitch);
    private native void n_speed(float speed);
    private native void n_amplifyVol(float vol);
    private native int n_sampleRate();
    private native void n_playmodel(int model);
    private native void n_showframe(double timestamp);
    private native void n_showframeFromSeek(double timestamp);


/************************按时间抽帧******************************************/
    private native void n_init_frame(String source);
    private native void n_start_get_frame();


    public void onError(int code,String msg){
        stop();
        if (playerListener != null){
            playerListener.onError(code,msg);
        }
    }

    public void onGetVideoInfo(int fps,long duration,int width,int height){
        Log.e("kzg","**********************onGetVideoInfo--fps:"+fps +"   ,duration:" + duration+ "    ,width:"+width+ "   ,height:" + height );
        if (playerListener != null){
            playerListener.onGetVideoInfo(fps,duration,width,height);
        }
    }

    public void onPrepare(){
        if (playerListener != null){
            playerListener.onPrepare();
        }
    }

    public void onProgress(long currentTime,long totalTime){
        //Log.e("kzg","**********************currentTime:"+currentTime+"   ,totalTime:"+totalTime);
        if (playerListener != null){
            playerListener.onProgress(currentTime,totalTime);
        }
    }

    public void onLoad(boolean isLoad){
        if (playerListener != null){
            playerListener.onLoadChange(isLoad);
        }
    }

    public void onTimeInfo(int current,int total){
        if (playerListener != null){
            if (timeInfoBean == null){
                timeInfoBean = new TimeInfoBean();
            }
            timeInfoBean.setCurrentTime(current);
            timeInfoBean.setTotalTime(total);
            playerListener.onTimeInfo(timeInfoBean);
        }
    }

    public void onPlayStop(){
        if (playerListener != null){
            playerListener.onPlayStop();
        }
    }

    public void onComplete(){
        stop();
        if (playerListener != null){
            playerListener.onComplete();
        }
    }

    public void onPlayNext(){
        if (isPlayNext){
            isPlayNext = false;
            parpared();
        }
    }

    public void onDB(int db){
        if (playerListener != null){
            playerListener.onDB(db);
        }
    }

    public void onEnableStartPlay(boolean enable){
        enablePlay = enable;
        if (playerListener != null){
            playerListener.onEnablePlayChange(enable);
        }
    }

    public void onCallRenderYUV(int width, int height, byte[] y, byte[] u, byte[] v,int practicalWidth){
        //Log.e("kzg","获取到视频的yuv数据  y:" + y.length + "   u:" + u.length + "   v:" + v.length + "   ,width:" + width + "    ,height:"+height);
        if (kzgGLSurfaceView != null){
            kzgGLSurfaceView.getKzgGlRender().setRenderType(KzgGlRender.RENDER_YUV);
            kzgGLSurfaceView.setYUV(width,height,y,u,v,practicalWidth);
        }

    }


    public boolean onCallIsSupportMediaCodec(String ffCodecName){
        return KzglVideoSupportUtil.isSupportCodec(ffCodecName);
    }

    public int onCallJavaQueueSize(){
        if (yUVQueue == null){
            return 0;
        }
        return yUVQueue.getQueueSize();
    }


    public void onGetFrameInitSuccess(){
        if (getFrameListener != null){
            getFrameListener.onInited();
        }
    }






    public void setPlayerListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
    }



    public interface PlayerListener{
        void onError(int code,String msg);
        void onPrepare();
        void onLoadChange(boolean isLoad);
        void onProgress(long currentTime,long totalTime);
        void onTimeInfo(TimeInfoBean timeInfoBean);
        void onEnablePlayChange(boolean enable);
        void onPlayStop();
        void onComplete();
        void onDB(int db);
        void onGetVideoInfo(int fps,long duration,int widht,int height);
    }

    public void setGetFrameListener(GetFrameListener getFrameListener) {
        this.getFrameListener = getFrameListener;
    }

    public interface GetFrameListener{
        void onInited();
    }



    /**编码音频功能相关变量*/
    private MediaFormat encodeFormat;
    private int aacSampleRate;
    private MediaCodec enCoder;
    private MediaCodec.BufferInfo bufferInfo;
    private FileOutputStream outputStream;
    private int perPcmSize;
    private byte[] outByteBuffer;
    private boolean isStartCodec = false;


    private void initMediaCodec(int sampleRate, File outFile){

        try {
            aacSampleRate = getADTSsamplerate(sampleRate);
            //创建媒体文件格式信息封装类
            encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,2);
            //设置媒体文件封装格式信息
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE,96000);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,8192);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,4096);
            //创建解码器
            enCoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            //创建数据信息封装类
            bufferInfo = new MediaCodec.BufferInfo();
            if (enCoder == null){
                Log.e("kzg","********************创建解码器失败");
                return;
            }
            //配置解码器
            enCoder.configure(encodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            outputStream = new FileOutputStream(outFile);
            enCoder.start();
            Log.e("kzg","********************启动解码器："+sampleRate);
            isStartCodec = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void enCodecPCMToAAC(int size,byte[] pcmData){
        if (pcmData != null && enCoder != null && isStartCodec){
            //从解码器中获取可用的输入缓冲区下标
            Log.e("kzg","********************enCodecPCMToAAC：");
            int inputBufferIndex = enCoder.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0){
                //获取输入缓冲区
                ByteBuffer byteBuffer = enCoder.getInputBuffers()[inputBufferIndex];
                byteBuffer.clear();
                //将pcm数据放入缓冲区
                Log.e("kzg","**********************pcmDataL"+pcmData.length);
                Log.e("kzg","**********************size"+size);
                byteBuffer.put(pcmData);
                //将添加数据的缓冲区重新加入到解码列队
                enCoder.queueInputBuffer(inputBufferIndex,0,size,0,0);
            }

            //从解码器中获取输出缓冲区下标,并将缓冲区的信息赋值给bufferInfo
            int outputBufferIndex = enCoder.dequeueOutputBuffer(bufferInfo,0);
            //因为有可能无法一次取出全部数据，所以如果可用获取到下标就继续读数据
            while (outputBufferIndex >= 0){
                try {
                    //计算需要写入的数据大小，因为要添加DTS头，固定占用7个字节
                    perPcmSize = bufferInfo.size + 7;
                    //创建接收编码后数据的缓冲区
                    outByteBuffer = new byte[perPcmSize];
                    ByteBuffer byteBuffer = enCoder.getOutputBuffers()[outputBufferIndex];
                    //设置读取的范围
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    //向输出缓冲区中写入头部信息
                    addADtsHeader(outByteBuffer,perPcmSize,aacSampleRate);

                    //将byteBuffer里解码好的数据赋值给outByteBuffer
                    byteBuffer.get(outByteBuffer,7,bufferInfo.size);
                    byteBuffer.position(bufferInfo.offset);
                    outputStream.write(outByteBuffer,0,perPcmSize);

                    enCoder.releaseOutputBuffer(outputBufferIndex,false);
                    //继续获取解码后的输出缓冲区，如果还有数据，就继续循环
                    outputBufferIndex = enCoder.dequeueOutputBuffer(bufferInfo,0);
                    outByteBuffer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void addADtsHeader(byte[] packet, int packetLen, int samplerate){
        int profile = 2; // AAC LC
        int freqIdx = samplerate; // samplerate
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF; // 0xFFF(12bit) 这里只取了8位，所以还差4位放到下一个里面
        packet[1] = (byte) 0xF9; // 第一个t位放F
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private int getADTSsamplerate(int samplerate){
        int rate = 4;
        switch (samplerate){
            case 96000:
                rate = 0;
                break;
            case 88200:
                rate = 1;
                break;
            case 64000:
                rate = 2;
                break;
            case 48000:
                rate = 3;
                break;
            case 44100:
                rate = 4;
                break;
            case 32000:
                rate = 5;
                break;
            case 24000:
                rate = 6;
                break;
            case 22050:
                rate = 7;
                break;
            case 16000:
                rate = 8;
                break;
            case 12000:
                rate = 9;
                break;
            case 11025:
                rate = 10;
                break;
            case 8000:
                rate = 11;
                break;
            case 7350:
                rate = 12;
                break;
        }
        return rate;
    }


    /**
     * 视频硬解码
     */

    public void initMediaCodecVideo(String codecName,int width,int height,byte[] csd_0,byte[] csd_1){
        Log.e("kzg","**********************initMediaCodecVideo");
        if (surface != null){
            //获取ffmpeg中视频编码格式，在mediaCodec中对应的名字
            kzgGLSurfaceView.getKzgGlRender().setRenderType(KzgGlRender.RENDER_MEDIACODEC);
            String mime = KzglVideoSupportUtil.findVideoCodecName(codecName);
            //创建MediaFormat
            mediaFormat = MediaFormat.createVideoFormat(mime,width,height);
            //设置参数
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,width * height);
            mediaFormat.setByteBuffer("cds-0",ByteBuffer.wrap(csd_0));
            mediaFormat.setByteBuffer("cds-1",ByteBuffer.wrap(csd_1));
            Log.e("kzg","**************mediaFormat:" + mediaFormat.toString());
            videoDecodeInfo = new MediaCodec.BufferInfo();

            try {
                mediaCodec = MediaCodec.createDecoderByType(mime);
                mediaCodec.configure(mediaFormat,surface,null,0);
                mediaCodec.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void decodeAVPacket(int dataSize,byte[] data){

        try {
            if(surface != null && dataSize > 0 && data != null && mediaCodec != null){
                //获取可用的输入缓冲区下标
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(10);
                if (inputBufferIndex >= 0){
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffers()[inputBufferIndex];
                    byteBuffer.clear();
                    byteBuffer.put(data);
                    mediaCodec.queueInputBuffer(inputBufferIndex,0,dataSize,0,0);
                }

                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(videoDecodeInfo, 10);
                while (outputBufferIndex >= 0){
                    mediaCodec.releaseOutputBuffer(outputBufferIndex,true);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(videoDecodeInfo, 10);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }




    public void initMediaCodecVideoByYUV(String codecName,int width,int height,byte[] csd_0,byte[] csd_1){
        //获取ffmpeg中视频编码格式，在mediaCodec中对应的名字
        String mime = KzglVideoSupportUtil.findVideoCodecName(codecName);
        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        yUVQueue = new MyQueue();
        packetQueue = new PacketQueue();
        bufferInfo = new MediaCodec.BufferInfo();
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaCodec.configure(mediaFormat, null, null, 0);
        mediaCodec.start();
        decodePacket();

        /*String mime = KzglVideoSupportUtil.findVideoCodecName(codecName);
        //创建MediaFormat
        mediaFormat = MediaFormat.createVideoFormat(mime,width,height);
        //设置参数
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,width * height);
        mediaFormat.setByteBuffer("cds-0",ByteBuffer.wrap(csd_0));
        mediaFormat.setByteBuffer("cds-1",ByteBuffer.wrap(csd_1));
        Log.e("kzg","**************mediaFormat:" + mediaFormat.toString());
        videoDecodeInfo = new MediaCodec.BufferInfo();

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);
            mediaCodec.configure(mediaFormat,surface,null,0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }

    long startTime;
    public void decodeAVPacketForYUV(int dataSize,byte[] data,int width,int height,double timestamp){
       /* Log.e("kzg","**********************decodeAVPacketForYUV width:" + width + ", height:"
                + height + " , dataSize:" + dataSize + ", lenght:" + data.length + " ,timestamp" + timestamp);*/

        PacketBean packetBean = new PacketBean();
        packetBean.setData(data);
        packetBean.setDataSize(dataSize);
        packetBean.setWidth(width);
        packetBean.setHeight(height);
        packetBean.setPts(timestamp);
        packetQueue.enQueue(packetBean);

        /*try {
            if(surface != null && dataSize > 0 && data != null && mediaCodec != null){
                //获取可用的输入缓冲区下标
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(10);
                if (inputBufferIndex >= 0){
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffers()[inputBufferIndex];
                    byteBuffer.clear();
                    byteBuffer.put(data);
                    mediaCodec.queueInputBuffer(inputBufferIndex,0,dataSize,0,0);
                }

                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(videoDecodeInfo, 10);
                while (outputBufferIndex >= 0){
                    mediaCodec.releaseOutputBuffer(outputBufferIndex,true);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(videoDecodeInfo, 10);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }



    private void decodePacket(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int outputFrameCount = 0;
                while (isPlaying) {
                    if (yUVQueue.getQueueSize() < 90 && packetQueue.getQueueSize() > 0){
                        if (yUVQueue.getQueueSize() == 0) {
                            startTime = System.currentTimeMillis();
                        }
                        synchronized (KzgPlayer.class) {
                            PacketBean packetBean = packetQueue.deQueue();

                            if (packetBean.getDataSize() > 0 && packetBean.getData() != null && mediaCodec != null) {
                                int inputBufferIndex = mediaCodec.dequeueInputBuffer(10);
                                if (inputBufferIndex >= 0) {
                                    ByteBuffer inputBuffer;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                                    } else {
                                        inputBuffer = mediaCodec.getInputBuffers()[inputBufferIndex];
                                    }
                                    if (inputBuffer != null) {
                                        inputBuffer.clear();
                                        inputBuffer.put(packetBean.getData(), 0, packetBean.getDataSize());
                                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, packetBean.getDataSize(), 0, 0);
                                    }
                                }
                                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10);
                                ByteBuffer outputBuffer;
                                while (outputBufferIndex > 0) {
                                    outputFrameCount ++;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                                        Image image = mediaCodec.getOutputImage(outputBufferIndex);

                                        String fileName;
                                        //fileName = Environment.getExternalStorageDirectory() + "/jpe/" + String.format("frame_%05d.jpg", outputFrameCount);
                                        //compressToJpeg(fileName, image);
                                    } else {
                                        outputBuffer = mediaCodec.getOutputBuffers()[outputBufferIndex];
                                    }


                                    if (outputBuffer != null) {
                                        /*outputBuffer.position(0);
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                                        byte[] yuvData = new byte[outputBuffer.remaining()];
                                        outputBuffer.get(yuvData);

                                        //分离yuv
                                        int yBufferSize = packetBean.getWidth() * packetBean.getHeight();
                                        int uBufferSize = packetBean.getWidth() / 2 * packetBean.getHeight() / 2;
                                        int vBufferSize = packetBean.getWidth() / 2 * packetBean.getHeight() / 2;
                                        byte[] dstY = new byte[yBufferSize];
                                        byte[] dstU = new byte[uBufferSize];
                                        byte[] dstV = new byte[vBufferSize];

                                        System.arraycopy(yuvData, 0, dstY, 0, yBufferSize);
                                        //交叉存储的uv数据分离
                                        int sizeU = 0, sizeV = 0;
                                        for (int i = yBufferSize; i < packetBean.getWidth() * packetBean.getHeight() * 3 / 2; i++) {
                                            dstU[sizeU] = yuvData[i];
                                            if (i == (packetBean.getWidth() * packetBean.getHeight() * 3 / 2 - 1))
                                                break;
                                            dstV[sizeV] = yuvData[i + 1];
                                            sizeU++;
                                            sizeV++;
                                            i++;
                                        }
                                        Log.e("kzg", "**********************yUVQueue.getQueueSize():" + yUVQueue.getQueueSize());
                                        //缓存

                                        YUVBean yuvBean = new YUVBean();
                                        yuvBean.setTimestamp(packetBean.getPts());
                                        yuvBean.setyData(dstY);
                                        yuvBean.setuData(dstU);
                                        yuvBean.setvData(dstV);
                                        yuvBean.setWidth(packetBean.getWidth());
                                        yuvBean.setHeight(packetBean.getHeight());
                                        boolean isSuccess = yUVQueue.enQueue(yuvBean);
                                        if (!isSuccess) {
                                            Log.e("kzg", "*******************加入队列失败：" + yUVQueue.getQueueSize());
                                        }*/
                                        if (yUVQueue.getQueueSize() == 90) {
                                            long endTime = System.currentTimeMillis();
                                            Log.e("kzg", "*******************解码总耗时：" + (endTime - startTime));
                                        }


                                        //给openGL处理
                                        //onCallRenderYUV(width,height,dstY,dstU,dstV);

                                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                        outputBuffer.clear();
                                    }
                                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10);
                                }
                            }
                        }

                    }
                }
            }
        }).start();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void compressToJpeg(String fileName, Image image) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(getDataFromImage(image, COLOR_FormatNV21), ImageFormat.NV21, rect.width(), rect.height(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
    }


    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Log.v("kzg", "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            /*if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }*/
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            Log.v("kzg", "Finished reading data from plane " + i);
        }
        return data;
    }


    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public void release(){
        if (mediaCodec != null) {
            try {
                mediaCodec.flush();
                mediaCodec.stop();
                mediaCodec.release();
            }catch (Exception e){
                e.printStackTrace();
            }

            mediaCodec = null;
            mediaFormat = null;
            bufferInfo = null;
        }
        if (surface != null){
            surface.release();
            surface = null;
        }
        if (kzgGLSurfaceView != null){
            kzgGLSurfaceView.removeCallbacks(null);
            kzgGLSurfaceView = null;
        }
    }


    private void showFrameByYUV(double timestamp) {
        //Log.e("kzg","kzgPlayer get showFrame ,   timestamp1111111111:"+timestamp);
        if ( yUVQueue != null && yUVQueue.getQueueSize() > 0){
            YUVBean yuvBean = (YUVBean) yUVQueue.getFirst();
            if (yuvBean == null){
                Log.e("kzg","**********************从队列取数据失败");
                return;
            }
            double pts = yuvBean.getTimestamp();
            Log.e("kzg","kzgPlayer get frameQueue pts222222222222222:" + pts + " ,   timestamp:"+timestamp);
            if (timestamp >= (pts - 0.03) && timestamp <= (pts + 0.03)){
                if (yUVQueue.deQueue() == null){
                    Log.e("kzg","**********************从队列删除数据失败");
                }
                onCallRenderYUV(yuvBean.getWidth(),yuvBean.getHeight(),yuvBean.getyData(),yuvBean.getuData(),yuvBean.getvData(),yuvBean.getWidth());
            } else{
                if (timestamp >(pts + 0.03)){
                    if (yUVQueue.deQueue() == null){
                        Log.e("kzg","**********************从队列删除数据失败");
                    }
                    this.showFrameByYUV(timestamp);
                }
                return;
            }
            return;

        } else{
            Log.e("kzg","kzgVideo frameQueue is empty");
        }
    }


}
