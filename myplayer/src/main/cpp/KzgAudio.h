//
// Created by Administrator on 2020/5/10.
//

#ifndef MYCSDNMUSICPLAYER_KZGAUDIO_H
#define MYCSDNMUSICPLAYER_KZGAUDIO_H

#include "pthread.h"
#include "SafeQueue.h"
#include "KzgPlayerStatus.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "JavaCallHelper.h"
#include "soundtouch/include/SoundTouch.h"
extern "C"{
#include "include/libavcodec/avcodec.h"
#include "include/libswresample/swresample.h"
#include "include/libavutil/time.h"
};



class KzgAudio {

public:
    KzgAudio(KzgPlayerStatus *kzgPlayerStatus,int sample_rate,JavaCallHelper *helper);
    ~KzgAudio();

    void play();
    int play_t(void **pcmbuf);
    void initOpenSLES();
    SLuint32 getCurrentSampleRateForOpensles(int sample_rale);
    void pause();
    void resume();
    void stop();
    void release();
    void setVolume(int volume);
    int getSoundTouchData();
    void setPitch(float pi);
    void setSpeed(float sp);
    int getPCMDB(char *pcmcata, size_t pcmsize);
    int volume_adjust(short  * in_buf,  float in_vol);
    void setAmplifyVol(float vol);

public:
    int streamIndex = -1;
    AVCodecContext *avCodecContext = 0;
    AVCodecParameters *avCodecParameters = 0;
    SafeQueue *queue = NULL;
    KzgPlayerStatus *playerStatus = NULL;
    pthread_t playThread;
    AVPacket *avPacket = NULL;
    AVFrame *avFrame = NULL;
    uint8_t *buffer = NULL;
    int data_size = 0;
    int sample_rate = 0;
    JavaCallHelper *helper = NULL;
    int ret = 0;
    //音频总时长
    int duration = 0;
    //已播放时长
    double clock = 0;
    //当前frame时间
    double now_frame_time = 0;
    //上一次调用的时间
    double last_time = 0;
    //时间基本单位，是一个结构体
    AVRational time_base;
    int volume = 50;
    float pitch = 1.0f;
    float speed = 1.0f;
    float amplifyVol = 5.0f;

    FILE *fp_out = NULL;
    bool isReadFrameFinish = true;
    pthread_mutex_t codecMute;


    soundtouch::SoundTouch *soundTouch = NULL;
    soundtouch::SAMPLETYPE *sampleBuffer = NULL;
    bool finished = true;
    uint8_t *out_buffer = NULL;
    int nb = 0;
    int num = 0;


    // 引擎接口
    SLObjectItf engineObject = NULL;
    SLEngineItf engineEngine = NULL;

    //混音器
    SLObjectItf outputMixObject = NULL;
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
    SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

    //pcm
    SLObjectItf pcmPlayerObject = NULL;
    SLPlayItf pcmPlayerPlay = NULL;
    SLVolumeItf pcmPlayVolume = NULL;

    //缓冲器队列接口
    SLAndroidSimpleBufferQueueItf pcmBufferQueue = NULL;

};


#endif //MYCSDNMUSICPLAYER_KZGAUDIO_H
