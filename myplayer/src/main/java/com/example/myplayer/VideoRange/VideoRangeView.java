package com.example.myplayer.VideoRange;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.myplayer.KzgPlayer;
import com.example.myplayer.R;
import com.example.myplayer.bean.VideoBitmapBean;
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
    private VideoRangeRecyclerView videoPreRecyclerView;
    private DividingView dividingView;
    //当前预览位置的线
    private View currentPreLine;


    private VideoPreViewAdapter adapter;
    private FFmpegMediaMetadataRetriever fmmr;
    private VideoToFrames videoToFrames;
    private MyLinearLayoutManager linearLayoutManager;

    private List<VideoBitmapBean> bitmapList = new ArrayList<>();
    private long videoDuration = 0;
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



    //缩放时的两个手指的起始点
    private PointF startPointOne;
    private PointF startPointTow;
    //当前是否是双指触碰状态
    private boolean isDoublePoint = false;
    //是否拦截触摸事件
    public static boolean isIntercept = false;
    //两点之间的距离
    private float doublePointDistance = 0;
    //每一次从缩放开始到手指抬起总共缩放的宽度
    private int totalZoomWidth = 0;



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


        findViewById(R.id.video_range_view_root).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("kzg","**********************video_range_view_root");
                return false;
            }
        });



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
        linearLayoutManager = new MyLinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        videoPreRecyclerView.addItemDecoration(new SpacesItemDecoration2(recyclerViewLeftPaddin,countItm));
        videoPreRecyclerView.setLayoutManager(linearLayoutManager);
        videoPreRecyclerView.setPadding(videoPreRecyclerView.getPaddingLeft(),videoPreRecyclerView.getPaddingTop(),recyclerViewLeftPaddin,videoPreRecyclerView.getPaddingBottom());
        videoPreRecyclerView.setAdapter(adapter);
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
        videoDuration = Long.parseLong(fmmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION));
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
        final int duration = (int) (videoDuration / itemTime);
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
                    final VideoBitmapBean videoBitmapBean = new VideoBitmapBean(frameAtTime2, (finalI - 1) * 1000 * 1000);
                    bitmapList.add(videoBitmapBean);
                    ((Activity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addData(videoBitmapBean);
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
                public void onGetBitmap(final Bitmap bitmap, final long usTime) {
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
                                    VideoBitmapBean videoBitmapBean = new VideoBitmapBean(bitmap, usTime);
                                    if (j > 0){
                                        //刚开始预览条上面的预览帧可能没有解码完，所以就先给个默认时间戳
                                        videoBitmapBean.setPresentationTimeUs((j +1) * 1000 * 1000);
                                    }
                                    bitmapList.add(videoBitmapBean);
                                }
                                adapter.setDataList(bitmapList);
                            }else {
                                VideoBitmapBean videoBitmapBean = new VideoBitmapBean(bitmap, usTime);
                                adapter.setData(i[0],videoBitmapBean);
                            }
                            //adapter.addData(bitmap);
                            if (i[0] == 1){
                                /*int with = videoPreRecyclerView.computeHorizontalScrollRange();
                                itemWidth = with-recyclerViewLeftPaddin;*/
                                itemWidth = videoPreRecyclerView.getChildAt(0).getWidth();
                                Log.e("kzg","**********************itemWidth:"+itemWidth);
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
            Iterator<VideoBitmapBean> iterator = bitmapList.iterator();
            while (iterator.hasNext()){
                VideoBitmapBean next = iterator.next();
                if (next.getBitmap() != null){
                    next.getBitmap().recycle();
                    next.setBitmap(null);
                }
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = true;
                videoPreRecyclerView.setIntercept(!isIntercept);
                doublePointDistance = getDoubleFingerDistance(ev);
                isDoublePoint = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = false;
                videoPreRecyclerView.setIntercept(!isIntercept);
                dividingView.setLastPointPadding(dividingView.getPointPadding());
                totalZoomWidth = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (ev.getPointerCount() == 2 && dividingView.getPointPadding() <=DividingView.maxPadding * 5 && dividingView.getPointPadding() >=0){
                    float tempDoubleDistance = getDoubleFingerDistance(ev);
                    //数字刻度
                    if (tempDoubleDistance - doublePointDistance>0){
                        //拉长
                        dividingView.setPointPadding((int) ((tempDoubleDistance - doublePointDistance)*0.3 + dividingView.getLastPointPadding()));
                    }else {
                        //缩短
                        dividingView.setPointPadding((int) ((tempDoubleDistance - doublePointDistance)*0.3 + dividingView.getLastPointPadding()));
                    }
                    ViewGroup.LayoutParams layoutParams = dividingView.getLayoutParams();
                    layoutParams.width = layoutParams.width + dividingView.getPointPadding()*dividingView.getVideoPic();
                    dividingView.setLayoutParams(layoutParams);
                    maxWidth = layoutParams.width - 2*recyclerViewLeftPaddin;
                    dividingView.invalidate();


                    bitmapList.add(bitmapList.get(bitmapList.size() - 1));
                    adapter.notifyDataSetChanged();
                    linearLayoutManager.setMaxWidth(1000);
                    videoPreRecyclerView.requestLayout();


                    //视频预览条
                    //zoomVideoPreList();

                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = true;
                videoPreRecyclerView.setIntercept(!isIntercept);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = false;
                videoPreRecyclerView.setIntercept(!isIntercept);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        /*if (isIntercept){
            return isIntercept;
        }*/

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = false;
                videoPreRecyclerView.setIntercept(!isIntercept);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
       /* if (isIntercept){
            return isIntercept;
        }*/
        return  super.onTouchEvent(event);
    }


    private void zoomVideoPreList(){
        int i = 1;
        for (;i<bitmapList.size();i++){
            VideoBitmapBean next = bitmapList.get(i);
            long currentDur = (long) (((float)(itemWidth *(i+1))) / maxWidth * videoDuration);
            if (next.getPresentationTimeUs()/1000 - currentDur > (videoDuration/bitmapList.size())*0.5){
                Log.e("kzg","**********************i:"+i + "  ,next:"+next.getPresentationTimeUs() + "  ,currentDur:" + currentDur + "  ,bitmapList.size():"+bitmapList.size());
                VideoBitmapBean bitmapBean = new VideoBitmapBean(next.getBitmap(),currentDur * 1000);
                bitmapList.add(i,bitmapBean);
                adapter.notifyDataSetChanged();
                break;
            }
        }
        if (i<bitmapList.size() - 1){
            zoomVideoPreList();
        }
    }

    /**
     * 计算零个手指间的距离
     * @param event
     * @return
     */
    public static float  getDoubleFingerDistance(MotionEvent event){
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return  (float)Math.sqrt(x * x + y * y) ;
    }

}
