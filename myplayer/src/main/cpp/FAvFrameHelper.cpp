//
// Created by destroyer on 2022/4/5.
//

#include "FAvFrameHelper.h"

FAvFrameHelper::FAvFrameHelper(JavaCallHelper *_helper, const char *_url,
                               KzgPlayerStatus *_kzgPlayerStatus) {
    helper = _helper;
    url = _url;
    playerStatus = _kzgPlayerStatus;
    pthread_mutex_init(&init_mutex,NULL);
    pthread_mutex_init(&frame_mutex,NULL);

}

FAvFrameHelper::~FAvFrameHelper() {
    pthread_mutex_destroy(&init_mutex);
    pthread_mutex_destroy(&frame_mutex);
}

void* t_decode_avpacke(void *args){
    FAvFrameHelper *fAvFrameHelper = static_cast<FAvFrameHelper*>(args);
    fAvFrameHelper->decodeAVPackate();
    return 0;
}

void FAvFrameHelper::init() {
    if (playerStatus != NULL && !playerStatus->exit){
        pthread_create(&decodeAvPacketThread,NULL,t_decode_avpacke,this);
    }

}
int avformat_ff_callback(void *ctx){
    FAvFrameHelper *fFmpeg = (FAvFrameHelper *) ctx;
    if(fFmpeg->playerStatus->exit)
    {
        return AVERROR_EOF;
    }
    return 0;
}

void FAvFrameHelper::decodeAVPackate() {
    pthread_mutex_lock(&init_mutex);
    av_register_all();

    avFormatContext = avformat_alloc_context();
    avFormatContext->interrupt_callback.callback = avformat_ff_callback;
    avFormatContext->interrupt_callback.opaque = this;
    int ret;
    ret = avformat_open_input(&avFormatContext,url,0,0);
    if (ret != 0){
        LOGE("打开视频输入流失败");
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }

    ret = avformat_find_stream_info(avFormatContext,0);
    if (ret != 0){
        LOGE("查找视频流失败");
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }

    ret = -1;
    for (int i = 0; i <avFormatContext->nb_streams; ++i) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO){
            //找到视频流
            ret = 0;
            avStreamIndex = i;
            time_base = avFormatContext->streams[i]->time_base;
            break;
        }
    }

    if (ret != 0){
        LOGE("没有找到视频流");
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }

    helper->onGetFrameInitSuccess(THREAD_CHILD);
    pthread_mutex_unlock(&init_mutex);
}




int getAvPacketRefType2(AVPacket *pAvPkt){
    unsigned char *pData = pAvPkt->data; /* 帧数据 */
    unsigned char *pEnd = NULL;
    int dataSize = pAvPkt->size; /* pAvPkt->data的数据量 */
    int curSize = 0;
    int naluSize = 0;
    int i;
    unsigned char nalHeader, nalType,refType;

    while(curSize < dataSize){
        if(pEnd-pData < 4)
            goto fail;

        /* 前四个字节表示当前NALU的大小 */
        for(i = 0; i < 4; i++){
            naluSize <<= 8;
            naluSize |= pData[i];
        }

        pData += 4;

        if(naluSize > (pEnd-pData+1) || naluSize <= 0){
            goto fail;
        }

        nalHeader = *pData;
        nalType = nalHeader&0x1F;
        refType = (nalHeader >> 5 )&0x03;
        LOGE("nalHeader:%d  ,nalType:%d, ref:%d",nalHeader,nalType,refType);
        if(nalType == 5){
            //IDR帧
            return 1;
        } else if (nalType == 1){
            //I,P,B帧
            return refType;
        } else{
            return 1;
        }
    }

    fail:
    return 0;
}


void FAvFrameHelper::starDecode() {
    LOGE("开始解码抽帧");
    int ret;
    int count = 0;
    while (playerStatus != NULL && !playerStatus->exit){
        if (count >= 90){
            av_usleep(1000*10);
            continue;
        }
        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(avFormatContext,avPacket);
        if (ret != 0){
            LOGE("获取视频avPacket 失败");
            return;
        }

        if (avPacket->stream_index == avStreamIndex){
            //找到视频avPacket
            getAvPacketRefType2(avPacket);
        }

        count ++;
    }

    isExit = true;
}

void FAvFrameHelper::releas() {
    LOGE("开始释放FAvFrameHelper");
    playerStatus->exit = true;
    pthread_join(decodeAvPacketThread,NULL);

    pthread_mutex_lock(&init_mutex);
    int sleep_count = 0;
    while (!isExit){
        if (sleep_count > 1000){
            isExit = true;
        }
        LOGE("wait ffmpeg  exit %d", sleep_count);
        sleep_count ++;
        av_usleep(1000*10);//10毫秒
    }

    if(avFormatContext != NULL){
        LOGE("释放avFormatContext");
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }

    if (helper != NULL){
        LOGE("释放helper");
        helper = NULL;
    }

    if (playerStatus != NULL){
        LOGE("释放kzgPlayerStatus");
        playerStatus = NULL;
    }

    pthread_mutex_unlock(&init_mutex);
}
