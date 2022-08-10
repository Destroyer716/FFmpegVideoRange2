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







static int h264_extradata_to_annexb(const unsigned char *pCodecExtraData, const int codecExtraDataSize,
                                    AVPacket *pOutExtradata, int padding)
{
    const unsigned char *pExtraData = NULL; /* 前四个字节没用 */
    int len = 0;
    int spsUnitNum, ppsUnitNum;
    int unitSize, totolSize = 0;
    unsigned char startCode[] = {0, 0, 0, 1};
    unsigned char *pOut = NULL;
    int err;

    pExtraData = pCodecExtraData+4;
    len = (*pExtraData++ & 0x3) + 1;

    /* 获取SPS */
    spsUnitNum = (*pExtraData++ & 0x1f); /* SPS数量 */
    while(spsUnitNum--)
    {
        unitSize = (pExtraData[0]<<8 | pExtraData[1]); /* 两个字节表示这个unit的长度 */
        pExtraData += 2;
        totolSize += unitSize + sizeof(startCode);
        printf("unitSize:%d\n", unitSize);

        if(totolSize > INT_MAX - padding)
        {
            av_log(NULL, AV_LOG_ERROR,
                   "Too big extradata size, corrupted stream or invalid MP4/AVCC bitstream\n");
            av_free(pOut);
            return AVERROR(EINVAL);
        }

        if(pExtraData + unitSize > pCodecExtraData + codecExtraDataSize)
        {
            av_log(NULL, AV_LOG_ERROR, "Packet header is not contained in global extradata, "
                                       "corrupted stream or invalid MP4/AVCC bitstream\n");
            av_free(pOut);
            return AVERROR(EINVAL);
        }

        if((err = av_reallocp(&pOut, totolSize + padding)) < 0)
            return err;


        memcpy(pOut+totolSize-unitSize-sizeof(startCode), startCode, sizeof(startCode));
        memcpy(pOut+totolSize-unitSize, pExtraData, unitSize);

        pExtraData += unitSize;
    }

    /* 获取PPS */
    ppsUnitNum = (*pExtraData++ & 0x1f); /* PPS数量 */
    while(ppsUnitNum--)
    {
        unitSize = (pExtraData[0]<<8 | pExtraData[1]); /* 两个字节表示这个unit的长度 */
        pExtraData += 2;
        totolSize += unitSize + sizeof(startCode);
        printf("unitSize:%d\n", unitSize);

        if(totolSize > INT_MAX - padding)
        {
            av_log(NULL, AV_LOG_ERROR,
                   "Too big extradata size, corrupted stream or invalid MP4/AVCC bitstream\n");
            av_free(pOut);
            return AVERROR(EINVAL);
        }

        if(pExtraData + unitSize > pCodecExtraData + codecExtraDataSize)
        {
            av_log(NULL, AV_LOG_ERROR, "Packet header is not contained in global extradata, "
                                       "corrupted stream or invalid MP4/AVCC bitstream\n");
            av_free(pOut);
            return AVERROR(EINVAL);
        }

        if((err = av_reallocp(&pOut, totolSize + padding)) < 0)
            return err;


        memcpy(pOut+totolSize-unitSize-sizeof(startCode), startCode, sizeof(startCode));
        memcpy(pOut+totolSize-unitSize, pExtraData, unitSize);

        pExtraData += unitSize;
    }

    pOutExtradata->data = pOut;
    pOutExtradata->size = totolSize;

    return len;
}

/* 将数据复制并且增加start      code */
static int alloc_and_copy(AVPacket *pOutPkt, const uint8_t *spspps, uint32_t spsppsSize,
                          const uint8_t *pIn, uint32_t inSize)
{
    int err;
    int startCodeLen = 3; /* start code长度 */

    /* 给pOutPkt->data分配内存 */
    err = av_grow_packet(pOutPkt, spsppsSize + inSize + startCodeLen);
    if (err < 0)
        return err;

    if (spspps)
    {
        memcpy(pOutPkt->data , spspps, spsppsSize); /* 拷贝SPS与PPS(前面分离的时候已经加了startcode(00 00 00 01)) */
    }

    /* 将真正的原始数据写入packet中 */
    (pOutPkt->data + spsppsSize)[0] = 0;
    (pOutPkt->data + spsppsSize)[1] = 0;
    (pOutPkt->data + spsppsSize)[2] = 1;
    memcpy(pOutPkt->data + spsppsSize + startCodeLen , pIn, inSize);

    return 0;
}

static int h264Mp4ToAnnexb(AVFormatContext *pAVFormatContext, AVPacket *pAvPkt, FILE *pFd)
{
    unsigned char *pData = pAvPkt->data; /* 帧数据 */
    unsigned char *pEnd = NULL;
    int dataSize = pAvPkt->size; /* pAvPkt->data的数据量 */
    int curSize = 0;
    int naluSize = 0;
    int i;
    unsigned char nalHeader, nalType;
    AVPacket spsppsPkt;
    AVPacket *pOutPkt;
    int ret;
    int len;

    pOutPkt = av_packet_alloc();
    pOutPkt->data = NULL;
    pOutPkt->size = 0;
    spsppsPkt.data = NULL;
    spsppsPkt.size = 0;

    pEnd = pData + dataSize;

    while(curSize < dataSize)
    {
        if(pEnd-pData < 4)
            goto fail;

        /* 前四个字节表示当前NALU的大小 */
        for(i = 0; i < 4; i++)
        {
            naluSize <<= 8;
            naluSize |= pData[i];
        }

        pData += 4;

        if(naluSize > (pEnd-pData+1) || naluSize <= 0)
        {
            goto fail;
        }

        nalHeader = *pData;
        nalType = nalHeader&0x1F;
        //LOGE("nalHeader:%d  ,nalType:%d, ref:%d",nalHeader,nalType,(nalHeader >> 5 )&0x03);
        if(nalType == 5)
        {
            /* 得到SPS与PPS（存在与codec->extradata中） */
            h264_extradata_to_annexb(pAVFormatContext->streams[pAvPkt->stream_index]->codec->extradata,
                                     pAVFormatContext->streams[pAvPkt->stream_index]->codec->extradata_size,
                                     &spsppsPkt, AV_INPUT_BUFFER_PADDING_SIZE);
            /* 添加start code */
            ret = alloc_and_copy(pOutPkt, spsppsPkt.data, spsppsPkt.size, pData, naluSize);
            if(ret < 0)
                goto fail;
        }
        else
        {
            /* 添加start code */
            ret = alloc_and_copy(pOutPkt, NULL, 0, pData, naluSize);
            if(ret < 0)
                goto fail;
        }

        /* 将处理好的数据写入文件中 */
        len = fwrite(pOutPkt->data, 1, pOutPkt->size, pFd);
        if(len != pOutPkt->size)
        {
            av_log(NULL, AV_LOG_DEBUG, "fwrite warning(%d, %d)!\n", len, pOutPkt->size);
        }

        /* 将数据从缓冲区写入磁盘 */
        fflush(pFd);

        curSize += (naluSize+4);
        pData += naluSize; /* 处理下一个NALU */
    }

    fail:
    av_packet_free(&pOutPkt);
    if(spsppsPkt.data)
    {
        free(spsppsPkt.data);
        spsppsPkt.data = NULL;
    }

    return 0;
}




int getAvPacketRefType(AVPacket *pAvPkt){
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








int avformat_callback(void *ctx){
    KzgFFmpeg *fFmpeg = (KzgFFmpeg *) ctx;
    if(fFmpeg->kzgPlayerStatus->exit)
    {
        return AVERROR_EOF;
    }
    return 0;
}

void KzgFFmpeg::decodeFFmpegThread() {
    LOGE("开始初始化ffmpeg 加载视频");
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
                kzgAudio = new KzgAudio(kzgPlayerStatus,avFormatContext->streams[i]->codecpar->sample_rate,helper);
                kzgAudio->streamIndex = i;
                kzgAudio->avCodecParameters = avFormatContext->streams[i]->codecpar;
                kzgAudio->duration = avFormatContext->duration / AV_TIME_BASE;
                kzgAudio->time_base = avFormatContext->streams[i]->time_base;
                if (duration <= 0){
                    duration = kzgAudio->duration;
                }

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
        getAVCodecContext(kzgAudio->avCodecParameters,&kzgAudio->avCodecContext);
    }
    if (kzgVideo !=NULL){
        kzgVideo->videoIndex = videoIndex;
        getAVCodecContext(kzgVideo->avCodecParameters,&kzgVideo->avCodecContext);
        //LOGE("视频的帧率:%d,   视频的时长%d  视频宽%d,  视频高%d",kzgVideo->fps,kzgVideo->duration,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height);
        helper->onCallVideoInfo(THREAD_CHILD,kzgVideo->fps,kzgVideo->duration,kzgVideo->avCodecContext->width,kzgVideo->avCodecContext->height);
    }

    //关闭非关键帧的环路滤波，seek 大GOP要快100ms左右
    kzgVideo->avCodecContext->skip_loop_filter = AVDISCARD_NONKEY;

    if(kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
        helper->onPrepare(videoIndex,THREAD_CHILD);
    } else{
        exit = true;
    }

    pthread_mutex_unlock(&init_mutex);

    LOGE("初始化ffmpeg加载视频结束");
}

void KzgFFmpeg::start() {
    LOGE("ffmpeg开始解码线程");
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

    //FILE *f;
    //char *outputfilename = "/data/data/com.example.ffmpegvideorange2/yuvtest.h264";
    //f = fopen(outputfilename, "ab+");
    kzgAudio->play();
    kzgVideo->play();
    while (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){

        if (!kzgVideo->kzgPlayerStatus->isFramePreview){
            //正常播放只缓存40帧解码前的音频
            if (kzgAudio->queue->getQueueSize() > 40){
                //LOGE("11111111111111");
                av_usleep(1000*100);
                continue;
            }
        }


        //缓存40帧avpacket
        if (kzgVideo->queue->getQueueSize() > 40){
            av_usleep(1000*20);
            //LOGE("222222222222222");
            continue;
        }

        if (kzgVideo->kzgPlayerStatus->isFramePreview){
            if (kzgVideo->kzgPlayerStatus->isBackSeekFramePreview && kzgVideo->queue->getQueueSize() > 5 && kzgVideo->kzgPlayerStatus->isSeekPause){
                //LOGE("333333333333");
                av_usleep(1000*1);
                continue;
            } else{
                int queueSize = 0;
                if (supportMediacodec){
                    queueSize = kzgVideo->helper->onCallJavaQueueSize(THREAD_CHILD);
                } else{
                    queueSize = kzgVideo->frameQueue->getQueueSize();
                }
                if (queueSize >= kzgVideo->cacheFrameNum){
                    //LOGE("4444444444444");
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
                //LOGE("开始解码音频第 %d 帧",count);
                kzgAudio->queue->putAvPacket(avPacket);

            }else if (avPacket->stream_index == kzgVideo->streamIndex){
                //开始解码视频
                count ++;
                //LOGE("avpacket pts %lld , %f",avPacket->pts,(avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE));
                if ( kzgVideo->avCodecContext->skip_frame != AVDISCARD_DEFAULT && (avPacket->pts *av_q2d( kzgVideo->time_base)* AV_TIME_BASE) > (kzgVideo->seekTime - 1000000)){
                    kzgVideo->avCodecContext->skip_frame = AVDISCARD_DEFAULT;
                } else if (kzgVideo->kzgPlayerStatus->isBackSeekFramePreview ){
                    kzgVideo->avCodecContext->skip_frame = AVDISCARD_NONREF;
                }
                kzgVideo->queue->putAvPacket(avPacket);
                /*if (getAvPacketRefType(avPacket) > 0){
                } else{
                    //LOGE("avpacket ref 为 0 ");
                }*/

                /*if (!f){
                    LOGE("open file fail ");
                } else{
                    h264Mp4ToAnnexb(avFormatContext, avPacket, f);
                }*/
            }else{
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
            tempIndex ++;
        } else{
            //LOGE("read frame fail %d",videoIndex);
            av_packet_free(&avPacket);
            av_free(avPacket);
            while (kzgPlayerStatus != NULL && !kzgPlayerStatus->exit){
                if (kzgVideo->queue->getQueueSize() > 0 || kzgVideo->frameQueue->getQueueSize() > 0 || kzgVideo->helper->onCallJavaQueueSize(THREAD_CHILD) > 0){
                    av_usleep(1000*20);
                    //LOGE("55555555555");
                    continue;
                } else{
                    //LOGE("777777777777777");
                    if (!kzgPlayerStatus->seeking){
                        //下面事播放完成退出，这里暂时先不用退出
                        av_usleep(1000*100);
                        //helper->onPlayStop(THREAD_CHILD);
                        /*if (!kzgVideo->kzgPlayerStatus->isFramePreview){
                            kzgPlayerStatus->exit = true;
                        }*/

                    }
                    break;
                }
            }
            //LOGE("666666666666666");
            continue;
            //break;
        }
    }

    if (helper != NULL){
        //helper->onComplete(THREAD_CHILD);
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

    if (sec < 0){
        sec = 0;
    }

    //LOGE("start  seeking %lld  , duration:%lld  ,index:%d",sec,duration ,videoIndex);
    if (sec > 0 && sec <= duration){
        pthread_mutex_lock(&seek_mutex);
        kzgPlayerStatus->seeking = true;
        int64_t res = sec/1000.0 * AV_TIME_BASE;
        if (kzgVideo != NULL){
            kzgVideo->seekTime = res;
            if (!kzgVideo->kzgPlayerStatus->isShowSeekFrame){
                pthread_mutex_unlock(&seek_mutex);
                kzgPlayerStatus->seeking = false;
                return;
            }
        }

        kzgVideo->kzgPlayerStatus->isShowSeekFrame = false;
        avformat_seek_file(avFormatContext,-1,INT64_MIN,res,res,0);
        //avformat_seek_file(avFormatContext,-1,INT64_MIN,res,INT64_MAX,0);
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
        kzgVideo->kzgPlayerStatus->isSeekPause = false;
        pthread_mutex_unlock(&seek_mutex);
        kzgPlayerStatus->seeking = false;
        //LOGE("***********seeking:%d",kzgPlayerStatus->seeking);
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
    (*avCodecContext)->thread_count = 2;

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
        if (!isFramePreview && kzgVideo->showFrameTimestamp > 0){
            kzgAudio->queue->clearByBeforeTime(kzgVideo->showFrameTimestamp,kzgAudio->time_base);
        }

        if (!isFramePreview){
            kzgVideo->kzgPlayerStatus->isBackSeekFramePreview = false;
        }
        kzgVideo->setIsFramePreview(isFramePreview);
        LOGE("isFramePreview:%d   ,seeking:%d",isFramePreview,kzgPlayerStatus->seeking);
    }
}

void *t_showframe(void * arg){
    KzgFFmpeg *kzgFFmpeg = static_cast<KzgFFmpeg *>(arg);
    while (kzgFFmpeg->kzgVideo->kzgPlayerStatus->isBackSeekForAdvance&& !kzgFFmpeg->kzgVideo->kzgPlayerStatus->exit){
        av_usleep(1000 * 10);
        kzgFFmpeg->kzgVideo->showFrame(kzgFFmpeg->kzgVideo->showFrameTimestamp);
    }
    return 0;
}

void KzgFFmpeg::showFrame(double timestamp) {
    if (kzgVideo != NULL){
        if (kzgVideo->kzgPlayerStatus->isBackSeekForAdvance){
            kzgVideo->showFrameTimestamp = timestamp;
        }
        kzgVideo->showFrame(timestamp);
    }
}

void KzgFFmpeg::showFrameFromSeek(double timestamp) {
    if (kzgVideo != NULL){
        //kzgVideo->kzgPlayerStatus->frameSeeking = true;
    }
}

void KzgFFmpeg::setSeekType(int type,int forAdvance) {
    if (kzgVideo != NULL){
        if (type == 0){
            //回退
            kzgVideo->kzgPlayerStatus->isBackSeekFramePreview = true;
        } else{
            //前进
            kzgVideo->kzgPlayerStatus->isBackSeekFramePreview = false;
        }
        if (forAdvance != 0){
            kzgVideo->kzgPlayerStatus->isBackSeekForAdvance = true;
        } else{
            kzgVideo->kzgPlayerStatus->isBackSeekForAdvance = false;
        }
    }
}

