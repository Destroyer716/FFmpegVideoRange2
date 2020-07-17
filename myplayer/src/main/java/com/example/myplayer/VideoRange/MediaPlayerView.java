package com.example.myplayer.VideoRange;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;


import com.example.myplayer.R;

import java.text.SimpleDateFormat;

/**
 * 动态详情视频播放控件
 */
public class MediaPlayerView extends LinearLayout implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {

    //播放控件
    private VideoView videoView;

    //暂停/播放按钮
    private ImageView statusIV;

    //当前播放时间
    private TextView currentTV;

    //总共播放时间
    private TextView totalTV;

    //进度条
    private SeekBar seekBar;
    private View mMaskLayout;

    //是否拖动了进度条
    private boolean isStartTrackingTouch;

    //是否处于暂停状态
    private boolean isPause = false;

    //是否正在播放
    private boolean isPlaying = false;

    //用于传递播放进度
    private Handler handler = new Handler();

    private Context context;
    private MediaPlayListener mMediaPlayListener;
    private int videoPos = 0;


    //监听播放进度的线程
    private Runnable videoProgressThread = new Runnable() {
        int buffer, currentPosition, duration;

        public void run() {
            // 获得当前播放时间和当前视频的长度
            currentPosition = videoView.getCurrentPosition();
            duration = videoView.getDuration();
            // 设置进度条的主要进度
            seekBar.setProgress(currentPosition);
            //进度条总长度
            seekBar.setMax(duration);
            //文字总长度
            totalTV.setText(getTime(duration));
            //文字当前进度
            currentTV.setText(getTime(currentPosition));
            // 设置进度条的次要进度，表示视频的缓冲进度
            buffer = videoView.getBufferPercentage();
            seekBar.setSecondaryProgress(buffer);


            handler.postDelayed(videoProgressThread, 500);
        }
    };

    public MediaPlayerView(Context context) {
        this(context, null);
    }

    public MediaPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1){
                mMaskLayout.setVisibility(GONE);
            }else{
                mMaskLayout.setVisibility(VISIBLE);
                mHandler.removeMessages(1);
                mHandler.sendEmptyMessageDelayed(1,2000);
            }
        }
    };


    private void initView(Context context) {
        this.context = context;
        inflate(context, R.layout.view_media_player, this);
        videoView = (VideoView) findViewById(R.id.video_view);
        statusIV = (ImageView) findViewById(R.id.iv_status);
        currentTV = (TextView) findViewById(R.id.tv_current);
        totalTV = (TextView) findViewById(R.id.tv_total);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        mMaskLayout = findViewById(R.id.video_mask_layout);
        Log.e("kzg","**********************initView");
    }

    private void initAction() {
        statusIV.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        findViewById(R.id.video_mask_frame_layout).setOnClickListener(this);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        //FixMe 获取视频资源的宽度
                        int mVideoWidth = mp.getVideoWidth();
                        //FixMe 获取视频资源的高度
                        int mVideoHeight = mp.getVideoHeight();
                        if (mMediaPlayListener != null){
                            mMediaPlayListener.onLoadVideoSize(mVideoWidth,mVideoHeight);
                        }
                    }
                });
            }
        });
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.e("kzg","**********************setOnInfoListener：" + what);
                switch (what){
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        Log.e("kzg","**********************开始加载");
                        break;
                    
                }
                return false;
            }
        });
    }

    public void setResource(String uri) {
        String url;
        if (uri.startsWith("/storage")) {
            url = uri;
        }else{
            if (!uri.startsWith("http")) {
                url =  uri;
            }else{
                url = uri;
            }
        }

        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        Uri mUri = Uri.parse(url);
        videoView.setVideoURI(mUri);
        videoView.start();
        videoView.pause();
        initAction();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_status) {
            if (videoView.isPlaying()) {
                pause();
            } else {
                play();
            }
        } else if (id == R.id.video_mask_frame_layout) {
            mHandler.sendEmptyMessage(mMaskLayout.getVisibility() == VISIBLE ? 1 : 0);
        }
    }

    public void play() {
        Log.e("kzg","**********************MediaPlayView---play");
        if (isPause){
            videoView.seekTo(videoPos);
        }
        isPause = false;
        isPlaying = true;
        videoView.start();
        statusIV.setSelected(true);
        if (mMediaPlayListener != null) {
            mMediaPlayListener.onPlay();
        }
        mHandler.sendEmptyMessage(0);
        handler.postDelayed(videoProgressThread, 500);
    }

    public void pause() {
        videoPos = videoView.getCurrentPosition();
        isPause = true;
        isPlaying = false;
        videoView.pause();
        statusIV.setSelected(false);
        Log.e("kzg","**********************MediaPlayView---pause");
    }

    public boolean isPlaying() {
        return videoView.isPlaying();
    }

    public boolean isPause() {
        return isPause;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.GONE) {
            videoView.stopPlayback();
            videoView.setFocusable(false);
            videoView.setFocusableInTouchMode(false);
            videoView.clearFocus();
        }
    }

    public void relaseResource() {
        if (videoView != null) {
            videoView.stopPlayback();
        }
        if (handler != null){
            handler.removeCallbacksAndMessages(null);
        }
    }

    private String getTime(long time) {

        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");//初始化Formatter的转换格式。
        String hms = formatter.format(time);
        return hms;
    }

    //移动触发
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.e("kzg","*******************progress:"+progress);
        if (fromUser) {
            videoView.seekTo(progress);
            currentTV.setText(getTime(videoView.getCurrentPosition()));
        }
    }

    //起始触发
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

        isStartTrackingTouch = true;
    }


    //结束触发
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.e("kzg","*******************seekBar.getProgress():"+seekBar.getProgress());
        videoView.seekTo(seekBar.getProgress());
        isStartTrackingTouch = false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

//        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//            int currentPosition, duration;
//
//            public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                // 获得当前播放时间和当前视频的长度
//                Log.i("taoran", "onBufferingUpdate");
//                //当前进度
//                currentPosition = videoView.getCurrentPosition();
//
//                //总进度
//                duration = videoView.getDuration();
//
//                //文字总长度
//                totalTV.setText(getTime(duration));
//
//                //文字当前进度
//                currentTV.setText(getTime(currentPosition));
//
//                //进度条总长度
//                seekBar.setMax(duration);
//
//                //进度条当前长度
//                seekBar.setProgress(currentPosition);
//
//                //缓冲长度
//                seekBar.setSecondaryProgress(percent);
//            }
//        });

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        videoPos = 0;
        seekBar.setProgress(0);
        videoView.seekTo(0);
        pause();
        if (mMediaPlayListener != null) {
            mMediaPlayListener.onPlayComplete();
        }
        handler.removeCallbacks(videoProgressThread);
    }

    public void setMediaPlayListener(MediaPlayListener mMediaPlayListener) {
        this.mMediaPlayListener = mMediaPlayListener;
    }

    public interface MediaPlayListener {
        void onPlayComplete();

        void onPlay();

        void onLoadVideoSize(int width, int height);
    }

    public VideoView getVideoView(){
        return videoView;
    }




}
