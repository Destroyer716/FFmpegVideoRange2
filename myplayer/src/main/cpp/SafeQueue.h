//
// Created by Administrator on 2020/5/10.
//

#ifndef MYCSDNMUSICPLAYER_SAFEQUEUE_H
#define MYCSDNMUSICPLAYER_SAFEQUEUE_H

#include "queue"
#include "pthread.h"
#include "KzgPlayerStatus.h"
#include "log.h"

extern "C"{
#include "include/libavcodec/avcodec.h"
};


class SafeQueue {

public:
    SafeQueue(KzgPlayerStatus *playerStatus1);
    ~SafeQueue();

    int putAvPacket(AVPacket *avPacket);
    int getAvPacket(AVPacket *avPacket);
    int getQueueSize();
    void clearAvPacket();
    void noticeQueue();
    void clearByBeforeTime(int64_t time,AVRational time_base);
    double getMaxPts();

public:
    std::queue<AVPacket *> queuePacket;
    pthread_cond_t condPacket;
    pthread_mutex_t mutexPacket;
    KzgPlayerStatus *playerStatus = NULL;


};


#endif //MYCSDNMUSICPLAYER_SAFEQUEUE_H
