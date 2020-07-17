//
// Created by Administrator on 2020/5/9.
//

#include "KzgFFmpeg.h"
#include "include/libavutil/hwcontext.h"


KzgFFmpeg::KzgFFmpeg(JavaCallHelper *_helper, const char *_url,KzgPlayerStatus *kzgPlayerStatus1) {
    helper = _helper;
    url = _url;
    this->kzgPlayerStatus = kzgPlayerStatus1;
    pthread_mutex_init(&init_mutex,NULL);
    pthread_mutex_init(&seek_mutex,NULL);
    exit = false;
}

KzgFFmpeg::~KzgFFmpeg() {
    pthread_mutex_destroy(&init_mutex);
    pthread_mutex_destroy(&seek_mutex);
}

void *t_decode(void * arg){
    KzgFFmpeg *kzgFFmpeg = static_cast<KzgFFmpeg *>(arg);
    kzgFFmpeg->decodeFFmpegThread();
    return 0;
}


void KzgFFmpeg::parpared() {
    if (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
        pthread_create(&decodeThread,NULL,t_decode,this);
    }
}

int avformat_callback(void *ctx){
    KzgFFmpeg *fFmpeg = (KzgFFmpeg *) ctx;
    if(fFmpeg->kzgPlayerStatus->exit)
    {
        return AVERROR_EOF;
    }
    return 0;
}

void KzgFFmpeg::decodeFFmpegThread() {
    pthread_mutex_lock(&init_mutex);
    av_register_all();
    avformat_network_init();

    avFormatContext = avformat_alloc_context();
    int ret;

    avFormatContext->interrupt_callback.callback = avformat_callback;
    avFormatContext->interrupt_callback.opaque = this;
    ret = avformat_open_input(&avFormatContext,url,0,0);
    if (ret != 0){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("open url fail %s:" ,url);
        helper->onError(1001,"open url fail",THREAD_CHILD);
        return;
    }
    ret = avformat_find_stream_info(avFormatContext,0);
    if (ret < 0){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("find audio stream fail %s:", url);
        helper->onError(1002,"find audio stream fail",THREAD_CHILD);
        return;
    }
    for(int i=0;i<avFormatContext->nb_streams;i++){
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO){
            if (kzgAudio == NULL){
                //先不处理音频
                /*kzgAudio = new KzgAudio(kzgPlayerStatus,avFormatContext->streams[i]->codecpar->sample_rate,helper);
                kzgAudio->streamIndex = i;
                kzgAudio->avCodecParameters = avFormatContext->streams[i]->codecpar;
                kzgAudio->duration = avFormatContext->duration / AV_TIME_BASE;
                kzgAudio->time_base = avFormatContext->streams[i]->time_base;
                duration = kzgAudio->duration;*/
            }
        } else if(avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO){
            if (kzgVideo == NULL){
                kzgVideo = new KzgVideo(kzgPlayerStatus,helper);
                kzgVideo->streamIndex = i;
                kzgVideo->avCodecParameters = avFormatContext->streams[i]->codecpar;
                kzgVideo->time_base = avFormatContext->streams[i]->time_base;
                kzgVideo->duration = avFormatContext->duration;
                duration = avFormatContext->duration;
                int num = avFormatContext->streams[i]->avg_frame_rate.num;
                int den = avFormatContext->streams[i]->avg_frame_rate.den;
                if (num > 0 && den > 0){
                    int fps = num / den;//比如 25 / 1  也就是每秒25帧
                    kzgVideo->defaultDelayTime = 1.0 / fps;
                    kzgVideo->fps = fps;
                }
            }


        }
    }


    if (kzgAudio != NULL){
        //getAVCodecContext(kzgAudio->avCodecParameters,&kzgAudio->avCodecContext);
    }
    if (kzgVideo !=NULL){
        getAVCodecContext(kzgVideo->avCodecParameters,&kzgVideo->avCodecContext);
        //LOGE("视频的帧率:%d,   视频的时长%d  视频宽%d,  视频高%d",kzgVideo->fps,kzgVideo->duration,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height);
        helper->onCallVideoInfo(THREAD_CHILD,kzgVideo->fps,kzgVideo->duration,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height);
    }

    if(kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
        helper->onPrepare(THREAD_CHILD);
    } else{
        exit = true;
    }

    pthread_mutex_unlock(&init_mutex);
}

void KzgFFmpeg::start() {
    if (kzgAudio == NULL){
        LOGE("audio is NULL ");
        //return;
    }

    if (kzgVideo == NULL){
        return;
    }

    kzgVideo->kzgAudio = kzgAudio;
    //判断是否支持硬解码
    const char *codecName =  kzgVideo->avCodecContext->codec->name;
    if(helper->onCallIsSupperMediaCodec(codecName,THREAD_CHILD)){
        //支持硬解码
        LOGE("当前设备支持硬解码当前视频");
        supportMediacodec = true;
        if (strcasecmp(codecName,"h264") == 0){
            //如果编码器的名字为h264
            bsFilter = av_bsf_get_by_name("h264_mp4toannexb");
        } else if (strcasecmp(codecName,"h265") == 0){
            //如果编码器的名字为h264
            bsFilter = av_bsf_get_by_name("hevc_mp4toannexb");
        }
        if (bsFilter == NULL){
            supportMediacodec = false;
            goto end;
        }

        //创建解码器过滤器上下文
        if(av_bsf_alloc(bsFilter,&kzgVideo->avbsfContext) != 0){
            supportMediacodec = false;
            goto end;
        }

        if (avcodec_parameters_copy(kzgVideo->avbsfContext->par_in,kzgVideo->avCodecParameters) < 0){
            supportMediacodec = false;
            av_bsf_free(&kzgVideo->avbsfContext);
            kzgVideo->avbsfContext = NULL;
            goto end;
        }

        //初始化解码器过滤器上下文
        if (av_bsf_init(kzgVideo->avbsfContext) != 0){
            supportMediacodec = false;
            av_bsf_free(&kzgVideo->avbsfContext);
            kzgVideo->avbsfContext = NULL;
            goto end;
        }
        //赋值时间基
        kzgVideo->avbsfContext->time_base_in = kzgVideo->time_base;
    } else{
        supportMediacodec = false;
    }

    supportMediacodec = false;
    end:
    if (supportMediacodec){
        LOGE("支持硬解码");
        kzgVideo->codectype = CODEC_MEDIACODEC;
        if (kzgVideo->kzgPlayerStatus->isFramePreview){
            kzgVideo->helper->onCallInitMediaCodecByYUV(
                    codecName,
                    kzgVideo->avCodecContext->width,
                    kzgVideo->avCodecContext->height,
                    kzgVideo->avCodecContext->extradata_size,
                    kzgVideo->avCodecContext->extradata_size,
                    kzgVideo->avCodecContext->extradata,
                    kzgVideo->avCodecContext->extradata);
        } else{
            kzgVideo->helper->onCallInitMediaCodec(
                    codecName,
                    kzgVideo->avCodecContext->width,
                    kzgVideo->avCodecContext->height,
                    kzgVideo->avCodecContext->extradata_size,
                    kzgVideo->avCodecContext->extradata_size,
                    kzgVideo->avCodecContext->extradata,
                    kzgVideo->avCodecContext->extradata);
        }
    } else {
        LOGE("不支持硬解码");
        kzgVideo->codectype = CODEC_YUV;
    }

    int count = 0;
    int ret;

    //kzgAudio->play();
    kzgVideo->play();
    while (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){

        /*if (kzgPlayerStatus->seeking){
            av_usleep(1000*20);
            continue;
        }*/


        /*if (kzgAudio->queue->getQueueSize() > 40){
            av_usleep(1000*100);
            continue;
        }*/

        if (kzgVideo->queue->getQueueSize() > 40){
            av_usleep(1000*20);
            continue;
        }

        if (kzgVideo->kzgPlayerStatus->isFramePreview){
            if (kzgVideo->kzgPlayerStatus->isBackSeekFramePreview && kzgVideo->queue->getQueueSize() > 5 && kzgVideo->kzgPlayerStatus->isSeekPause){
                av_usleep(1000*1);
                continue;
            } else{
                int queueSize = 0;
                if (supportMediacodec){
                    queueSize = kzgVideo->helper->onCallJavaQueueSize(THREAD_CHILD);
                } else{
                    queueSize = kzgVideo->frameQueue->getQueueSize();
                }
                if (queueSize >= 90){
                    av_usleep(1000*10);
                    continue;
                }
            }

        }


        AVPacket *avPacket = av_packet_alloc();
        ret = av_read_frame(avFormatContext,avPacket);
        if (ret == 0){
            if (kzgAudio != NULL && avPacket->stream_index == kzgAudio->streamIndex){
                //开始解码音频
                LOGE("开始解码音频第 %d 帧",count);
                kzgAudio->queue->putAvPacket(avPacket);

            }else if (avPacket->stream_index == kzgVideo->streamIndex){
                //开始解码视频
                count ++;
                /*if (avPacket->flags & AV_PKT_FLAG_KEY){
                    //KEY FRAME
                    LOGE("开始解码 I帧 %lf      pts:%lld    ,dts:%lld     flags:%d,",(avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE),avPacket->pts ,avPacket->dts,avPacket->flags);
                    kzgVideo->queue->putAvPacket(avPacket);
                    lastIsBFrame = false;
                    kzgVideo->tempqueue->clearAvPacket();
                } else {
                    if (avPacket->pts < kzgVideo->lastPFramePTS && (avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE) < (kzgVideo->seekTime) *//*- 5000000*//*){
                        //B FRAME
                        if(!lastIsBFrame){
                            //lastBFramePacket = av_packet_clone(avPacket);
                            kzgVideo->tempqueue->clearAvPacket();
                            LOGE("开始解码 第一个B帧 %lf      pts:%lld    ,dts:%lld     flags:%d,",(avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE),avPacket->pts ,avPacket->dts,avPacket->flags);
                            kzgVideo->tempqueue->putAvPacket(avPacket);
                        } else{
                            if (kzgVideo->tempqueue->getQueueSize() > 0){
                                AVPacket *avPacket22 = av_packet_alloc();
                                kzgVideo->tempqueue->getAvPacket(avPacket22);
                                LOGE("第一个B帧 pts:%lld",avPacket22->pts);
                                if(avPacket->pts < avPacket22->pts){
                                    kzgVideo->queue->putAvPacket(avPacket22);
                                }
                            }
                            LOGE("丢弃B帧 视频第 %lf 帧      pts:%lld    ,dts:%lld     flags:%d,",(avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE),avPacket->pts ,avPacket->dts,avPacket->flags);
                            av_packet_free(&avPacket);
                            av_free(avPacket);

                        }

                        lastIsBFrame = true;
                    } else{
                        LOGE("开始解码 P帧 %lf      pts:%lld    ,dts:%lld     flags:%d,",(avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE),avPacket->pts ,avPacket->dts,avPacket->flags);
                        kzgVideo->lastPFramePTS = avPacket->pts;
                        kzgVideo->queue->putAvPacket(avPacket);
                        lastIsBFrame = false;
                    }

                }*/
                kzgVideo->queue->putAvPacket(avPacket);
            }else{
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
            tempIndex ++;
        } else{
            LOGE("read frame fail ");
            av_packet_free(&avPacket);
            av_free(avPacket);
            while (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
                if (kzgVideo->queue->getQueueSize() > 0 || kzgVideo->frameQueue->getQueueSize() > 0 || kzgVideo->helper->onCallJavaQueueSize(THREAD_CHILD) > 0){
                    av_usleep(1000*20);
                    continue;
                } else{
                    if (!kzgPlayerStatus->seeking){
                        //下面事播放完成退出，这里暂时先不用退出
                        //av_usleep(1000*20);
                        /*if (!kzgVideo->kzgPlayerStatus->isFramePreview){
                            kzgPlayerStatus->exit = true;
                        }*/

                    }
                    break;
                }
            }
            continue;
            //break;
        }
    }

    if (helper != NULL){
        helper->onComplete(THREAD_CHILD);
    }
    exit = true;
    LOGE("解码完成");
}

void KzgFFmpeg::pause() {
    if (kzgAudio != NULL){
        kzgAudio->pause();
    }
    kzgPlayerStatus->isPause = true;
}

void KzgFFmpeg::resume() {
    if (kzgAudio != NULL){
        kzgAudio->resume();
    }
    kzgPlayerStatus->isPause = false;
}

void KzgFFmpeg::stop() {
    LOGE("开始释放Ffmpe");

    LOGE("开始释放Ffmpe2");
    kzgPlayerStatus->exit = true;
    pthread_join(decodeThread,NULL);

    pthread_mutex_lock(&init_mutex);
    int sleep_count = 0;
    while (!exit){
        if (sleep_count > 1000){
            exit = true;
        }
        LOGE("wait ffmpeg  exit %d", sleep_count);
        sleep_count ++;
        av_usleep(1000*10);//10毫秒
    }

    if(kzgAudio != NULL){
        LOGE("释放kzgAudio");
        kzgAudio->release();
        delete kzgAudio;
        kzgAudio = NULL;
    }

    if (kzgVideo != NULL){
        LOGE("释放kzgVideo");
        kzgVideo->release();
        delete kzgVideo;
        kzgVideo = NULL;
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

    if (kzgPlayerStatus != NULL){
        LOGE("释放kzgPlayerStatus");
        kzgPlayerStatus = NULL;
    }
    LOGE("释放FFmpeg 完成");
    pthread_mutex_unlock(&init_mutex);

}


//
void KzgFFmpeg::seek(int64_t sec) {

    if(duration <= 0){
        return;
    }

    if (sec > 0 && sec <= duration){
        pthread_mutex_lock(&seek_mutex);
        kzgPlayerStatus->seeking = true;
        int64_t res = sec/1000.0 * AV_TIME_BASE;
        if (kzgVideo != NULL){
            kzgVideo->seekTime = res;
            if (!kzgVideo->kzgPlayerStatus->isShowSeekFrame){
                pthread_mutex_unlock(&seek_mutex);
                return;
            }
        }

        kzgVideo->kzgPlayerStatus->isShowSeekFrame = false;
        avformat_seek_file(avFormatContext,-1,INT64_MIN,res,INT64_MAX,0);
        if (kzgAudio != NULL){

            kzgAudio->queue->clearAvPacket();
            kzgAudio->clock = 0;
            kzgAudio->last_time = 0;
            pthread_mutex_lock(&kzgAudio->codecMute);
            avcodec_flush_buffers(kzgAudio->avCodecContext);
            pthread_mutex_unlock(&kzgAudio->codecMute);
        }

        if (kzgVideo != NULL){
            if (kzgVideo->kzgPlayerStatus->isBackSeekFramePreview){
                kzgVideo->frameQueue->clearAvPacket();
            }
            kzgVideo->queue->clearAvPacket();
            kzgVideo->lock = 0;
            LOGE("seeking %lld,   kzgVideo->seekTime: %lld",sec,kzgVideo->seekTime);
            pthread_mutex_lock(&kzgVideo->codecMutex);
            avcodec_flush_buffers(kzgVideo->avCodecContext);
            pthread_mutex_unlock(&kzgVideo->codecMutex);

            struct timeval tv;
            gettimeofday(&tv,NULL);
            kzgVideo->startSeekTime = tv.tv_sec*1000 + tv.tv_usec/1000;

        }
        kzgVideo->lastPFramePTS = 0;
        lastIsBFrame = false;
        kzgVideo->kzgPlayerStatus->isSeekPause = false;
        pthread_mutex_unlock(&seek_mutex);
        kzgPlayerStatus->seeking = false;
    }
}

int KzgFFmpeg::getDuration() {
    return duration;
}

void KzgFFmpeg::setVolume(int volume) {
    if (kzgAudio != NULL){
        kzgAudio->setVolume(volume);
    }
}

void KzgFFmpeg::setPitch(float pi) {
    if (kzgAudio != NULL){
        kzgAudio->setPitch(pi);
    }
}

void KzgFFmpeg::setSpeed(float sp) {
    if (kzgAudio != NULL){
        kzgAudio->setSpeed(sp);
    }
}

void KzgFFmpeg::setAmplifyVol(float vol) {
    if (kzgAudio != NULL){
        kzgAudio->setAmplifyVol(vol);
    }
}

int KzgFFmpeg::getSampleRate() {
    if (kzgAudio != NULL){
        return kzgAudio->avCodecContext->sample_rate;
    }
    return 0;
}

int KzgFFmpeg::getAVCodecContext(AVCodecParameters *avCodecParameters,
                                 AVCodecContext **avCodecContext) {

    int ret;
    AVCodec * avCodec = avcodec_find_decoder(avCodecParameters->codec_id);
    //AVCodec * avCodec = avcodec_find_decoder_by_name("h264_mediacodec");
    if (!avCodec){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("find decoder fail %s:", url);
        helper->onError(1003,"find decoder fail",THREAD_CHILD);
        return -1;
    }


    *avCodecContext = avcodec_alloc_context3(avCodec);
    if (!*avCodecContext){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("alloc codec context fail %s:", url);
        helper->onError(1004,"alloc codec context fail",THREAD_CHILD);
        return -1;
    }


    ret = avcodec_parameters_to_context(*avCodecContext,avCodecParameters);
    if (ret < 0){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("avcodec_parameters_from_context fail %s:", url);
        helper->onError(1005,"avcodec_parameters_from_context fail",THREAD_CHILD);
        return -1;
    }
    (*avCodecContext)->thread_type = FF_THREAD_FRAME;
    (*avCodecContext)->thread_count = 8;

    ret = avcodec_open2(*avCodecContext,avCodec,0);
    if (ret != 0){
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        LOGE("open audio codec fail %s:", url);
        helper->onError(1006,"open audio codec fail",THREAD_CHILD);
        return -1;
    }
    return 0;
}

void KzgFFmpeg::setIsFramePreview(bool isFramePreview) {
    if (kzgVideo != NULL){
        kzgVideo->setIsFramePreview(isFramePreview);
    }
}

void KzgFFmpeg::showFrame(double timestamp) {
    if (kzgVideo != NULL){
        kzgVideo->showFrame(timestamp);
    }
}

void KzgFFmpeg::showFrameFromSeek(double timestamp) {
    if (kzgVideo != NULL){
        //kzgVideo->kzgPlayerStatus->frameSeeking = true;
    }
}

void KzgFFmpeg::setSeekType(int type) {
    if (kzgVideo != NULL){
        if (type == 0){
            //回退
            kzgVideo->kzgPlayerStatus->isBackSeekFramePreview = true;
        } else{
            //前进
            kzgVideo->kzgPlayerStatus->isBackSeekFramePreview = false;
        }

    }
}

