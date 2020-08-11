//
// Created by Administrator on 2020/6/7.
//

#include "AVFrameQueue.h"

AVFrameQueue::AVFrameQueue(KzgPlayerStatus *playerStatus1) {
    this->playerStatus = playerStatus1;
    pthread_mutex_init(&mutexPacket,NULL);
    pthread_cond_init(&condPacket,NULL);
}

AVFrameQueue::~AVFrameQueue() {
    LOGE("AVFrameQueue  析构");
    clearAvPacket();
    pthread_mutex_destroy(&mutexPacket);
    pthread_cond_destroy(&condPacket);
}

int AVFrameQueue::putAvFrame(AVFrame *avFrame) {
    pthread_mutex_lock(&mutexPacket);

    queuePacket.push(avFrame);
    //LOGE("向列队插入一个packet 当前总共 %d 个",queuePacket.size());
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);

    return 0;
}

int AVFrameQueue::getAvFrameByTime(AVFrame *avFrame,double timestamp,AVRational bastTime) {
    pthread_mutex_lock(&mutexPacket);
    while (playerStatus != NULL && !playerStatus->exit){
        if (queuePacket.size() > 0){
            AVFrame * frame = queuePacket.front();
            int ret = av_frame_ref(avFrame,frame);
            double pts = avFrame->pts;
            pts *= av_q2d(bastTime);
            if (ret == 0 && timestamp >= (pts - 0.03)){
                queuePacket.pop();
                if (this->playerStatus->isCrop){
                    if(frame->data != NULL){
                        if(frame->data[0] != NULL){
                            free(frame->data[0]);
                        }
                        if(frame->data[1] != NULL){
                            free(frame->data[1]);
                        }
                        if(frame->data[2] != NULL){
                            free(frame->data[2]);
                        }
                    }
                }

                av_frame_free(&frame);
                av_free(frame);
                frame = NULL;
            } else if (frame->data[0] != NULL&& timestamp >= (pts - 0.03)){
                memcpy(avFrame->data,frame->data, sizeof(frame->data));
                queuePacket.pop();

                /*if(frame->data != NULL){
                    if(frame->data[0] != NULL){
                        free(frame->data[0]);
                    }
                    if(frame->data[1] != NULL){
                        free(frame->data[1]);
                    }
                    if(frame->data[2] != NULL){
                        free(frame->data[2]);
                    }
                }*/
                av_frame_free(&frame);
                av_free(frame);
                frame = NULL;
            }
            frame = NULL;
            //LOGE("取出一个packet  当前剩余 %d 个",queuePacket.size());
            break;
        } else{
            pthread_cond_wait(&condPacket,&mutexPacket);
        }
    }
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

int AVFrameQueue::getAvFrame(AVFrame *avFrame) {
    pthread_mutex_lock(&mutexPacket);
    while (playerStatus != NULL && !playerStatus->exit){
        if (queuePacket.size() > 0){
            AVFrame * avFrame1 = queuePacket.front();
            int ret = av_frame_ref(avFrame,avFrame1);
            avFrame->linesize[0] = avFrame1->linesize[0];
            if (ret == 0){
                queuePacket.pop();
            }
            av_frame_free(&avFrame1);
            av_free(avFrame1);
            avFrame1 = NULL;
            break;
        } else{
            pthread_cond_wait(&condPacket,&mutexPacket);
        }
    }
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}




int AVFrameQueue::getQueueSize() {
    pthread_mutex_lock(&mutexPacket);
    int size = queuePacket.size() + structQueuePacket.size();
    pthread_mutex_unlock(&mutexPacket);
    return size;
}

void AVFrameQueue::clearAvPacket() {
    pthread_cond_signal(&condPacket);
    pthread_mutex_lock(&mutexPacket);
    while (!queuePacket.empty()){
        LOGE("AVFrameQueue  release   isCrop：%d",this->playerStatus->isCrop);
        AVFrame * avFrame = queuePacket.front();
        queuePacket.pop();
        if (this->playerStatus->isCrop){
            if(avFrame->data != NULL){
                if(avFrame->data[0] != NULL){
                    free(avFrame->data[0]);
                }
                if(avFrame->data[1] != NULL){
                    free(avFrame->data[1]);
                }
                if(avFrame->data[2] != NULL){
                    free(avFrame->data[2]);
                }
            }
        }


        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = NULL;
    }

    while (!structQueuePacket.empty()){
        frameStruct * avFrame = structQueuePacket.front();
        structQueuePacket.pop();
        avFrame = NULL;
    }
    pthread_mutex_unlock(&mutexPacket);
}

void AVFrameQueue::noticeQueue() {
    pthread_cond_signal(&condPacket);
}

int AVFrameQueue::putStructAvFrame(frameStruct *structFrame) {
    pthread_mutex_lock(&mutexPacket);

    structQueuePacket.push(structFrame);
    //LOGE("向列队插入一个packet 当前总共 %d 个",queuePacket.size());
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);

    return 0;
}

int
AVFrameQueue::getStructAvFrame(frameStruct **structFrame, double timestamp, AVRational bastTime) {
    pthread_mutex_lock(&mutexPacket);
    while (playerStatus != NULL && !playerStatus->exit){
        if (structQueuePacket.size() > 0){
            frameStruct * frame = structQueuePacket.front();
            double pts = frame->pts;
            if (timestamp >= (pts - 0.03)){
                *structFrame = frame;
                structQueuePacket.pop();
            }

            //LOGE("取出一个packet  当前剩余 %d 个",queuePacket.size());
            break;
        } else{
            pthread_cond_wait(&condPacket,&mutexPacket);
        }
    }
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

