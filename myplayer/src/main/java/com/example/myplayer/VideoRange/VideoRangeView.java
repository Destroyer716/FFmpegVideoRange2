package com.example.myplayer.VideoRange;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myplayer.KzgPlayer;
import com.example.myplayer.R;
import com.example.myplayer.mediacodecframes.OutputImageFormat;
import com.example.myplayer.mediacodecframes.VideoToFrames;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created By Ele
 * on 2020/1/14
 **/
public class VideoRangeView extends FrameLayout {
    //获取预览帧的方式
    public static int FRAME_MODEL_FFMPEG_PLUS = 0;
    public static int FRAME_MODEL_MEDIACODEC = 1;




    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);
    private ThreadPoolExecutor threadPoolExecutor;

    private Context mContext;
    private RecyclerView videoPreRecyclerView;
    private DividingView dividingView;
    //当前预览位置的线
    private View currentPreLine;


    private VideoPreViewAdapter adapter;
    private FFmpegMediaMetadataRetriever fmmr;
    private VideoToFrames videoToFrames;

    private List<Bitmap> bitmapList = new ArrayList<>();
    private int videoDuration = 0;
    private int frameRate = 0;//帧率
    private int recyclerViewLeftPaddin = 0;
    //每一个预览图的宽度
    private int itemWidth = 0;
    //没一个预览图的间隔时间
    private int itemTime = 1000;
    //视频预览条滑动的距离
    private int scrollCount = 0;
    private long lastChangeTime = 0;
    private long lastShowTime = 0;
    private float fps;
    private KzgPlayer player;
    private int getPreFramesModel = 1;
    private String videoFilePaht;
    //预览条的宽度
    private int maxWidth;


    public VideoRangeView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoRangeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoRangeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        this.mContext = context;
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.layout_video_range, this, true);
        videoPreRecyclerView = inflate.findViewById(R.id.rl_video_pre_recyclerview);
        dividingView = inflate.findViewById(R.id.dv_kedu);
        currentPreLine = inflate.findViewById(R.id.v_current_line);
        recyclerViewLeftPaddin = getScreenWidth()/2;



        videoPreRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                long l = System.currentTimeMillis();

                scrollCount += dx;
                dividingView.scrollBy(dx,0);

                if (KzgPlayer.playModel == KzgPlayer.PLAY_MODEL_DEFAULT){
                    return;
                }

                if (l - lastChangeTime < 50){
                    return;
                }
                Log.e("kzg","**********************addOnScrollListener22222");
                lastChangeTime = l;
                //计算当前的对应的预览图
                int millTime = 0;
                if (scrollCount > 0 && itemWidth > 0){
                    int scrollTime = scrollCount / itemWidth; //获取秒数
                    millTime = (int) (((float)scrollCount % (float)itemWidth)/itemWidth * 1000) + scrollTime * 1000;
                }
                if (player != null){
                    if (dx > 0){
                        player.showFrame(millTime / 1000.0,KzgPlayer.seek_advance);
                    }else if (dx < 0){
                        player.showFrame(millTime / 1000.0,KzgPlayer.seek_back);
                    }

                }
                 if (videoRangeViewListener != null && (millTime - lastShowTime)>=fps ){
                     videoRangeViewListener.onScrollerX(millTime, null );
                     lastShowTime = millTime;
                }
            }
        });


        threadPoolExecutor = new ThreadPoolExecutor(5,8,3, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(50));
    }

    private void initRecyclerView(int countItm){
        adapter = new VideoPreViewAdapter(mContext);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        videoPreRecyclerView.addItemDecoration(new SpacesItemDecoration2(recyclerViewLeftPaddin,countItm));
        videoPreRecyclerView.setLayoutManager(linearLayoutManager);
        videoPreRecyclerView.setAdapter(adapter);
    }

    /**
     *
     * @param bitmapData
     */
    public void setBitmapData(List<Bitmap> bitmapData){
        bitmapList = bitmapData;
        adapter.setDataList(bitmapList);
    }


    public void setPlayer(KzgPlayer player){
        this.player = player;
    }

    /**
     * 视频文件路径
     * @param filePath
     */
    public void setFilePath(String filePath){
        File file = new File(filePath);
        if (file == null || !file.exists()){
            Log.e("kzg","*******************文件不存在");
            return;
        }
        videoFilePaht = filePath;

        long start = System.currentTimeMillis();
        fmmr = new FFmpegMediaMetadataRetriever();
        fmmr.setDataSource(file.getAbsolutePath());
        videoDuration = Integer.parseInt(fmmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));
        String frame = fmmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FRAMERATE);
        if (frame.contains(".")){
            frame = frame.split("\\.")[0];
        }
        frameRate = Integer.parseInt(frame) / 2;

        long end = System.currentTimeMillis();
        Log.e("kzg","**********************获取视频基本信息耗时："+(start - end));
        showVideoRangeImg();
    }

    private void showVideoRangeImg(){
        //多少毫秒一帧
        fps = 1000.0f / frameRate;
        Log.e("kzg","**********************fpt:");
        final int duration = videoDuration / itemTime;
        final int totalFrameNum = frameRate * duration;
        Log.e("kzg","**********************videoDuration:"+videoDuration);
        Log.e("kzg","**********************duration:"+duration);

        Log.e("kzg","**********************fpt:"+fps);
        initRecyclerView(duration);
        showSpecifyframes(duration);
    }



    private void showSpecifyframes(int duration){
        switch (getPreFramesModel){
            case 0:
                showModelOne(duration);
                break;
            case 1:
                showFramesModelTow(duration);
                break;
        }
    }


    private void showModelOne(final int duration){
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<duration;i++){
                    final int finalI = i;
                    Bitmap frameAtTime = fmmr.getFrameAtTime(finalI * 1000 * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                    if (frameAtTime == null){
                        frameAtTime = fmmr.getFrameAtTime((finalI - 1) * 1000 * 1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
                    }
                    final Bitmap frameAtTime2 = Bitmap.createScaledBitmap(frameAtTime, 120, 160, false);
                    frameAtTime.recycle();
                    bitmapList.add(frameAtTime2);
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addData(frameAtTime2);
                            if (finalI == 1){
                                int with = videoPreRecyclerView.computeHorizontalScrollRange();
                                itemWidth = with-recyclerViewLeftPaddin;
                                maxWidth = itemWidth * duration;
                                dividingView.setMaxWidth(maxWidth);
                                dividingView.setVideoPicNum(duration);
                                dividingView.setLeftPaddin(recyclerViewLeftPaddin);
                                ViewGroup.LayoutParams layoutParams = dividingView.getLayoutParams();
                                layoutParams.width = maxWidth + 2*recyclerViewLeftPaddin;
                                dividingView.setLayoutParams(layoutParams);
                            }

                        }
                    });
                }
                Log.e("kzg","**********************over");
            }
        });
    }

    private void showFramesModelTow(final int duration){
        //final long[] frameTimeArr = getFrameTimeArr(videoDuration, 10);


        final long startTime = System.currentTimeMillis();
        videoToFrames = new VideoToFrames();
        videoToFrames.setCallback(new VideoToFrames.Callback() {
            @Override
            public void onFinishDecode() {
                long endTime = System.currentTimeMillis();

            }

            @Override
            public void onDecodeFrame(int index) {
            }
        });
        try {
            final int[] i = {0};
            videoToFrames.setOnGetFrameBitmapCallback(new VideoToFrames.OnGetFrameBitmapCallback() {
                @Override
                public void onGetBitmap(final Bitmap bitmap) {
                    if (i[0] >= duration){
                        return;
                    }

                    //bitmapList.add(bitmap);
                    //videoToFrames.seek(frameTimeArr[i[0] + 1]);
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (i[0] == 0){
                                for (int j=0;j<duration;j++){
                                    bitmapList.add(bitmap);
                                }
                                adapter.setDataList(bitmapList);
                            }else {
                                adapter.setData(i[0],bitmap);
                            }
                            //adapter.addData(bitmap);
                            if (i[0] == 1){
                                /*int with = videoPreRecyclerView.computeHorizontalScrollRange();
                                itemWidth = with-recyclerViewLeftPaddin;*/
                                itemWidth = videoPreRecyclerView.getChildAt(0).getWidth();
                                maxWidth = itemWidth * duration;
                                dividingView.setMaxWidth(maxWidth);
                                dividingView.setVideoPicNum(duration);
                                dividingView.setLeftPaddin(recyclerViewLeftPaddin);
                                ViewGroup.LayoutParams layoutParams = dividingView.getLayoutParams();
                                layoutParams.width = maxWidth + 2*recyclerViewLeftPaddin;
                                dividingView.setLayoutParams(layoutParams);
                            }
                            i[0]++;
                        }
                    });
                }

                @Override
                public void onCodecStart() {
                    //videoToFrames.seek(frameTimeArr[0]);
                }
            });
            videoToFrames.setSaveFrames( Environment.getExternalStorageDirectory() + "/jpe", OutputImageFormat.values()[2]);
            Log.e("kzg","**********************videoFilePaht:"+videoFilePaht);
            videoToFrames.decode(videoFilePaht);
        } catch (Throwable t) {
            Log.e("kzg","**********************t:"+t.getMessage());
            t.printStackTrace();
        }
    }


    public void setPlayPercent(float percent){
        if (videoPreRecyclerView != null){
            Log.e("kzg","**********************percent:"+percent);
            int scrollX = (int) (maxWidth * percent);
            Log.e("kzg","**********************scrollX:"+scrollX);
            videoPreRecyclerView.smoothScrollBy(scrollX - scrollCount,0);
        }
    }


    private void showFrameModelThree(){

    }



    //

    /**计算显示指定预览图数量，并计算时间点
     *
     * @param duration   ms
     * @param count  需要生成多少张预览图
     * @return  每一帧的时间 us 值
     */
    private long[] getFrameTimeArr(long duration,int count){
        long[] frameTimeArr = new long[count];
        long tempTime = duration * 1000 / (count - 1);
        frameTimeArr[0] = 0;
        for (int i=0;i<count;i++){
            if (i == count - 1){
                frameTimeArr[i] = duration * 1000;
            }else {
                frameTimeArr[i] = i * tempTime;
            }
        }

        return frameTimeArr;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.e("kzg","**********************VideoRangeView--height:"+getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("kzg","**********************VideoRangeView--onDraw:"+getHeight());

    }


    public void release(){
        if (fixedThreadPool != null){
            fixedThreadPool.shutdown();
            fixedThreadPool = null;
        }
        if (videoToFrames != null){
            videoToFrames.release();
            videoToFrames = null;
        }

        if (player != null){
            player.release();
            player = null;
        }
        mContext = null;
        if (bitmapList != null){
            Iterator<Bitmap> iterator = bitmapList.iterator();
            while (iterator.hasNext()){
                Bitmap next = iterator.next();
                next.recycle();
                next = null;
            }
        }
    }



    private VideoRangeViewListener videoRangeViewListener;

    public void setVideoRangeViewListener(VideoRangeViewListener videoRangeViewListener) {
        this.videoRangeViewListener = videoRangeViewListener;
    }

    public interface VideoRangeViewListener{
        void onScrollerX(int millTime, Bitmap bitmap);

    }


    private int getScreenWidth(){
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        return width;
    }
}
