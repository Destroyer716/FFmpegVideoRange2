//
// Created by Administrator on 2020/5/10.
//

#include "KzgAudio.h"


#define VOL_FILE_PATH "/storage/emulated/0/test/new_test_pcm.pcm"


KzgAudio::KzgAudio(KzgPlayerStatus *kzgPlayerStatus,int sample_rate,JavaCallHelper *helper) {
    this->playerStatus = kzgPlayerStatus;
    this->queue = new SafeQueue(kzgPlayerStatus);
    this->helper = helper;
    buffer = static_cast<uint8_t *>(av_malloc(sample_rate * 2 * 2));
    this->sample_rate = sample_rate;

    sampleBuffer = static_cast<soundtouch::SAMPLETYPE *>(malloc(sample_rate * 2 * 2));
    soundTouch = new soundtouch::SoundTouch();
    soundTouch->setSampleRate(sample_rate);
    soundTouch->setChannels(2);
    soundTouch->setPitch(pitch);
    soundTouch->setTempo(speed);
    pthread_mutex_init(&codecMute,NULL);
    fp_out = fopen(VOL_FILE_PATH, "wb+");
}

KzgAudio::~KzgAudio() {
    LOGE("ddddddd");
    pthread_mutex_destroy(&codecMute);
    if (fp_out != NULL){
        fclose(fp_out);
    }
}

void *decodePlay(void *arg){
    KzgAudio *kzgAudio = static_cast<KzgAudio *>(arg);
    kzgAudio->initOpenSLES();
    return 0;
}

void KzgAudio::play() {
    if (playerStatus != NULL && !playerStatus->exit){
        pthread_create(&playThread,NULL,decodePlay,this);
    }
}

int KzgAudio::play_t(void **pcmbuf) {
    data_size = 0;
    while (playerStatus != NULL && !playerStatus->exit){

        if (playerStatus->seeking){
            av_usleep(1000*100);
            continue;
        }

        if(queue->getQueueSize() == 0)//加载中
        {
            if(!playerStatus->loading)
            {
                playerStatus->loading = true;
                helper->onLoad(true,THREAD_CHILD);
            }
            av_usleep(1000*100);
            continue;
        } else{
            if(playerStatus->loading)
            {
                playerStatus->loading = false;
                helper->onLoad(false,THREAD_CHILD);
            }
        }

        pthread_mutex_lock(&codecMute);
        if (isReadFrameFinish){
            avPacket = av_packet_alloc();
            if (queue->getAvPacket(avPacket) != 0){
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                pthread_mutex_unlock(&codecMute);
                continue;
            }
            ret = avcodec_send_packet(avCodecContext,avPacket);
            if (ret != 0){
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                pthread_mutex_unlock(&codecMute);
                continue;
            }
        }


        avFrame = av_frame_alloc();
        ret = avcodec_receive_frame(avCodecContext,avFrame);
        if (ret != 0){
            isReadFrameFinish = true;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            pthread_mutex_unlock(&codecMute);
            continue;
        }
        isReadFrameFinish = false;

        if (avFrame->channels > 0 && avFrame->channel_layout == 0){
            avFrame->channel_layout = av_get_default_channel_layout(avFrame->channels);
        } else if (avFrame->channels == 0 && avFrame->channel_layout > 0){
            avFrame->channels = av_get_channel_layout_nb_channels(avFrame->channel_layout);
        }

        SwrContext *swrContext;
        swrContext = swr_alloc_set_opts(NULL, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, avFrame->sample_rate,
                                        avFrame->channel_layout,
                                        static_cast<AVSampleFormat>(avFrame->format), avFrame->sample_rate, NULL, NULL);


        if (!swrContext || swr_init(swrContext) < 0){
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            swr_free(&swrContext);
            isReadFrameFinish = true;
            pthread_mutex_unlock(&codecMute);
            continue;
        }

        nb = swr_convert(swrContext, &buffer, avFrame->nb_samples,
                             reinterpret_cast<const uint8_t **>(&avFrame->data), avFrame->nb_samples);

        int out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
        data_size = nb * out_channels *av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);

        now_frame_time = avFrame->pts * av_q2d(time_base);
        if (now_frame_time < clock){
            now_frame_time = clock;
        }
        clock = now_frame_time;
        *pcmbuf = buffer;
        //LOGE("data_size is %d", data_size);
        /*av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;*/
        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = NULL;
        swr_free(&swrContext);
        pthread_mutex_unlock(&codecMute);
        break;
    }
    return data_size;
}


void amplifyPcmVol(int db,short  * in_buf,int size){

    int32_t pcmval;
    float multiplier = pow(10,db/20);
    for (int ctr = 0; ctr < size/2; ctr++) {
        pcmval = in_buf[ctr] * multiplier;
        if (pcmval < 32767 && pcmval > -32768) {
            in_buf[ctr] = pcmval;
        } else if (pcmval > 32767) {
            in_buf[ctr] = 32767;
        } else if (pcmval < -32768) {
            in_buf[ctr] = -32768;
        }
    }
}



void pcmBufferCallBack(SLAndroidSimpleBufferQueueItf bf, void * context){
    KzgAudio *kzgAudio = static_cast<KzgAudio *>(context);
    if (kzgAudio != NULL){
        int bufferSize = kzgAudio->getSoundTouchData();
        if (bufferSize > 0){
            //计算已播放的时长,固定的公式
            kzgAudio->clock += bufferSize / (double)((kzgAudio->sample_rate) * 2 * 2);
            //不需要调用那么频繁，所以加个限制
            if (kzgAudio->clock - kzgAudio->last_time >= 0.2){
                kzgAudio->last_time = kzgAudio->clock;
                kzgAudio->helper->onTimeInfo(kzgAudio->clock,kzgAudio->duration,THREAD_CHILD);
            }



            //改变pcm实际的音量
            for (int i = 0; i <bufferSize * 2 * 2 ; i+=1) {
                kzgAudio->volume_adjust((kzgAudio->sampleBuffer + i),kzgAudio->amplifyVol);
            }

            //保存音频pcm文件
            //fwrite(kzgAudio->sampleBuffer,1,bufferSize * 2 * 2,kzgAudio->fp_out);

            //kzgAudio->helper->onPcmToAac(bufferSize *2*2,kzgAudio->sampleBuffer, THREAD_CHILD);
            /*kzgAudio->helper->onGetDB(kzgAudio->getPCMDB(
                    reinterpret_cast<char *>(kzgAudio->sampleBuffer), bufferSize * 2 * 2), THREAD_CHILD);*/


            (*kzgAudio->pcmBufferQueue)->Enqueue(bf,kzgAudio->sampleBuffer,bufferSize*2*2);
        }
    }
}

void KzgAudio::initOpenSLES() {
    SLresult result;
    result = slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);

    //第二步，创建混音器
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, mids, mreq);
    (void)result;
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    (void)result;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
        result = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                outputMixEnvironmentalReverb, &reverbSettings);
        (void)result;
    }
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&outputMix, 0};


    // 第三步，配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue={SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,2};

    SLDataFormat_PCM pcm={
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            2,//2个声道（立体声）
            getCurrentSampleRateForOpensles(sample_rate),//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };
    SLDataSource slDataSource = {&android_queue, &pcm};


    const SLInterfaceID ids[4] = {SL_IID_BUFFERQUEUE,SL_IID_MUTESOLO,SL_IID_VOLUME,SL_IID_PLAYBACKRATE};
    const SLboolean req[4] = {SL_BOOLEAN_TRUE,SL_BOOLEAN_TRUE,SL_BOOLEAN_TRUE,SL_BOOLEAN_TRUE};

    (*engineEngine)->CreateAudioPlayer(engineEngine, &pcmPlayerObject, &slDataSource, &audioSnk, 4, ids, req);
    //初始化播放器
    (*pcmPlayerObject)->Realize(pcmPlayerObject, SL_BOOLEAN_FALSE);

//    得到接口后调用  获取Player接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_PLAY, &pcmPlayerPlay);
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_VOLUME, &pcmPlayVolume);
    setVolume(volume);

//    注册回调缓冲区 获取缓冲队列接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_BUFFERQUEUE, &pcmBufferQueue);
    //缓冲接口回调
    (*pcmBufferQueue)->RegisterCallback(pcmBufferQueue, pcmBufferCallBack, this);
//    获取播放状态接口
    (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    pcmBufferCallBack(pcmBufferQueue, this);
}

SLuint32 KzgAudio::getCurrentSampleRateForOpensles(int sample_rale) {
    int rate = 0;
    switch (sample_rale)
    {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            rate = SL_SAMPLINGRATE_11_025;
            break;
        case 12000:
            rate = SL_SAMPLINGRATE_12;
            break;
        case 16000:
            rate = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            rate = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            rate = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            rate = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            rate = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            rate = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            rate = SL_SAMPLINGRATE_192;
            break;
        default:
            rate =  SL_SAMPLINGRATE_44_1;
    }
    return rate;
}

void KzgAudio::pause() {
    if (pcmPlayerPlay != NULL){
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay,  SL_PLAYSTATE_PAUSED);
    }
}

void KzgAudio::resume() {
    if(pcmPlayerPlay != NULL)
    {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay,  SL_PLAYSTATE_PLAYING);
    }
}

void KzgAudio::stop() {
    if(pcmPlayerPlay != NULL){
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay,  SL_PLAYSTATE_STOPPED);
    }
}

void KzgAudio::release() {

    if (queue != NULL){
        //有可能此时线程真处于堵塞状态，这里是通知下队列不再堵塞，继续执行，就能正常退出队列
        queue->noticeQueue();
    }
    //这里等待线程退出再继续执行
    pthread_join(playThread,NULL);

    if (queue != NULL){
        delete queue;
        queue = NULL;
    }

    if (pcmPlayerObject != NULL){
        (*pcmPlayerObject)->Destroy(pcmPlayerObject);
        pcmPlayerObject = NULL;
        pcmPlayerPlay = NULL;
        pcmBufferQueue = NULL;
        pcmPlayVolume = NULL;
    }

    if (outputMixObject != NULL){
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        outputMixEnvironmentalReverb = NULL;
    }

    if (engineObject != NULL){
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;

    }

    if (buffer != NULL){
        free(buffer);
        buffer = NULL;
    }

    if (out_buffer != NULL){
        out_buffer = NULL;
    }

    if (soundTouch != NULL){
        delete soundTouch;
        soundTouch = NULL;
    }

    if (sampleBuffer != NULL){
        free(sampleBuffer);
        sampleBuffer = NULL;
    }

    if (avCodecContext != NULL){
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }

    if (playerStatus != NULL){
        playerStatus = NULL;
    }

    if (helper != NULL){
        helper = NULL;
    }

}

void KzgAudio::setVolume(int volume) {
    this->volume = volume;
    if(pcmPlayVolume != NULL)
    {
        if(volume > 30)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -20);
        }
        else if(volume > 25)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -22);
        }
        else if(volume > 20)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -25);
        }
        else if(volume > 15)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -28);
        }
        else if(volume > 10)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -30);
        }
        else if(volume > 5)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -34);
        }
        else if(volume > 3)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -37);
        }
        else if(volume > 0)
        {
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -40);
        }
        else{
            (*pcmPlayVolume)->SetVolumeLevel(pcmPlayVolume, (100 - volume) * -100);
        }
    }
}

int KzgAudio::getSoundTouchData() {
    while(playerStatus != NULL && !playerStatus->exit)
    {
        out_buffer = NULL;
        if(finished)
        {
            finished = false;
            data_size = play_t(reinterpret_cast<void **>(&out_buffer));
            if(data_size > 0)
            {
                for(int i = 0; i < data_size / 2 + 1; i++)
                {
                    sampleBuffer[i] = (out_buffer[i * 2] | ((out_buffer[i * 2 + 1]) << 8));
                }
                soundTouch->putSamples(sampleBuffer, nb);
                num = soundTouch->receiveSamples(sampleBuffer, data_size / 4);
            } else{
                soundTouch->flush();
            }
        }
        if(num == 0)
        {
            finished = true;
            continue;
        } else{
            if(out_buffer == NULL)
            {
                num = soundTouch->receiveSamples(sampleBuffer, data_size / 4);
                if(num == 0)
                {
                    finished = true;
                    continue;
                }
            }
            return num;
        }
    }
    return 0;
}

void KzgAudio::setPitch(float pi) {
    pitch = pi;
    if (soundTouch != NULL){
        soundTouch->setPitch(pitch);
    }
}

void KzgAudio::setSpeed(float sp) {
    speed = sp;
    if (soundTouch != NULL){
        soundTouch->setTempo(speed);
    }
}

int KzgAudio::getPCMDB(char *pcmcata, size_t pcmsize) {
    int db = 0;
    double sum = 0;
    short int pervalue = 0;
    for (int i = 0; i <pcmsize ; i+=2) {
        memcpy(&pervalue,pcmcata+i,2);
        sum += abs(pervalue);
    }

    sum = sum / (pcmsize / 2);
    if (sum > 0){
        db = (int) 20.0 * log10(sum);
    }
    return db;
}


int KzgAudio:: volume_adjust(short  * in_buf,  float in_vol)
{
    int tmp;

    float vol = 20.0 * (in_vol / 100.0);

    tmp = (*in_buf)*vol; // 上面所有关于vol的判断，其实都是为了此处*in_buf乘以一个倍数，你可以根据自己的需要去修改

    // 下面的code主要是为了溢出判断
    if(tmp > 32767)
        tmp = 32767;
    else if(tmp < -32768)
        tmp = -32768;
    *in_buf = tmp;

    return 0;
}

void KzgAudio::setAmplifyVol(float vol) {
    amplifyVol = vol;
}







