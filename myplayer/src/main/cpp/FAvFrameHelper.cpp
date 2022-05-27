//
// Created by destroyer on 2022/4/5.
//

#include "FAvFrameHelper.h"

FAvFrameHelper::FAvFrameHelper(JavaCallHelper *_helper, const char *_url,
                               KzgPlayerStatus *_kzgPlayerStatus) {
    helper = _helper;
    url = _url;
    playerStatus = _kzgPlayerStatus;
    this->queue = new SafeQueue(_kzgPlayerStatus);
    pthread_mutex_init(&init_mutex,NULL);
    pthread_mutex_init(&frame_mutex,NULL);

    pthread_mutex_init(&codecMutex,NULL);

}

FAvFrameHelper::~FAvFrameHelper() {
    pthread_mutex_destroy(&init_mutex);
    pthread_mutex_destroy(&frame_mutex);
    pthread_mutex_destroy(&codecMutex);
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
            duration = avFormatContext->duration;
            LOGE("获取视频流成功");
            break;
        }
    }

    if (ret != 0){
        LOGE("没有找到视频流");
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }

    if (getAvCodecContent(avFormatContext->streams[avStreamIndex]->codecpar,&avCodecContext) != 0){
        LOGE("获取解码器信息失败");
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }

    helper->onGetFrameInitSuccess(avCodecContext->codec->name,
            avCodecContext->width,
            avCodecContext->height,
            avCodecContext->extradata_size,
            avCodecContext->extradata_size,
            avCodecContext->extradata,
            avCodecContext->extradata);
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
        //LOGE("nalHeader:%d  ,nalType:%d, ref:%d",nalHeader,nalType,refType);
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
        if (isPause){
            av_usleep(1000*10);
            continue;
        }
        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(avFormatContext,avPacket);
        if (ret != 0){
            LOGE("获取视频avPacket 失败");
            av_usleep(1000*100);
            continue;
        }

        //找到视频avPacket
        if (avPacket->stream_index == avStreamIndex){
            LOGE("seekTo sec3 %lld:", seekTime);
            getAvPacketRefType2(avPacket);
        }

        count ++;
    }
    LOGE("退出 抽帧解码线程");
    isExit = true;
}

void FAvFrameHelper::releas() {
    LOGE("开始释放FAvFrameHelper");
    playerStatus->exit = true;
    isPause = false;

    if (queue != NULL){
        queue->noticeQueue();
    }

    pthread_join(decodeAvPacketThread,NULL);

    pthread_mutex_lock(&init_mutex);
    int sleep_count = 0;
    while (!isExit){
        if (sleep_count > 1000){
            isExit = true;
        }
        //LOGE("wait ffmpeg  exit %d", sleep_count);
        sleep_count ++;
        av_usleep(1000*10);//10毫秒
    }

    if (queue != NULL){
        delete(queue);
        queue = NULL;
    }

    if(avFormatContext != NULL){
        LOGE("释放avFormatContext");
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }

    if (avCodecContext != NULL){
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }

    if (helper != NULL){
        LOGE("释放helper");
        helper = NULL;
    }

    if (playerStatus != NULL){
        LOGE("释放kzgPlayerStatus");
        playerStatus = NULL;
    }

    if(mimType != NULL){
        av_bitstream_filter_close(mimType);
    }

    pthread_mutex_unlock(&init_mutex);
    LOGE("释放FAvFrameHelper结束");
}

int FAvFrameHelper::getAvCodecContent(AVCodecParameters *avCodecParameters,
                                       AVCodecContext **avCodecContext) {int ret;
    AVCodec * avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    //AVCodec * avCodec = avcodec_find_decoder_by_name("h264_mediacodec");
    if (!avCodec){
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("find decoder fail %s:", url);
        helper->onError(1003,"find decoder fail",THREAD_CHILD);
        return -1;
    }


    *avCodecContext = avcodec_alloc_context3(avCodec);
    if (!*avCodecContext){
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("alloc codec context fail %s:", url);
        helper->onError(1004,"alloc codec context fail",THREAD_CHILD);
        return -1;
    }


    ret = avcodec_parameters_to_context(*avCodecContext,avCodecParameters);
    if (ret < 0){
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("avcodec_parameters_from_context fail %s:", url);
        helper->onError(1005,"avcodec_parameters_from_context fail",THREAD_CHILD);
        return -1;
    }
    (*avCodecContext)->thread_type = FF_THREAD_FRAME;
    (*avCodecContext)->thread_count = 8;

    ret = avcodec_open2(*avCodecContext,avCodec,0);
    if (ret != 0){
        isExit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("open audio codec fail %s:", url);
        helper->onError(1006,"open audio codec fail",THREAD_CHILD);
        return -1;
    }

    mimType =  av_bitstream_filter_init("h264_mp4toannexb");
    return 0;

}

void FAvFrameHelper::seekTo(int64_t sec,bool isCurrentGop) {
    LOGE("FAvFrameHelper  seekto:%lld",sec);

    if(duration <= 0){
        return;
    }

    if (isCurrentGop){
        int64_t res = sec/1000.0 * AV_TIME_BASE;
        LOGE("seekTo sec1  %lld， %lld:",sec, res);
        seekTime = res;
        isPause = false;
    } else{
        if (sec >= 0 && sec < duration){
            isPause = true;
            pthread_mutex_lock(&frame_mutex);
            queue->clearAvPacket();
            int64_t res = sec/1000.0 * AV_TIME_BASE;
            LOGE("seekTo sec1  %lld， %lld:",sec, res);
            seekTime = res;
            avformat_seek_file(avFormatContext,-1,INT64_MIN,res,INT64_MAX,0);
            pthread_mutex_lock(&codecMutex);
            avcodec_flush_buffers(avCodecContext);
            pthread_mutex_unlock(&codecMutex);
            isPause = false;
            //decodeAvPacket(res);
            pthread_mutex_unlock(&frame_mutex);
        }
    }

}

void FAvFrameHelper::decodeAvPacket() {
    int ret;
    LOGE("开始解码抽帧");
    while (playerStatus != NULL && !playerStatus->exit){
        if (isPause){
            av_usleep(1000*10);
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(avFormatContext,avPacket);
        if (ret != 0){
            LOGE("获取视频avPacket 失败");
            av_usleep(1000*10);
            continue;
        }

        if (avPacket->stream_index != avStreamIndex){
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
        } else if (avPacket->stream_index == avStreamIndex){
            //找到视频avPacket

            //LOGE("seekTo sec2 %f" , (avPacket->pts *av_q2d( time_base)* AV_TIME_BASE));
            if (getAvPacketRefType2(avPacket) > 0){
                uint8_t *data;
                av_bitstream_filter_filter(mimType, avFormatContext->streams[avStreamIndex]->codec, NULL, &data, &avPacket->size, avPacket->data, avPacket->size, 0);
                uint8_t *tdata = NULL;
                tdata = avPacket->data;
                avPacket->data = data;

                /*if (avPacket->flags & AV_PKT_FLAG_KEY){
                    LOGE("获取到关键帧：%f",(avPacket->pts *av_q2d( time_base)* AV_TIME_BASE));
                }*/

                if(tdata != NULL)
                {
                    av_free(tdata);
                }

                helper->onGetFramePacket(avPacket->size,(avPacket->pts *av_q2d( time_base)* AV_TIME_BASE),avPacket->data);
            } else{
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
            }

        } else{
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
        }
    }
    isExit = true;
    LOGE("抽帧结束");
}

void FAvFrameHelper::pauseOrStar(bool isPause) {
    LOGE("FAvFrameHelper  pauseOrStar %d",isPause);
    this->isPause = isPause;
}

void FAvFrameHelper::decodeFrame() {
    int ret;
    LOGE("decodeFrame 开始解码抽帧: %d",isPause);
    while (playerStatus != NULL && !playerStatus->exit){
        if (isPause || queue->getQueueSize() > 30){
            av_usleep(1000*10);
            continue;
        }


        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(avFormatContext,avPacket);
        if (ret != 0){
            LOGE("获取视频avPacket 失败");
            av_usleep(1000*10);
            continue;
        }

        if (avPacket->stream_index != avStreamIndex){
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        } else if (avPacket->stream_index == avStreamIndex){
            //找到视频avPacket
            LOGE("找到视频avPacket");
            if (getAvPacketRefType2(avPacket) > 0){
                LOGE("queue 增加AVpacket");
                queue->putAvPacket(avPacket);
            } else{
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
            }
        } else{
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
        }
    }
    isExit = true;
    LOGE("抽帧结束");
}

void FAvFrameHelper::decodeFrameFromQueue() {

    LOGE("avFrameHelper: %d",isPause);
    while (playerStatus != NULL && !playerStatus->exit){

        if (isPause){
            av_usleep(1000 * 10);
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        if (queue->getAvPacket(avPacket) != 0){
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            LOGE("avFrameHelper avframe errer 0");
            continue;
        }
        pthread_mutex_lock(&codecMutex);
        if (avcodec_send_packet(avCodecContext,avPacket) != 0){
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&codecMutex);
            LOGE("avFrameHelper avframe errer 1");
            continue;
        }

        AVFrame * avFrame = av_frame_alloc();
        if (avcodec_receive_frame(avCodecContext,avFrame) != 0){
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&codecMutex);
            LOGE("avFrameHelper avframe errer 2");
            continue;
        }

        if (avFrame->format == AV_PIX_FMT_YUV420P || avFrame->format == AV_PIX_FMT_YUVJ420P){
            avFrame->pts = av_frame_get_best_effort_timestamp(avFrame);
            int width = avFrame->linesize[0] > avCodecContext->width? avFrame->linesize[0]:avCodecContext->width;
            LOGE("avFrameHelper avframe yuv420p %d  ,%d",width,avCodecContext->width);
            helper->onCallYUVToBitmap(
                    width,
                    avCodecContext->height,
                    avFrame->data[0],
                    avFrame->data[1],
                    avFrame->data[2],
                    avCodecContext->width,
                    (avFrame->pts *av_q2d(time_base) * AV_TIME_BASE),
                    THREAD_CHILD);

            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
        } else{
            LOGE("avFrameHelper avframe 不是 yuv420p");
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
        }


        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;
        pthread_mutex_unlock(&codecMutex);
    }

    LOGE("avFrameHelper 解码frame结束");

}

double FAvFrameHelper::getAvpacketQueueMaxPts() {
    return queue->getMaxPts()*av_q2d(time_base) * AV_TIME_BASE;
}
