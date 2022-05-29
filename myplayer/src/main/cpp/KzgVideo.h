//
// Created by Administrator on 2020/5/29.
//

#ifndef MYCSDNVIDEOPLAYER_KZGVIDEO_H
#define MYCSDNVIDEOPLAYER_KZGVIDEO_H

#include "KzgPlayerStatus.h"
#include "JavaCallHelper.h"
#include "SafeQueue.h"
#include "KzgAudio.h"
#include "AVFrameQueue.h"

extern "C"{
#include "include/libavutil/time.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/imgutils.h"
#include "include/libswscale/swscale.h"
};

#define CODEC_YUV 0
#define CODEC_MEDIACODEC 1

class KzgVideo {

public:
    int streamIndex = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *avCodecParameters = NULL;
    KzgPlayerStatus *kzgPlayerStatus = NULL;
    JavaCallHelper *helper = NULL;
    SafeQueue *queue = NULL;
    SafeQueue *tempqueue = NULL;
    AVFrameQueue *frameQueue = NULL;
    AVRational time_base;
    KzgAudio *kzgAudio = NULL;

    pthread_t play_thread = NULL;
    double lock = 0;
    double delayTime = 0;
    double defaultDelayTime = 0.04;
    int fps;
    pthread_mutex_t codecMutex;
    pthread_mutex_t showFrameMutex;
    int codectype = CODEC_YUV;
    AVBSFContext *avbsfContext = NULL;
    int64_t duration = 0;
    int64_t seekTime = 0;
    int64_t lastPFramePTS = 0;

    long startSeekTime = 0;
    double showFrameTimestamp = 0;//拖动预览条后当前显示的帧的时间
    //队列中缓存的帧的数量
    int cacheFrameNum = 60;


public:
    KzgVideo(KzgPlayerStatus *kzgPlayerStatus,JavaCallHelper *helper);
    ~KzgVideo();
    void play();
    void release();
    //计算音频当前播放的帧的时间与当前视频播放的帧的时间差,如果返回正数就是视频比音频慢，如果返回负数就是视频比音频快
    double getFrameDiffTime(AVFrame *avFrame,AVPacket *avPacket);
    //获取视频因该延迟的时间
    double getDelayTime(double diff);

    double myGetDelayTime(AVFrame *avFrame);
    void setIsFramePreview(bool isFramePreview);
    void showFrame(double timestamp);

};


#endif //MYCSDNVIDEOPLAYER_KZGVIDEO_H
