//
// Created by destroyer on 2022/4/5.
//



#include "log.h"
#include "JavaCallHelper.h"
#include "KzgPlayerStatus.h"
#include "pthread.h"

extern "C"{
#include "include/libavutil/time.h"
#include "include/libavformat/avformat.h"
};

class FAvFrameHelper {

public:
    FAvFrameHelper(JavaCallHelper *_helper,const char *_url,KzgPlayerStatus *kzgPlayerStatus);
    ~FAvFrameHelper();

    void init();
    void decodeAVPackate();
    void seekTo(int64_t sec);
    void starDecode();
    void releas();

public:
    KzgPlayerStatus *playerStatus;
    bool isExit = false;

private:
    JavaCallHelper *helper;
    const char *url;

    pthread_mutex_t init_mutex;
    pthread_mutex_t frame_mutex;
    pthread_t decodeAvPacketThread;
    AVFormatContext *avFormatContext;
    int avStreamIndex = -1;
    AVRational time_base;


};

