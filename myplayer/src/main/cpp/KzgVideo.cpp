//
// Created by Administrator on 2020/5/29.
//

#include "KzgVideo.h"

#define AV_SYNC_THRESHOLD_MIN 0.04
#define AV_SYNC_THRESHOLD_MAX 0.1

/*typedef struct frameStruct{
    uint8_t *yData;
    uint8_t *uData;
    uint8_t *vData;
    double pts;
};*/

//FILE *yuvFile;
char *outputfilename = "/storage/emulated/0/yuvtest.yuv";


KzgVideo::KzgVideo(KzgPlayerStatus *kzgPlayerStatus, JavaCallHelper *helper) {
    this->helper = helper;
    this->kzgPlayerStatus = kzgPlayerStatus;
    this->queue = new SafeQueue(kzgPlayerStatus);
    this->tempqueue = new SafeQueue(kzgPlayerStatus);
    this->frameQueue = new AVFrameQueue(kzgPlayerStatus);
    pthread_mutex_init(&codecMutex,NULL);
    pthread_mutex_init(&showFrameMutex,NULL);
}

KzgVideo::~KzgVideo() {
    pthread_mutex_destroy(&codecMutex);
    pthread_mutex_destroy(&showFrameMutex);
}

struct timeval tv;
long startTime;

int SaveYuv(unsigned char *buf, int wrap, int xsize, int ysize, char *filename)
{
    FILE *f;
    int i;

    f = fopen(filename, "ab+");
    for (i = 0; i<ysize; i++)
    {
        fwrite(buf + i * wrap, 1, xsize, f);
    }
    fclose(f);
    return 1;
}



void *videoPlay(void *arg){
    KzgVideo *kzgVideo = static_cast<KzgVideo *>(arg);

    while (kzgVideo->kzgPlayerStatus != NULL && !kzgVideo->kzgPlayerStatus->exit){

        if (kzgVideo->frameQueue->getQueueSize() == 0){
            gettimeofday(&tv,NULL);
            startTime = tv.tv_sec*1000 + tv.tv_usec/1000;
        }

        /*if (kzgVideo->kzgPlayerStatus->seeking){
            av_usleep(1000 * 20);
            continue;
        }*/

        if (kzgVideo->kzgPlayerStatus->isPause){
            LOGE("77777777777");
            av_usleep(1000 * 100);
            continue;
        }


        if (kzgVideo->queue->getQueueSize() == 0){
            if (!kzgVideo->kzgPlayerStatus->loading){
                kzgVideo->kzgPlayerStatus->loading = true;
                kzgVideo->helper->onLoad(true,THREAD_CHILD);
            }
            LOGE("888888888");
            av_usleep(1000 * 20);
            continue;
        } else{
            if (kzgVideo->kzgPlayerStatus->loading){
                kzgVideo->kzgPlayerStatus->loading = false;
                kzgVideo->helper->onLoad(false,THREAD_CHILD);
            }
        }

        if (kzgVideo->kzgPlayerStatus->isFramePreview){
            int queueSize = 0;
            if (kzgVideo->codectype == CODEC_MEDIACODEC){
                queueSize = kzgVideo->helper->onCallJavaQueueSize(THREAD_CHILD);
            } else{
                queueSize = kzgVideo->frameQueue->getQueueSize();
            }
            if (queueSize >= 90){
                LOGE("99999999");
                av_usleep(1000*10);
                continue;
            }
        }

        if(kzgVideo->kzgPlayerStatus->isSeekPause && kzgVideo->kzgPlayerStatus->isBackSeekFramePreview){
            av_usleep(1000*1);
            LOGE("1010101010101010");
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        if (!kzgVideo->kzgPlayerStatus->isFramePreview && kzgVideo->frameQueue->getQueueSize() > 0){

        } else{
            if (kzgVideo->queue->getAvPacket(avPacket) != 0){
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                continue;
            }
        }


        if (kzgVideo->codectype == CODEC_MEDIACODEC){
            //硬解码

            //将解码前的avPacket发送给过滤器
            if(av_bsf_send_packet(kzgVideo->avbsfContext,avPacket) != 0){
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                continue;
            }
            while (av_bsf_receive_packet(kzgVideo->avbsfContext,avPacket) == 0){

                if (kzgVideo->kzgPlayerStatus->isFramePreview){
                    double pts = avPacket->pts;
                    kzgVideo->helper->onCallDecodeAVPacketByYUV(avPacket->size,avPacket->data,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,pts * av_q2d(kzgVideo->time_base));
                } else{
                    //享学课堂的同步算法
                    //av_usleep(kzgVideo->myGetDelayTime(avFrame) * AV_TIME_BASE);
                    //杨万里老师的同步算法
                    /*double diff = kzgVideo->getFrameDiffTime(NULL,avPacket);
                    double delayTime = kzgVideo->getDelayTime(diff);
                    av_usleep(delayTime * AV_TIME_BASE);*/

                    //给Java层MediaCodec解码
                    kzgVideo->helper->onCallDecodeAVPacket(avPacket->size,avPacket->data);
                }

                av_packet_free(&avPacket);
                av_free(avPacket);
            }

            avPacket = NULL;
        } else if(kzgVideo->codectype == CODEC_YUV){
            pthread_mutex_lock(&kzgVideo->codecMutex);
            if (!kzgVideo->kzgPlayerStatus->isFramePreview && kzgVideo->frameQueue->getQueueSize() > 0){

            } else{
                if (avcodec_send_packet(kzgVideo->avCodecContext,avPacket) != 0){
                    av_packet_free(&avPacket);
                    av_free(avPacket);
                    avPacket = NULL;
                    pthread_mutex_unlock(&kzgVideo->codecMutex);
                    continue;
                }
            }

            AVFrame *avFrame = av_frame_alloc();

            if (!kzgVideo->kzgPlayerStatus->isFramePreview && kzgVideo->frameQueue->getQueueSize() > 0){
                kzgVideo->frameQueue->getAvFrame(avFrame);
            } else{
                if(avcodec_receive_frame(kzgVideo->avCodecContext,avFrame) != 0){

                    av_frame_free(&avFrame);
                    av_free(avFrame);
                    avFrame = NULL;
                    av_packet_free(&avPacket);
                    av_free(avPacket);
                    avPacket = NULL;
                    pthread_mutex_unlock(&kzgVideo->codecMutex);
                    continue;
                }
            }


            if (avFrame->format == AV_PIX_FMT_YUV420P || avFrame->format == AV_PIX_FMT_YUVJ420P){
                //LOGE("子线程解码一个AVframe成功  timestamp:%lf,    seekTime:%lld",(avFrame->pts *av_q2d( kzgVideo->time_base) * AV_TIME_BASE),kzgVideo->seekTime);

                if (kzgVideo->kzgPlayerStatus->isFramePreview){

                    //逐帧预览
                    /*for (int i = 0; i <100 ; ++i) {
                        //查看YUV对齐
                        LOGE("**********linesize[0] : %d     ,linesize[1] : %d    ,linesize[2] : %d    ,width:%d    ,height:%d",avFrame->linesize[0],avFrame->linesize[1],avFrame->linesize[2],avFrame->width,avFrame->height);
                    }*/

                    if (kzgVideo->kzgPlayerStatus->isBackSeekFramePreview){
                        //后退专题的逐帧预览
                        if ((avFrame->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE) < kzgVideo->seekTime){
                            av_frame_free(&avFrame);
                            av_free(avFrame);
                            avFrame = NULL;
                            av_packet_free(&avPacket);
                            av_free(avPacket);
                            avPacket = NULL;
                            pthread_mutex_unlock(&kzgVideo->codecMutex);
                            continue;
                        }
                        kzgVideo->kzgPlayerStatus->isSeekPause = true;
                        kzgVideo->kzgPlayerStatus->isShowSeekFrame = true;

                        gettimeofday(&tv,NULL);
                        long endTime = tv.tv_sec*1000 + tv.tv_usec/1000;
                        LOGE("seek 一帧frame 耗时：%ld" ,(endTime-kzgVideo->startSeekTime));
                    } else{
                        //前进状态的逐帧预览
                        if (avFrame->linesize[0] > kzgVideo->avCodecContext->width && false){
                            //当Y的宽度大于视频实际的宽度，就进行裁剪到视频实际的宽度
                            kzgVideo->kzgPlayerStatus->isCrop = true;
                            AVFrame *cropAvframe = av_frame_alloc();
                            int size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,1);
                            //创建一个缓冲区
                            uint8_t *buffer = static_cast<uint8_t *>(av_malloc(size * sizeof(uint8_t)));
                            /****************我的处理对齐的方式******************/
//                            int nYUVBufsize = 0;
//                            for (int i=0; i < avFrame->height; i++){
//                                memcpy(buffer + nYUVBufsize , avFrame->data[0] + i * avFrame->linesize[0],
//                                       avFrame->width);
//                                nYUVBufsize += avFrame->width;
//                            }
//                            for (int i=0; i < avFrame->height/2; i++){
//                                memcpy(buffer + nYUVBufsize , avFrame->data[1] + i * avFrame->linesize[1],
//                                       avFrame->width/2);
//                                nYUVBufsize += avFrame->width/2;
//                            }
//                            for (int i=0; i < avFrame->height/2; i++){
//                                memcpy(buffer + nYUVBufsize , avFrame->data[2] + i * avFrame->linesize[2],
//                                       avFrame->width/2);
//                                nYUVBufsize += avFrame->width/2;
//                            }
                            /****************我的处理对齐的方式******************/

                            /****************ffmpeg处理对齐的方式******************/
                            int ret = av_image_copy_to_buffer(buffer,size,avFrame->data,avFrame->linesize,AV_PIX_FMT_YUV420P
                                    ,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,1);
                            /****************ffmpeg处理对齐的方式******************/
                            //保存为YUV文件
                            //SaveYuv(avFrame->data[0], avFrame->linesize[0], kzgVideo->avCodecContext->width, kzgVideo->avCodecContext->height, outputfilename);
                            //SaveYuv(avFrame->data[1], avFrame->linesize[1], kzgVideo->avCodecContext->width / 2, kzgVideo->avCodecContext->height / 2, outputfilename);
                            //SaveYuv(avFrame->data[2], avFrame->linesize[2], kzgVideo->avCodecContext->width / 2, kzgVideo->avCodecContext->height / 2, outputfilename);


                            cropAvframe->data[0] = static_cast<uint8_t *>(malloc(avFrame->width * avFrame->height));
                            cropAvframe->data[1] = static_cast<uint8_t *>(malloc(avFrame->width/2 * avFrame->height/2));
                            cropAvframe->data[2] = static_cast<uint8_t *>(malloc(avFrame->width/2 * avFrame->height/2));
                            memcpy( cropAvframe->data[0],buffer,avFrame->width * avFrame->height);
                            memcpy(cropAvframe->data[1],buffer + avFrame->width * avFrame->height,avFrame->width/2 * avFrame->height/2);
                            memcpy(cropAvframe->data[2],buffer + (avFrame->width * avFrame->height + avFrame->width/2 * avFrame->height/2),avFrame->width/2 * avFrame->height/2);
                            cropAvframe->pts = av_frame_get_best_effort_timestamp(avFrame);

                            if ((cropAvframe->pts *av_q2d( kzgVideo->time_base) * AV_TIME_BASE)==0 && kzgVideo->seekTime == 0){
                                //显示首帧
                                int width = cropAvframe->linesize[0] > kzgVideo->avCodecContext->width? cropAvframe->linesize[0]:kzgVideo->avCodecContext->width;
                                kzgVideo->helper->onCallRenderYUV(
                                        width,
                                        kzgVideo->avCodecContext->height,
                                        cropAvframe->data[0],
                                        cropAvframe->data[1],
                                        cropAvframe->data[2],
                                        THREAD_CHILD);

                                av_frame_free(&cropAvframe);
                                av_free(cropAvframe);
                                cropAvframe = NULL;
                            } else{
                                kzgVideo->frameQueue->putAvFrame(cropAvframe);
                            }

                            av_free(buffer);
                            av_frame_free(&avFrame);
                            av_free(avFrame);
                            avFrame = NULL;
                        } else{
                            kzgVideo->kzgPlayerStatus->isCrop = false;
                            avFrame->pts = av_frame_get_best_effort_timestamp(avFrame);
                            if ((avFrame->pts *av_q2d( kzgVideo->time_base) * AV_TIME_BASE)==0 && kzgVideo->seekTime == 0){
                                //显示首帧
                                int width = avFrame->linesize[0] > kzgVideo->avCodecContext->width? avFrame->linesize[0]:kzgVideo->avCodecContext->width;
                                kzgVideo->helper->onCallRenderYUV(
                                        width,
                                        kzgVideo->avCodecContext->height,
                                        avFrame->data[0],
                                        avFrame->data[1],
                                        avFrame->data[2],
                                        THREAD_CHILD);

                                av_frame_free(&avFrame);
                                av_free(avFrame);
                                avFrame = NULL;
                            } else{
                                kzgVideo->frameQueue->putAvFrame(avFrame);
                            }
                        }
                        kzgVideo->kzgPlayerStatus->isShowSeekFrame = true;

                        if (kzgVideo->frameQueue->getQueueSize() == 90){
                            gettimeofday(&tv,NULL);
                            long endTime = tv.tv_sec*1000 + tv.tv_usec/1000;
                            kzgVideo->helper->onEnablePlay(true,THREAD_CHILD);
                            LOGE("软解码90帧耗时：%ld" ,(endTime-startTime));
                        }


                        av_packet_free(&avPacket);
                        av_free(avPacket);
                        avPacket = NULL;
                        pthread_mutex_unlock(&kzgVideo->codecMutex);
                        continue;
                    }
                }

                if (!kzgVideo->kzgPlayerStatus->isFramePreview){
                    //享学课堂的同步算法
                    //av_usleep(kzgVideo->myGetDelayTime(avFrame) * AV_TIME_BASE);
                    //杨万里老师的同步算法
                    double diff = kzgVideo->getFrameDiffTime(avFrame,NULL);
                    double delayTime = kzgVideo->getDelayTime(diff);
                    av_usleep(delayTime * AV_TIME_BASE);
                }

                int width = avFrame->linesize[0] > kzgVideo->avCodecContext->width? avFrame->linesize[0]:kzgVideo->avCodecContext->width;
                //传回Java进行渲染
                if (avFrame->linesize[0] > kzgVideo->avCodecContext->width){
                    /*int size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,1);
                    //创建一个缓冲区
                    uint8_t *buffer = static_cast<uint8_t *>(av_malloc(size * sizeof(uint8_t)));
                    av_image_copy_to_buffer(buffer,size,avFrame->data,avFrame->linesize,AV_PIX_FMT_YUV420P
                            ,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,1);
                    uint8_t *dataY = static_cast<uint8_t *>(malloc(avFrame->width * avFrame->height));
                    uint8_t *dataU = static_cast<uint8_t *>(malloc(avFrame->width/2 * avFrame->height/2));
                    uint8_t *dataV = static_cast<uint8_t *>(malloc(avFrame->width/2 * avFrame->height/2));
                    memcpy( dataY,buffer,avFrame->width * avFrame->height);
                    memcpy(dataU,buffer + avFrame->width * avFrame->height,avFrame->width/2 * avFrame->height/2);
                    memcpy(dataV,buffer + (avFrame->width * avFrame->height + avFrame->width/2 * avFrame->height/2),avFrame->width/2 * avFrame->height/2);
                    kzgVideo->helper->onCallRenderYUV(
                            kzgVideo->avCodecContext->width,
                            kzgVideo->avCodecContext->height,
                            dataY,
                            dataU,
                            dataV,
                            THREAD_CHILD);

                    av_free(buffer);
                    av_free(dataY);
                    av_free(dataU);
                    av_free(dataV);*/


                    kzgVideo->helper->onCallRenderYUV(
                            width,
                            kzgVideo->avCodecContext->height,
                            avFrame->data[0],
                            avFrame->data[1],
                            avFrame->data[2],
                            THREAD_CHILD);
                } else{
                    kzgVideo->helper->onCallRenderYUV(
                            width,
                            kzgVideo->avCodecContext->height,
                            avFrame->data[0],
                            avFrame->data[1],
                            avFrame->data[2],
                            THREAD_CHILD);
                }


                //发送进度信息给Java
                int64_t  currentTime = (avFrame->pts *av_q2d( kzgVideo->time_base) * AV_TIME_BASE);
                //LOGE("视频帧时间：%lld    总时间：%lld",currentTime,kzgVideo->duration);
                if( !kzgVideo->kzgPlayerStatus->isFramePreview){
                    kzgVideo->helper->onProgress(currentTime,kzgVideo->duration,THREAD_CHILD);
                }

            } else{
                LOGE("codec  not  AV_PIX_FMT_YUV420P");
                //如果视频不是YUV420P的就将其转换成YUV420P
                AVFrame *avFrameYUV420P = av_frame_alloc();
                //获取需要的缓冲区大小
                int size = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height,1);

                //创建一个缓冲区
                uint8_t *buffer = static_cast<uint8_t *>(av_malloc(size * sizeof(uint8_t)));
                //
                av_image_fill_arrays(
                        avFrameYUV420P->data,
                        avFrameYUV420P->linesize,
                        buffer,
                        AV_PIX_FMT_YUV420P,
                        kzgVideo->avCodecContext->width,
                        kzgVideo->avCodecContext->height,
                        1);

                //
                SwsContext *swsContext = sws_getContext(
                        kzgVideo->avCodecContext->width,
                        kzgVideo->avCodecContext->height,
                        kzgVideo->avCodecContext->pix_fmt,
                        kzgVideo->avCodecContext->width,
                        kzgVideo->avCodecContext->height,
                        AV_PIX_FMT_YUV420P,
                        SWS_FAST_BILINEAR,NULL,NULL,NULL);

                if (!swsContext){
                    av_frame_free(&avFrameYUV420P);
                    av_free(avFrameYUV420P);
                    av_free(buffer);
                    pthread_mutex_unlock(&kzgVideo->codecMutex);
                    continue;
                }

                //格式转换
                sws_scale(
                        swsContext,
                        reinterpret_cast<const uint8_t *const *>(avFrame->data),
                        avFrame->linesize,
                        0,
                        avFrame->height,
                        avFrameYUV420P->data,
                        avFrameYUV420P->linesize);


                if (kzgVideo->kzgPlayerStatus->isFramePreview){
                    //逐帧预览
                    /*AVFrame *copyFrame = av_frame_alloc();
                    av_frame_get_buffer(copyFrame, 0);
                    memcpy(copyFrame->data, avFrameYUV420P->data, sizeof(avFrameYUV420P->data));
                    copyFrame->pts = av_frame_get_best_effort_timestamp(avFrame);*/
                    avFrameYUV420P->pts = av_frame_get_best_effort_timestamp(avFrame);
                    kzgVideo->frameQueue->putAvFrame(avFrameYUV420P);
                    //LOGE("put frameQueue pts:%lf   queueSize:%d ",(pts * av_q2d(kzgVideo->time_base)),kzgVideo->frameQueue->getQueueSize());
                    if (kzgVideo->frameQueue->getQueueSize() == 90){
                        gettimeofday(&tv,NULL);
                        long endTime = tv.tv_sec*1000 + tv.tv_usec/1000;
                        LOGE("软解码90帧耗时：%ld" ,(endTime-startTime));
                    }
                    /*av_free(buffer);
                    sws_freeContext(swsContext);

                    av_packet_free(&avPacket);
                    av_free(avPacket);
                    avPacket = NULL;*/
                    pthread_mutex_unlock(&kzgVideo->codecMutex);
                    continue;
                }

                //享学课堂的同步算法
                //av_usleep(kzgVideo->myGetDelayTime(avFrame) * AV_TIME_BASE);
                //杨万里老师的同步算法
                double diff = kzgVideo->getFrameDiffTime(avFrame,NULL);
                double delayTime = kzgVideo->getDelayTime(diff);
                av_usleep(delayTime * AV_TIME_BASE);

                //传回Java进行渲染
                kzgVideo->helper->onCallRenderYUV(
                        kzgVideo->avCodecContext->width,
                        kzgVideo->avCodecContext->height,
                        avFrameYUV420P->data[0],
                        avFrameYUV420P->data[1],
                        avFrameYUV420P->data[2],
                        THREAD_CHILD);

                av_frame_free(&avFrameYUV420P);
                av_free(avFrameYUV420P);
                av_free(buffer);
                sws_freeContext(swsContext);

            }

            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&kzgVideo->codecMutex);
        }
    }

    LOGE("KzgVideo 播放退出啦！");
    return 0;
}


void KzgVideo::play() {
    if (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
        pthread_create(&play_thread,NULL,videoPlay,this);
    }
}

void KzgVideo::release() {
    //fclose(yuvFile);
    if (frameQueue != NULL){
        frameQueue->noticeQueue();
    }

    if (queue != NULL){
        queue->noticeQueue();
    }
    pthread_join(play_thread,NULL);

    if (frameQueue != NULL){
        delete(frameQueue);
        frameQueue = NULL;
    }

    if (queue != NULL){
        delete(queue);
        queue = NULL;
    }

    if (avCodecContext != NULL){
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }

    if (kzgPlayerStatus != NULL){
        kzgPlayerStatus = NULL;
    }

    if(helper != NULL){
        helper = NULL;
    }
}

double KzgVideo::getFrameDiffTime(AVFrame *avFrame,AVPacket *avPacket) {
    if (kzgAudio == NULL || this->kzgPlayerStatus->isFramePreview){
        return 0;
    }
    double pts ;
    if (avFrame != NULL){
        pts = av_frame_get_best_effort_timestamp(avFrame);
    }

    if (avPacket != NULL){
        pts = avPacket->pts;
    }
    if (pts == AV_NOPTS_VALUE){
        pts = 0;
    }
    pts *= av_q2d(time_base);
    if (pts > 0){
        lock  = pts;
    }

    double diff = kzgAudio->clock - lock;
    return diff;
}

double KzgVideo::getDelayTime(double diff) {

    //LOGE("delay Time : %f",diff);
    if(diff > 0.003)
    {
        //如果视频比音频慢，就减少播放视频帧的间隔时间
        delayTime = delayTime * 2 / 3;
        //防止间隔时间过小或过大，造成视频忽快忽慢太明显
        if(delayTime < defaultDelayTime / 2)
        {
            delayTime = defaultDelayTime * 2 / 3;
        }
        else if(delayTime > defaultDelayTime * 2)
        {
            delayTime = defaultDelayTime * 2;
        }
    }
    else if(diff < - 0.003)
    {
        //如果视频比音频快，就增加视频帧的间隔时间
        delayTime = delayTime * 3 / 2;
        //防止间隔时间过小或过大，造成视频忽快忽慢太明显
        if(delayTime < defaultDelayTime / 2)
        {
            delayTime = defaultDelayTime * 2 / 3;
        }
        else if(delayTime > defaultDelayTime * 2)
        {
            delayTime = defaultDelayTime * 2;
        }
    }
    else if(diff == 0.003)
    {

    }
    if(diff >= 0.5)
    {
        //视频太慢的就将视频帧的间隔时间设置为0
        delayTime = 0;
    }
    else if(diff <= -0.5)
    {
        //视频太快了，就将视频帧的间隔时间设置为原来帧的间隔时间的2倍
        delayTime = defaultDelayTime * 2;
    }

    if(fabs(diff) >= 10)
    {
        delayTime = defaultDelayTime;
    }

    return delayTime;
}

double KzgVideo::myGetDelayTime(AVFrame *avFrame) {
    /**
        * 让视频同步音频
        */
    //获取每一帧需要的时长
    double frame_delay = 1.0 / fps;
    //获取ffmpeg 需要的一个额外时长
    double extra_delay = avFrame->repeat_pict /(2*fps);
    double delay = frame_delay + extra_delay;

    if (kzgAudio){
        //获取视频这一帧播放的时刻 也就是pts
        lock = avFrame->best_effort_timestamp * av_q2d(time_base);
        //计算音视频的时间差
        double diff = lock - kzgAudio->clock;

        /**
         * 计算一个合适的阈值
         * 1.如果delay < 0.04 那么阈值就是0.04
         * 2.如果delay > 0.1 那么阈值就是0.1
         * 3.如果0.04 < delay < 0.1  那么阈值就是delay
         */
        double sync = FFMAX(AV_SYNC_THRESHOLD_MIN, FFMIN(AV_SYNC_THRESHOLD_MAX, delay));

        if (diff < -sync){
            //视频落后太多，那么就要让delay的时间减小，但是又不能小于0
            delay = FFMAX(0,delay + diff);
        } else if (diff > sync){
            //如果视频提前了，就要增加休眠时间
            delay = delay + diff;
        }

    }
    return delay;
}

void KzgVideo::setIsFramePreview(bool isFramePreview) {
    if (kzgPlayerStatus != NULL){
        kzgPlayerStatus->isFramePreview = isFramePreview;
    }
}

void KzgVideo::showFrame(double timestamp) {

    //LOGE("kzgVideo get showFrame ,   timestamp:%lf",timestamp);
    if (frameQueue != NULL && frameQueue->getQueueSize() > 0){

        AVFrame *avFrame = av_frame_alloc();
        if(frameQueue->getAvFrameByTime(avFrame,timestamp,time_base) == 0){
            if (avFrame->data[0] == NULL){
                return;
            }
            double pts = avFrame->pts;
            pts *= av_q2d(time_base);
            //LOGE("kzgVideo get frameQueue pts:%lf ,   timestamp:%lf",pts,timestamp);
            if (timestamp >= (pts - 0.03) && timestamp <= (pts + 0.03)){
                int width = avFrame->linesize[0] > avCodecContext->width? avFrame->linesize[0]:avCodecContext->width;
                this->showFrameTimestamp = timestamp;
                //传回Java进行渲染
                helper->onCallRenderYUV(
                        width,
                        avCodecContext->height,
                        avFrame->data[0],
                        avFrame->data[1],
                        avFrame->data[2]);

            } else{
                if (kzgPlayerStatus->isCrop){
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
                if (timestamp >(pts + 0.03)){
                    showFrame(timestamp);
                }
                return;
            }

        }
        if (kzgPlayerStatus->isCrop){
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
        return;

    } else{
        LOGE("kzgVideo frameQueue is empty");
    }




}
