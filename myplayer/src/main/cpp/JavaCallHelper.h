//
// Created by Administrator on 2020/4/22.
//

#ifndef MYFFMPEGPLAYERTEST1_JAVACALLHELPER_H
#define MYFFMPEGPLAYERTEST1_JAVACALLHELPER_H

#include <jni.h>
#include "log.h"

#define THREAD_MAIN  1
#define THREAD_CHILD  2


//错误代码
//打不开视频
#define FFMPEG_CAN_NOT_OPEN_URL 1
//找不到流媒体
#define FFMPEG_CAN_NOT_FIND_STREAMS 2
//找不到解码器
#define FFMPEG_FIND_DECODER_FAIL 3
//无法根据解码器创建上下文
#define FFMPEG_ALLOC_CODEC_CONTEXT_FAIL 4
//根据流信息 配置上下文参数失败
#define FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL 6
//打开解码器失败
#define FFMPEG_OPEN_DECODER_FAIL 7
//没有音视频
#define FFMPEG_NOMEDIA 8






class JavaCallHelper {

public:
    JavaCallHelper(JavaVM *_javaVM,JNIEnv *_env,jobject &_jobj);

    ~JavaCallHelper();

    void onError(int code,char *msg,int thread=THREAD_MAIN);

    void onPrepare(int thread = THREAD_MAIN);
    void onProgress(int64_t current,int64_t total,int thread = THREAD_MAIN);
    void onLoad(bool isLoad,int thread=THREAD_MAIN);
    void onTimeInfo(int curr,int total,int thread = THREAD_MAIN);
    void onComplete(int thread = THREAD_MAIN);
    void onPlayStop(int thread = THREAD_MAIN);
    void onGetDB(int db,int thread = THREAD_MAIN);
    void onPcmToAac(int size,void *data,int thread = THREAD_MAIN);
    void onCallRenderYUV(int width,int height,uint8_t *fy, uint8_t *fu, uint8_t *fv,int practicalWidth,int thread = THREAD_MAIN);
    bool onCallIsSupperMediaCodec(const char *ffcodecname,int thread = THREAD_MAIN);
    void onCallInitMediaCodec(const char *codecName,int width,int height, int csd0_size, int csd1_size,uint8_t *csd_0,uint8_t *csd_1);
    void onCallDecodeAVPacket(int dataSize,uint8_t *data);
    void onCallInitMediaCodecByYUV(const char *codecName,int width,int height, int csd0_size, int csd1_size,uint8_t *csd_0,uint8_t *csd_1);
    void onCallDecodeAVPacketByYUV(int dataSize,uint8_t *data,int width,int height, double timstamp);
    int onCallJavaQueueSize(int thread);
    void onCallVideoInfo(int thread,int fps,int64_t duration,int width,int height);
    void onEnablePlay(bool enable,int thread);




    void onGetFrameInitSuccess(const char *codecName,int width,int height, int csd0_size, int csd1_size,uint8_t *csd_0,uint8_t *csd_1);
    void onGetFramePacket(int dataSize,double pts,uint8_t *data);
    void onCallYUVToBitmap(int width,int height,uint8_t *fy, uint8_t *fu, uint8_t *fv,int practicalWidth,double pts,int thread = THREAD_MAIN);
    void onCallYUVToBitmap2(int width,int height,uint8_t *fyuv,int practicalWidth,double pts,int thread = THREAD_MAIN);

public:
    JavaVM *javaVm;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmid_error;
    jmethodID jmid_prepera;
    jmethodID jmid_progress;
    jmethodID jmid_load;
    jmethodID jmid_timeInfo;
    jmethodID jmid_complete;
    jmethodID jmid_playStop;
    jmethodID jmid_getDB;
    jmethodID jmid_pcmToAac;
    jmethodID jmid_renderyuv;
    jmethodID jmid_mediacodecsuper;
    jmethodID jmid_initMediaCodecVideo;
    jmethodID jmid_decodeAVPacket;
    jmethodID jmid_initMediaCodecVideoYUV;
    jmethodID jmid_decodeAVPacketYUV;
    jmethodID jmid_calljavaqueuesize;
    jmethodID jmid_getVideoInfo;
    jmethodID jmid_enablePlay;
    jmethodID jmid_onGetFrameInitSuccess;
    jmethodID jmid_onGetFramePacket;
    jmethodID jmid_onCallYUVToBitmap;
    jmethodID jmid_onCallYUVToBitmap2;

};


#endif //MYFFMPEGPLAYERTEST1_JAVACALLHELPER_H
