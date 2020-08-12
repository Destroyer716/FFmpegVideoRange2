//
// Created by Administrator on 2020/5/9.
//

#ifndef MYCSDNMUSICPLAYER_KZGFFMPEG_H
#define MYCSDNMUSICPLAYER_KZGFFMPEG_H

#include "log.h"
#include "JavaCallHelper.h"
#include "pthread.h"
#include "KzgAudio.h"
#include "KzgPlayerStatus.h"
#include "KzgVideo.h"

extern "C"{
#include "include/libavutil/time.h"
#include "include/libavformat/avformat.h"
};



class KzgFFmpeg {

public:
    KzgFFmpeg(JavaCallHelper *_helper,const char *_url,KzgPlayerStatus *kzgPlayerStatus);
    ~KzgFFmpeg();

    void parpared();
    void start();
    void decodeFFmpegThread();
    void pause();
    void resume();
    void stop();
    void seek(int64_t sec);
    int getDuration();
    void setVolume(int volume);
    void setPitch(float pi);
    void setSpeed(float sp);
    void setAmplifyVol(float vol);
    int getSampleRate();
    int getAVCodecContext(AVCodecParameters *avCodecParameters,AVCodecContext **avCodecContext);
    void setIsFramePreview(bool isFramePreview);
    void showFrame(double timestamp);
    void showFrameFromSeek(double timestamp);
    void setSeekType(int type);

public:
    pthread_t decodeThread;
    KzgPlayerStatus *kzgPlayerStatus;

private:
    JavaCallHelper *helper = NULL;
    const char *url = NULL;
    AVFormatContext *avFormatContext = NULL;
    KzgAudio *kzgAudio = NULL;
    KzgVideo *kzgVideo = NULL;

    pthread_mutex_t init_mutex;
    pthread_mutex_t seek_mutex;
    bool exit = false;
    int64_t duration = 0;
    bool supportMediacodec = false;
    const AVBitStreamFilter *bsFilter = NULL;
    int tempIndex = 0;

};


#endif //MYCSDNMUSICPLAYER_KZGFFMPEG_H
