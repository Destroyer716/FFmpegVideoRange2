package com.example.myplayer.VideoRange;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


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

    //预览图随播放移动的百分比间隔
    public static final float SCROLL_DELAY_PERCENT = 0.001f;
    //上次预览图移动的播放百分比
    private float lastScrollPercent = 0;


    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);
    private ThreadPoolExecutor threadPoolExecutor;

    private Context mContext;
    private VideoTrackView videoTrackView;
    private DividingView dividingView;
    //当前预览位置的线
    private View currentPreLine;
    private VideoRangeHorizontalScrollView videoScrollView;


    private FFmpegMediaMetadataRetriever fmmr;
    private VideoToFrames videoToFrames;

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
    //初始化时预览条的宽度，未进行缩放的话就与maxWidth相等
    private int defaultWidth;
    private ScaleGestureDetector mScaleGestureDetector;



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
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), onScaleGestureListener);
        videoTrackView = inflate.findViewById(R.id.vt_video_track);
        dividingView = inflate.findViewById(R.id.dv_kedu);
        currentPreLine = inflate.findViewById(R.id.v_current_line);
        videoScrollView = inflate.findViewById(R.id.hsv_scroll_view);
        recyclerViewLeftPaddin = getScreenWidth()/2;
        videoTrackView.setPadding(recyclerViewLeftPaddin,videoTrackView.getTop(),videoTrackView.getRight(),videoTrackView.getBottom());


        videoScrollView.setOnScrollChangedListener(new VideoRangeHorizontalScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(int x, int y, int oldx, int oldy) {
                long l = System.currentTimeMillis();

                int changeX = (x - scrollCount);
                scrollCount = x;
                //dividingView.scrollBy((x - scrollCount),0);
                //dividingView.setChange(false);

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
                    //旧的计算滑动所在的时间戳
                    /*int scrollTime = scrollCount / itemWidth; //获取秒数
                    millTime = (int) (((float)scrollCount % (float)itemWidth)/itemWidth * 1000) + scrollTime * 1000;*/

                    double percent = ((double) scrollCount) / maxWidth;
                    millTime = (int) (percent * videoDuration);
                }
                if (player != null){
                    if (changeX > 0){
                        player.showFrame(millTime / 1000.0,KzgPlayer.seek_advance);
                    }else if (changeX < 0){
                        player.showFrame(millTime / 1000.0,KzgPlayer.seek_back);
                    }
                    if (videoRangeViewListener != null){
                        videoRangeViewListener.scrollTimestamp(millTime);
                    }
                }
                if (videoRangeViewListener != null && (millTime - lastShowTime)>=fps ){
                    videoRangeViewListener.onScrollerX(millTime, null );
                    lastShowTime = millTime;
                }
            }
        });

        //预览条随着刻度缩放
        dividingView.setOnRequestLayoutListener(new DividingView.OnRequestLayoutListener() {
            @Override
            public void onLayout() {
                if (isDoublePoint){

                    //计算新的预览条宽度，并计算应该增加几张图片
                    maxWidth = defaultWidth + dividingView.getTotalPadding();
                    dividingView.setMaxWidth(maxWidth);

                    int currentPicNum = maxWidth /itemWidth;
                    if (maxWidth %itemWidth > 0){
                        currentPicNum += 1;
                    }
                    //视频预览条
                    int addNum = currentPicNum - bitmapList.size();
                    if(addNum > 0){
                        //拉伸
                        long itemMillTime = videoDuration / bitmapList.size();
                        stretchVideoPreList(itemMillTime,addNum);
                    }else if (addNum < 0){
                        //缩短
                        addNum = Math.abs(addNum);
                        for (int i=0;i<addNum;i++){
                            long itemMillTime = videoDuration / (bitmapList.size() - 1);
                            shortenVideoPreList(itemMillTime);
                        }
                        videoTrackView.updateData(bitmapList);
                    }

                    ViewGroup.LayoutParams layoutParamsVideoTrack = videoTrackView.getLayoutParams();
                    layoutParamsVideoTrack.width = maxWidth + recyclerViewLeftPaddin;
                    videoTrackView.setLayoutParams(layoutParamsVideoTrack);


                }
            }

            @Override
            public void onChangeWidth(int changeWidth) {

                //计算缩放前中线位置对应的百分比
                float percent = (float)(scrollCount) /maxWidth;
                //计算新的预览条宽度，并计算应该增加几张图片
                maxWidth = defaultWidth + changeWidth;
                //计算缩放后应该将预览条滚动多少才能保持中线所在的位置的比例不变
                int scrollX = (int) (percent * maxWidth);
                videoScrollView.scrollTo(scrollX,0);
                //videoScrollView.scrollBy(scrollX - scrollCount,0);
                Log.e("kzg","**********************scrollX:"+scrollX);
                dividingView.setMaxWidth(maxWidth);
            }
        });

        threadPoolExecutor = new ThreadPoolExecutor(5,8,3, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(50));
    }

    private void initRecyclerView(int countItm){


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
        if (videoRangeViewListener != null){
            videoRangeViewListener.onLoadSuccess(videoDuration);
        }
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
                            videoTrackView.addData(videoBitmapBean);
                            if (finalI == 1){
                                int with = videoTrackView.getItemPicWidth();
                                itemWidth = with-recyclerViewLeftPaddin;
                                maxWidth = itemWidth * duration;
                                defaultWidth = maxWidth;
                                dividingView.setDefaultWidth(defaultWidth);
                                dividingView.setMaxWidth(maxWidth);
                                dividingView.setChange(true);
                                dividingView.setVideoPicNum(duration);
                                dividingView.setLeftPaddin(recyclerViewLeftPaddin);
                                dividingView.setDrawAround(0);
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

                    if (i[0] >= VideoTrackView.maxPicNum){
                        videoToFrames.setStopDecode(true);
                        bitmap.recycle();
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
                                videoTrackView.setData(bitmapList);
                            }else {
                                VideoBitmapBean videoBitmapBean = new VideoBitmapBean(bitmap, usTime);
                                videoTrackView.updateData(videoBitmapBean,i[0]);
                                VideoBitmapBean bitmapBean = bitmapList.get(i[0]);
                                bitmapBean.setBitmap(bitmap);
                                bitmapBean.setPresentationTimeUs(usTime);
                            }
                            //adapter.addData(bitmap);
                            if (i[0] == 1){
                                /*int with = videoPreRecyclerView.computeHorizontalScrollRange();
                                itemWidth = with-recyclerViewLeftPaddin;*/
                                itemWidth = videoTrackView.getItemPicWidth();
                                Log.e("kzg","**********************itemWidth:"+itemWidth);
                                maxWidth = itemWidth * duration;
                                defaultWidth = maxWidth;
                                dividingView.setMaxWidth(maxWidth);
                                dividingView.setDefaultWidth(defaultWidth);
                                dividingView.setChange(true);
                                dividingView.setVideoPicNum(duration);
                                dividingView.setLeftPaddin(recyclerViewLeftPaddin);
                                dividingView.setDrawAround(0);
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
        if (videoTrackView != null){
            int scrollX = (int) (maxWidth * percent);
            videoScrollView.smoothScrollBy(scrollX - scrollCount,0);

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
        void onLoadSuccess(long totalTIme);
        void onScrollerX(int millTime, Bitmap bitmap);
        void scrollTimestamp(long timestamp);
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
       mScaleGestureDetector.onTouchEvent(ev);
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = true;
                videoScrollView.setIntercept(!isIntercept);
                doublePointDistance = getDoubleFingerDistance(ev);
                isDoublePoint = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = false;
                isDoublePoint = false;
                videoScrollView.setIntercept(!isIntercept);
                dividingView.setLastPointPadding(dividingView.getPointPadding());
                totalZoomWidth = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (ev.getPointerCount() == 2 /*&& dividingView.getPointPadding() < DividingView.maxPadding * 4 && dividingView.getPointPadding() >=0*/){
                    /*dividingView.setChange(true);
                    dividingView.setDrawAround(scrollCount);
                    float tempDoubleDistance = getDoubleFingerDistance(ev);
                    //数字刻度
                    if (tempDoubleDistance - doublePointDistance>0){
                        //拉长
                        dividingView.setPointPadding((int) ((tempDoubleDistance - doublePointDistance)*0.3 + dividingView.getLastPointPadding()));
                    }else {
                        //缩短
                        dividingView.setPointPadding((int) ((tempDoubleDistance - doublePointDistance)*0.3 + dividingView.getLastPointPadding()));
                    }
                    dividingView.invalidate();*/
                }else if (ev.getPointerCount() == 1){
                    dividingView.setDrawAround(0);
                    dividingView.setChange(false);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            dividingView.setDrawAround(scrollCount);
            dividingView.setScaleFactor(detector.getScaleFactor());
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };


    /* @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                isIntercept = true;
                videoTrackView.setIntercept(!isIntercept);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                isIntercept = false;
                videoTrackView.setIntercept(!isIntercept);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }


        return super.onInterceptTouchEvent(ev);
    }*/

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.e("kzg","**********************ACTION_DOWN");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //屏幕上已经有一个点按住 再按下一点时触发该事件
                Log.e("kzg","**********************ACTION_POINTER_DOWN");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //屏幕上已经有两个点按住 再松开一点时触发该事件
                Log.e("kzg","**********************ACTION_POINTER_UP");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("kzg","**********************ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return  true;
    }*/


    /**
     * 拉伸预览条
     * @param itemMillTime
     */
    private void stretchVideoPreList(long itemMillTime,int addNum){
        int i = 1;
        int added = 0;
        boolean isAdded = false;
        for (;i<bitmapList.size();i++){
            if (i>= VideoTrackView.maxPicNum){
                return;
            }
            VideoBitmapBean next = bitmapList.get(i);
            long currentDur = (long) (((float)(itemWidth *(i+1))) / maxWidth * videoDuration);
            if (next.getPresentationTimeUs()/1000 - currentDur > itemMillTime){
                final VideoBitmapBean bitmapBean = new VideoBitmapBean(next.getBitmap(),currentDur * 1000);
                bitmapList.add(i,bitmapBean);
                videoTrackView.addData(bitmapBean, i);
                added ++;
                if (added == addNum){
                    isAdded = true;
                    break;
                }
            }
        }
        if (!isAdded){
            //如果需要添加一帧图片，但是最后却没有添加，就主动添加一帧
            int index = bitmapList.size() * 2 / 3;
            VideoBitmapBean next = bitmapList.get(index);
            long currentDur = (long) (((float)(itemWidth *(index+1))) / maxWidth * videoDuration);
            VideoBitmapBean bitmapBean = new VideoBitmapBean(next.getBitmap(),currentDur * 1000);
            bitmapList.add(index,bitmapBean);
            videoTrackView.addData(bitmapBean,index);
        }
    }

    /**
     * 缩短预览条
     * @param itemMillTime
     */
    private void shortenVideoPreList(Long itemMillTime){
        int i = 0;
        Iterator<VideoBitmapBean> iterator = bitmapList.iterator();
        while (iterator.hasNext()){
            if (i >= VideoTrackView.maxPicNum){
                return;
            }
            if (i > 0){
                VideoBitmapBean next = iterator.next();
                long currentDur = (long) (((float)(itemWidth *(i+1))) / maxWidth * videoDuration);
                if (currentDur - next.getPresentationTimeUs()/1000 > itemMillTime){
                    iterator.remove();
                    //videoTrackView.remove(i);
                    break;
                }
            }
            i ++ ;
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

    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long start = System.currentTimeMillis();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.e("kzg","*************************onMeasure:"+(System.currentTimeMillis() - start));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        long start = System.currentTimeMillis();
        super.onLayout(changed, left, top, right, bottom);
        Log.e("kzg","*************************onLayout:"+(System.currentTimeMillis() - start));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long start = System.currentTimeMillis();
        super.onDraw(canvas);
        Log.e("kzg","*************************onDraw:"+(System.currentTimeMillis() - start));
    }*/


}
