//
// Created by Administrator on 2020/5/10.
//

#include "SafeQueue.h"

SafeQueue::SafeQueue(KzgPlayerStatus *playerStatus1) {

    this->playerStatus = playerStatus1;
    pthread_mutex_init(&mutexPacket,NULL);
    pthread_cond_init(&condPacket,NULL);
}

SafeQueue::~SafeQueue() {
    clearAvPacket();
    pthread_mutex_destroy(&mutexPacket);
    pthread_cond_destroy(&condPacket);
}

int SafeQueue::putAvPacket(AVPacket *avPacket) {
    pthread_mutex_lock(&mutexPacket);

    queuePacket.push(avPacket);
    //LOGE("向列队插入一个packet 当前总共 %d 个",queuePacket.size());
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);

    return 0;
}

int SafeQueue::getAvPacket(AVPacket *avPacket) {
    if (playerStatus != NULL && !playerStatus->exit){
        pthread_mutex_lock(&mutexPacket);
        while (playerStatus != NULL && !playerStatus->exit){
            if (queuePacket.size() > 0){
                AVPacket * packet1 = queuePacket.front();
                int ret = av_packet_ref(avPacket,packet1);
                if (ret == 0){
                    queuePacket.pop();
                }
                av_packet_free(&packet1);
                av_free(packet1);
                packet1 = NULL;
                //LOGE("取出一个packet  当前剩余 %d 个",queuePacket.size());
                break;
            } else{
                if (playerStatus != NULL && !playerStatus->exit){
                    pthread_cond_wait(&condPacket,&mutexPacket);

                }
            }
        }
        pthread_mutex_unlock(&mutexPacket);
    }

    return 0;
}

int SafeQueue::getQueueSize() {
    pthread_mutex_lock(&mutexPacket);
    int size = queuePacket.size();
    pthread_mutex_unlock(&mutexPacket);
    return size;
}

void SafeQueue::clearAvPacket() {
    pthread_cond_signal(&condPacket);
    pthread_mutex_lock(&mutexPacket);
    while (!queuePacket.empty()){
        AVPacket * avPacket = queuePacket.front();
        queuePacket.pop();
        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;
    }
    pthread_mutex_unlock(&mutexPacket);

}

void SafeQueue::noticeQueue() {
    pthread_cond_signal(&condPacket);
}

void SafeQueue::clearByBeforeTime(int64_t time,AVRational time_base) {
    pthread_mutex_lock(&mutexPacket);
    LOGE("clearByBeforeTime:%d",queuePacket.size());
    while (queuePacket.size() > 50){
        AVPacket * avPacket = queuePacket.front();
        LOGE("   audio  pts : %lf",avPacket->pts * av_q2d(time_base));
        if (avPacket->pts * av_q2d(time_base) < time){
            queuePacket.pop();
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
        } else{
            break;
        }

    }
    pthread_mutex_unlock(&mutexPacket);
}

double SafeQueue::getMaxPts() {
    pthread_mutex_lock(&mutexPacket);
    double pts = 0;
    while (!queuePacket.empty()){
        AVPacket * avPacket = queuePacket.front();
        if (avPacket->pts > pts){
            pts = avPacket->pts;
        }
    }
    pthread_mutex_unlock(&mutexPacket);
    return pts;
}

