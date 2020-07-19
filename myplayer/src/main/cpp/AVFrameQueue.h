//
// Created by Administrator on 2020/6/7.
//

#ifndef MYCSDNVIDEOPLAYER_AVFRAMEQUEUE_H
#define MYCSDNVIDEOPLAYER_AVFRAMEQUEUE_H


#include "KzgPlayerStatus.h"
#include "queue"
#include "pthread.h"
#include "log.h"
extern "C"{
#include "include/libavcodec/avcodec.h"
};


typedef struct frameStruct{
    uint8_t *yData;
    uint8_t *uData;
    uint8_t *vData;
    double pts;
};


class AVFrameQueue {
public:
    AVFrameQueue(KzgPlayerStatus *playerStatus1);
    ~AVFrameQueue();

    int putAvFrame(AVFrame *avFrame);
    int putStructAvFrame(frameStruct *structFrame);
    int getAvFrame(AVFrame *avFrame,double timestamp,AVRational bastTime);
    int getStructAvFrame(frameStruct **structFrame,double timestamp,AVRational bastTime);
    int getQueueSize();
    void clearAvPacket();
    void noticeQueue();

public:
    std::queue<AVFrame *> queuePacket;
    std::queue<frameStruct *> structQueuePacket;
    pthread_cond_t condPacket;
    pthread_mutex_t mutexPacket;
    KzgPlayerStatus *playerStatus = NULL;
};


#endif //MYCSDNVIDEOPLAYER_AVFRAMEQUEUE_H
