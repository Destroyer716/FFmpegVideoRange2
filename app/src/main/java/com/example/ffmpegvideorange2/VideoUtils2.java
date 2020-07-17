package com.example.ffmpegvideorange2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created By Ele
 * on 2020/6/26
 **/
public class VideoUtils2 {

    private int defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    private String path;
    private Context context;
    private MediaCodec decoder;
    private MediaFormat mediaFormat;
    private MediaExtractor mediaExtractor;
    private MediaCodec.BufferInfo info;

    public VideoUtils2(String path, Context context) {
        this.path = path;
        this.context = context;

        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(path);
            // 查看是否含有视频轨
            int trackIndex = selectVideoTrack();
            if (trackIndex < 0) {
                Log.e("kzg","****************没有找到视频轨");
                return;
            }
            mediaExtractor.selectTrack(trackIndex);
            mediaFormat = mediaExtractor.getTrackFormat(trackIndex);

            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
            if (isSupportColorFormat(defDecoderColorFormat,mime)) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat);
            } else {
                Log.e("kzg","****************此视频不支持YUV420P");
                return;
            }

            decoder.configure(mediaFormat,null,null, 0);
            decoder.start();
        }catch (Exception e ){

        }

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void decodeFrame(long usTime)throws IOException {
        Bitmap b = null;
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        boolean outputDone = false;
        boolean inputDone = false;
        Image image = null;
        info = new MediaCodec.BufferInfo();
        //mediaExtractor.seekTo(usTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        Log.d("kzg", "seek to cost :${System.currentTimeMillis() - s} ");
        while (!outputDone) {
            if (!inputDone) {
                int inputBufferId = decoder.dequeueInputBuffer(10);
                if (inputBufferId >= 0){
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                    if (sampleSize >= 0) {
                        // 将数据压入到输入队列
                        long presentationTimeUs = mediaExtractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0,sampleSize, presentationTimeUs, 0);
                        mediaExtractor.advance();
                    } else {
                        inputDone = true;
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

            }
            //.e("kzg","************************3333333333");
            int outputBufferId = decoder.dequeueOutputBuffer(info, 10);
            if (outputBufferId >= 0){
                Log.e("kzg", "out time ${info.presentationTimeUs} " + info.presentationTimeUs);
                if (info.presentationTimeUs % 500000 == 0){
                }else {
                    decoder.releaseOutputBuffer(outputBufferId, true);
                    continue;
                }
                image = decoder.getOutputImage(outputBufferId);
                YuvImage yuvImage = new YuvImage(VideoUtils.YUV_420_888toNV21(image), ImageFormat.NV21, width, height, null);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                b = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                if (onGetFrameBitmapCallback != null){
                    onGetFrameBitmapCallback.onGetBitmap(b);
                }
                stream.close();
                image.close();
            }

        }
    }


    private int selectVideoTrack(){
        int trackCount = mediaExtractor.getTrackCount();
        for (int i=0;i<trackCount;i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSupportColorFormat(int colorForamt,String mime){
        MediaCodecInfo codecInfo = decoder.getCodecInfo();
        MediaCodecInfo.CodecCapabilities capabilitiesForType = codecInfo.getCapabilitiesForType(mime);
        for (int in:capabilitiesForType.colorFormats){
            if (in == colorForamt){
                return true;
            }
        }
        return false;
    }

    private OnGetFrameBitmapCallback onGetFrameBitmapCallback;

    public OnGetFrameBitmapCallback getOnGetFrameBitmapCallback() {
        return onGetFrameBitmapCallback;
    }

    public void setOnGetFrameBitmapCallback(OnGetFrameBitmapCallback onGetFrameBitmapCallback) {
        this.onGetFrameBitmapCallback = onGetFrameBitmapCallback;
    }

    public interface OnGetFrameBitmapCallback{
        void onGetBitmap(Bitmap bitmap);
    }
}
