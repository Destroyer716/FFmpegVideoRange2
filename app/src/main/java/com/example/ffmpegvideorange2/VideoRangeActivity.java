package com.example.ffmpegvideorange2;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myplayer.KzgPlayer;
import com.example.myplayer.TimeInfoBean;
import com.example.myplayer.VideoRange.VideoRangeView;
import com.example.myplayer.opengl.KzgGLSurfaceView;
import com.wang.avi.AVLoadingIndicatorView;


public class VideoRangeActivity extends AppCompatActivity implements View.OnClickListener {

    String inputPath = Environment.getExternalStorageDirectory() + "/video5.mp4";

    private static final int PLAY_ENABLE_CHANGE_HANDLER = 1001;


    private KzgGLSurfaceView surfaceView;
    private VideoRangeView videoRangeView;
    private Paint paint;
    private KzgPlayer kzgPlayer;
    private RelativeLayout relativeLayout;
    private ImageView ivPlay;
    private AVLoadingIndicatorView avLoading;


    private Handler handler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case PLAY_ENABLE_CHANGE_HANDLER:
                    if (ivPlay != null && ivPlay.getVisibility() == View.GONE && (boolean)msg.obj){
                        ivPlay.setVisibility(View.VISIBLE);
                        avLoading.hide();
                    }
                    break;
            }


            return false;
        }
    });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_range);

        initView();
        initAction();
        inputPath = getIntent().getStringExtra("filePath");
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        init();
    }


    private void  initView(){
        relativeLayout = findViewById(R.id.rl_surface_view_parent);
        surfaceView = findViewById(R.id.sv_video_view);
        videoRangeView = findViewById(R.id.vrv_video_range);
        ivPlay = findViewById(R.id.iv_play_stop_video);
        avLoading = findViewById(R.id.av_loading);
    }


    private void initAction(){
        ivPlay.setOnClickListener(this);
    }

    private void init(){
        avLoading.show();
        kzgPlayer = new KzgPlayer();
        kzgPlayer.setKzgGLSurfaceView(surfaceView);
        videoRangeView.setPlayer(kzgPlayer);
        videoRangeView.setFilePath(inputPath);
        videoRangeView.setVideoRangeViewListener(new VideoRangeView.VideoRangeViewListener() {
            @Override
            public void onScrollerX(int millTime, Bitmap bitmap) {
                /*if (bitmap != null && surfaceHolder != null){
                    Canvas canvas = surfaceHolder.lockCanvas();  // 先锁定当前surfaceView的画布
                    canvas.drawBitmap(bitmap, 0, 0, paint); //执行绘制操作
                    surfaceHolder.unlockCanvasAndPost(canvas); // 解除锁定并显示在界面上
                    bitmap.recycle();
                }*/
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        begin();
                    }
                });
            }
        }).start();


    }

    public void begin() {
        kzgPlayer.setSource(inputPath);
        //kzgPlayer.setSource("/storage/emulated/0/嗜人之夜_1080P.x264.官方中文字幕.eng.chs.aac.mp4");
        kzgPlayer.setPlayModel(KzgPlayer.PLAY_MODEL_FRAME_PREVIEW);
        kzgPlayer.parpared();
        kzgPlayer.setPlayerListener(new KzgPlayer.PlayerListener() {
            @Override
            public void onError(int code,String msg) {
                Log.e("kzg","************************error:"+msg);
                avLoading.hide();
            }

            @Override
            public void onPrepare() {
                Log.e("kzg","*********************onPrepare success");
                kzgPlayer.start();
            }

            @Override
            public void onLoadChange(boolean isLoad) {
                if (isLoad){
                    Log.e("kzg","开始加载");
                }else {
                    Log.e("kzg","加载结束");
                }
            }

            @Override
            public void onProgress(long currentTime,long totalTime) {
                if (videoRangeView != null){
                    videoRangeView.setPlayPercent(((float) currentTime)/totalTime);
                }
            }

            @Override
            public void onTimeInfo(TimeInfoBean timeInfoBean) {
                //Log.e("kzg","*********************timeInfoBean:"+timeInfoBean);

            }

            @Override
            public void onEnablePlayChange(boolean enable) {
                if (handler != null){
                    Message message = new Message();
                    message.what = PLAY_ENABLE_CHANGE_HANDLER;
                    message.obj = enable;
                    handler.sendMessage(message);
                }
            }

            @Override
            public void onComplete() {
                Log.e("kzg","*********************onComplete:");
            }

            @Override
            public void onDB(int db) {
                //Log.e("kzg","**********************onDB:"+db);
            }

            @Override
            public void onGetVideoInfo(int fps, long duration, final int widht, final int height) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeSurfaceViewSize(widht,height);
                    }
                });

            }
        });
    }


    private void changeSurfaceViewSize(int widht,int height){
        int surfaceWidth = surfaceView.getWidth();
        int surfaceHeight = surfaceView.getHeight();

        //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        float max;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //竖屏模式下按视频宽度计算放大倍数值
            max = Math.max((float) widht / (float) surfaceWidth, (float) height / (float) surfaceHeight);
        } else {
            //横屏模式下按视频高度计算放大倍数值
            max = Math.max(((float) widht / (float) surfaceHeight), (float) height / (float) surfaceWidth);
        }

        //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        widht = (int) Math.ceil((float) widht / max);
        height = (int) Math.ceil((float) height / max);

        //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
        layoutParams.width = widht;
        layoutParams.height = height;
        /*RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(widht, height);
        params.addRule(RelativeLayout.CENTER_VERTICAL, relativeLayout.getId());*/
        surfaceView.setLayoutParams(layoutParams);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (kzgPlayer != null){
            kzgPlayer.stop();
            kzgPlayer.release();
            kzgPlayer.setPlayerListener(null);
            kzgPlayer = null;
        }
        if (videoRangeView != null){
            videoRangeView.release();
            videoRangeView = null;
        }
        if(surfaceView != null){
            surfaceView.removeCallbacks(null);
            surfaceView = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_play_stop_video:
                if (kzgPlayer == null){
                    return;
                }
                if (KzgPlayer.playModel == KzgPlayer.PLAY_MODEL_DEFAULT){
                    //停止
                    kzgPlayer.setPlayModel(KzgPlayer.PLAY_MODEL_FRAME_PREVIEW);
                    ivPlay.setImageResource(R.drawable.play_ico);
                }else if (KzgPlayer.playModel == KzgPlayer.PLAY_MODEL_FRAME_PREVIEW){
                    //播放
                    if (!kzgPlayer.enablePlay){
                        return;
                    }
                    kzgPlayer.setPlayModel(KzgPlayer.PLAY_MODEL_DEFAULT);
                    ivPlay.setImageResource(R.drawable.stop_ico);
                }
                break;
        }
    }
}
