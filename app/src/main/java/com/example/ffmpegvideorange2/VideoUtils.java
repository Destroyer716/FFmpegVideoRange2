package com.example.ffmpegvideorange2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;


import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.security.acl.LastOwnerException;

public class VideoUtils {

    static private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private static final boolean VERBOSE = false;
    private static final long DEFAULT_TIMEOUT_US = 10;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmapBySec(MediaExtractor extractor, MediaFormat mediaFormat, MediaCodec decoder, long sec) throws IOException {

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Bitmap bitmap = null;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        boolean stopDecode = false;
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.i("getBitmapBySec", "w: " + width);
        long presentationTimeUs = -1;
        int outputBufferId;
        Image image = null;

        //extractor.seekTo(sec, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                //Log.i("getBitmapBySec", "sawInputEOS: " + sawInputEOS);
                int inputBufferId = decoder.dequeueInputBuffer(-1);
                //Log.i("getBitmapBySec", "inputBufferId: " + inputBufferId);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    //Log.i("getBitmapBySec", "sampleSize: +sampleSize");
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                        Log.e("getBitmapBySec", "presentationTimeUs: " + presentationTimeUs);
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            Log.e("kzg","**********************:");
            outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                Log.e("getBitmapBySec", "sec: " + sec  + "    presentationTimeUs:" + presentationTimeUs  + "   ,extractor.getSampleTime():"+extractor.getSampleTime());
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 | presentationTimeUs >= sec) {
                    sawOutputEOS = true;
                    boolean doRender = (info.size != 0);
                    if (doRender) {
                        Log.e("getBitmapBySec", "deal bitmap which at " + presentationTimeUs);
                        image = decoder.getOutputImage(outputBufferId);
                        YuvImage yuvImage = new YuvImage(YUV_420_888toNV21(image), ImageFormat.NV21, width, height, null);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                        bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        stream.close();
                        image.close();
                    }
                }else if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){

                }
                decoder.releaseOutputBuffer(outputBufferId, true);
            }
        }

        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Bitmap getBitmapByUri(Context context, String path){
        File file = new File(path);
        MediaExtractor extractor = null;
        MediaFormat mediaFormat = null;
        MediaCodec decoder = null;
        Bitmap bitmap =null;
        try{
            extractor = initMediaExtractor(file);
            mediaFormat = initMediaFormat(path, extractor);
            decoder = initMediaCodec(mediaFormat);
            decoder.configure(mediaFormat, null, null, 0);
            decoder.start();

            for (int i=1;i<4;i++){
                bitmap= getBitmapBySec(extractor, mediaFormat, decoder, i * 5000000);
                if (onGetFrameBitmapCallback != null){
                    onGetFrameBitmapCallback.onGetBitmap(bitmap);
                }
            }

        }catch (IOException ex){

        }
        return  bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static byte[] YUV_420_888toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if (VERBOSE) Log.v("YUV_420_888toNV21", "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;

                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;

                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v("YUV_420_888toNV21", "pixelStride " + pixelStride);
                Log.v("YUV_420_888toNV21", "rowStride " + rowStride);
                Log.v("YUV_420_888toNV21", "width " + width);
                Log.v("YUV_420_888toNV21", "height " + height);
                Log.v("YUV_420_888toNV21", "buffer size " + buffer.remaining());
            }
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
            if (VERBOSE) Log.v("", "Finished reading data from plane " + i);
        }
        return data;
    }

    private static long getValidSampleTime(long time, MediaExtractor extractor, MediaFormat format) {
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        int count = 0;
        int maxRange = 5;
        long sampleTime = extractor.getSampleTime();
        while (count < maxRange) {
            extractor.advance();
            long s = extractor.getSampleTime();
            if (s != -1L) {
                count++;
                // 选取和目标时间差值最小的那个
                sampleTime = getMinDiffTime(time, sampleTime, s);
                if (Math.abs(sampleTime - time) <= format.getInteger(MediaFormat.KEY_FRAME_RATE)) {
                    //如果这个差值在 一帧间隔 内，即为成功
                    return sampleTime;
                }
            } else {
                count = maxRange;
            }
        }
        return sampleTime;
    }

    private static long getMinDiffTime(long time, long value1, long value2) {
        long diff1 = value1 - time;
        long diff2 = value2 - time;
        diff1 = diff1 > 0 ? diff1 : -diff1;
        diff2 = diff2 > 0 ? diff2 : -diff2;
        return diff1 < diff2 ? value1 : value2;
    }


    static class VideoInfo {
        long time;
        int width;
        int height;
    }

    public static VideoInfo getVideoInfo(Context context, String path) {
        File file = new File(path);
        MediaPlayer mediaPlayer = getVideoMediaPlayer(context, file);
        VideoInfo vi = new VideoInfo();
        vi.time = mediaPlayer == null ? 0 : mediaPlayer.getDuration();
        vi.height = mediaPlayer == null ? 0 : mediaPlayer.getVideoHeight();
        vi.width = mediaPlayer == null ? 0 : mediaPlayer.getVideoWidth();
        mediaPlayer.release();
        return vi;
    }

/*    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static public boolean initFrameFromVideoBySecond(Context context, String savePath, String
            videoPath, int width, int height, long duration) {
        File file = new File(videoPath);
        MediaExtractor extractor = null;
        MediaFormat mediaFormat = null;
        MediaCodec decoder = null;

        boolean res = false;
        try {
            long totalSec = duration * 1000;

            Log.i("totalSec", "totalSec:" + totalSec);
            for (long time = 0; time < totalSec; time += 200000) {
                //获取这一帧图片
                extractor = initMediaExtractor(file);
                mediaFormat = initMediaFormat(videoPath, extractor);
                decoder = initMediaCodec(mediaFormat);
                decoder.configure(mediaFormat, null, null, 0);
                decoder.start();

                Bitmap bitmap = getBitmapBySec(extractor, mediaFormat, decoder, time);
                if (bitmap == null) continue;

                float xScale = (float) 100 / bitmap.getWidth();

                bitmap = BitmapUtils.compressBitmap(bitmap, xScale, xScale);

                bitmap = BitmapUtils.array2Bitmap(BitmapUtils.getBitmap2GaryArray(bitmap), bitmap.getWidth(), bitmap.getHeight());
                BitmapUtils.addGraphToGallery(context, bitmap, "FunVideo_CachePic_Source", false);
                bitmap.recycle();

                decoder.stop();
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                }
                if (extractor != null) {
                    extractor.release();
                }
            }

            res = true;
        } catch (IOException ex) {
            Log.i("init error", ex.getMessage());
            ex.printStackTrace();
        } finally {

        }
        return res;
    }*/

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d("selectTrack", "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }

    static public MediaCodec initMediaCodec(MediaFormat mediaFormat) throws IOException {

        MediaCodec decoder = null;
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
        if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
            Log.i("initMediaCodec", "set decode color format to type " + decodeColorFormat);
        } else {
            Log.i("initMediaCodec", "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
        }
        return decoder;
    }

    static private MediaFormat initMediaFormat(String path, MediaExtractor extractor) {
        int trackIndex = selectTrack(extractor);
        if (trackIndex < 0) {
            throw new RuntimeException("No video track found in " + path);
        }
        extractor.selectTrack(trackIndex);
        MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
        return mediaFormat;
    }

    static private MediaExtractor initMediaExtractor(File path) throws IOException {
        MediaExtractor extractor = null;
        extractor = new MediaExtractor();
        extractor.setDataSource(path.toString());
        return extractor;
    }

    static private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
    }

    static private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.
            CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

/*    static public String convertVideoBySourcePics(Context context, String picsDri) {
        SeekableByteChannel out = null;
        File destDir = new File(Environment.getExternalStorageDirectory() + "/FunVideo_Video");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File file = new File(destDir.getPath() + "/funvideo_" + System.currentTimeMillis() + ".mp4");
        try {
            file.createNewFile();
            // for Android use: AndroidSequenceEncoder
            File _piscDri = new File(picsDri);
            AndroidSequenceEncoder encoder = AndroidSequenceEncoder.createSequenceEncoder(file, 5);
            for (File childFile : _piscDri.listFiles()) {
                Bitmap bitmap = BitmapUtils.getBitmapByUri(context, Uri.fromFile(childFile));
                encoder.encodeImage(bitmap);
                bitmap.recycle();
            }
            encoder.finish();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
            Log.i("addGraphToGallery", "ok");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            NIOUtils.closeQuietly(out);
        }
        return file.getPath();
    }*/

    private static MediaPlayer getVideoMediaPlayer(Context context, File file) {
        try {
            return MediaPlayer.create(context, Uri.fromFile(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static OnGetFrameBitmapCallback onGetFrameBitmapCallback;

    public OnGetFrameBitmapCallback getOnGetFrameBitmapCallback() {
        return onGetFrameBitmapCallback;
    }

    public void setOnGetFrameBitmapCallback(OnGetFrameBitmapCallback onGetFrameBitmapCallback) {
        this.onGetFrameBitmapCallback = onGetFrameBitmapCallback;
    }

    public interface OnGetFrameBitmapCallback{
        void onGetBitmap(Bitmap bitmap);
    }


    /**
     * 将nv21转bitmap
     * @param data
     * @param width
     * @param height
     * @param practicalWidth  实际宽度，ffmpeg 解码出来的宽度可能会因为16位对齐的问题多出来一些无用数据，绘制的时候这些数据不绘制
     * @return
     */
    public static Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height,int practicalWidth) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bmp.setPixels(rgba, 0 , width, 0, 0, practicalWidth, height);
        return bmp;
    }


    /**
     * I420转nv21
     */
    public static byte[] I420Tonv21(byte[] data, int width, int height) {
        byte[] ret = new byte[data.length];
        int total = width * height;

        ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
        ByteBuffer bufferVU = ByteBuffer.wrap(ret, total, total / 2);

        bufferY.put(data, 0, total);
        for (int i = 0; i < total / 4; i += 1) {
            bufferVU.put(data[i + total + total / 4]);
            bufferVU.put(data[total + i]);
        }

        return ret;
    }

    public static byte[] YUVToNv21(byte[] y,byte[] u,byte[] v){
        //4、交叉存储VU数据
        int lengthY = y.length;
        int lengthU = u.length;
        int lengthV = v.length;

        int newLength = lengthY + lengthU + lengthV;
        byte[] arrayNV21 = new byte[newLength];

        //先将所有的Y数据存储进去
        System.arraycopy(y, 0, arrayNV21, 0, lengthY);

        //然后交替存储VU数据(注意U，V数据的长度应该是相等的，记住顺序是VU VU)
        for (int i = 0; i < lengthV; i++) {
            int index = lengthY + i * 2;
            arrayNV21[index] = v[i];
        }

        for (int i = 0; i < lengthU; i++) {
            int index = lengthY + i * 2 + 1;
            arrayNV21[index] = u[i];
        }
        return  arrayNV21;
    }


    /**
     * 第二种：按采样大小压缩
     *
     * @param src       源图片
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @param recycle   是否回收
     * @return 按采样率压缩后的图片
     */
    public static Bitmap compressBySampleSize(final Bitmap src, final int maxWidth, final int maxHeight, final boolean recycle) {
        if (src == null || src.getWidth() == 0 || src.getHeight() == 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        if (recycle && !src.isRecycled()) {
            src.recycle();
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        return bitmap;
    }

    /**
     * 计算获取缩放比例inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }
        return inSampleSize;
    }


}
