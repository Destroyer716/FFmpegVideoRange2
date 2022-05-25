//
// Created by destroyer on 2022/4/5.
//



#include "log.h"
#include "JavaCallHelper.h"
#include "KzgPlayerStatus.h"
#include "pthread.h"
#include "SafeQueue.h"

extern "C"{
#include "include/libavutil/time.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/imgutils.h"
};

class FAvFrameHelper {

public:
    FAvFrameHelper(JavaCallHelper *_helper,const char *_url,KzgPlayerStatus *kzgPlayerStatus);
    ~FAvFrameHelper();

    void init();
    void decodeAVPackate();
    void seekTo(int64_t sec,bool isCurrentGop);
    void starDecode();
    void releas();
    int getAvCodecContent(AVCodecParameters *avCodecParameters,AVCodecContext **avCodecContext);
    void decodeAvPacket();
    void pauseOrStar(bool  isPause);
    void decodeFrame();
    void decodeFrameFromQueue();
    double getAvpacketQueueMaxPts();

public:
    KzgPlayerStatus *playerStatus;
    bool isExit = false;
    bool isPause = true;
    int64_t seekTime = 0;
    AVBitStreamFilterContext* mimType = NULL;

private:
    JavaCallHelper *helper;
    const char *url;

    pthread_mutex_t init_mutex;
    pthread_mutex_t frame_mutex;
    pthread_t decodeAvPacketThread;
    AVFormatContext *avFormatContext;
    int avStreamIndex = -1;
    AVRational time_base;
    AVCodecContext *avCodecContext = NULL;
    int64_t duration = 0;
    SafeQueue *queue = NULL;
    pthread_mutex_t codecMutex;


};

