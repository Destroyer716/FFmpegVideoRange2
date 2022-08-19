//
// Created by Administrator on 2020/4/22.
//

#include "JavaCallHelper.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM,JNIEnv * _env,jobject &_jobj):javaVm(_javaVM),env(_env) {

    jobj = env->NewGlobalRef(_jobj);
    jclass jclass1 = env->GetObjectClass(jobj);

    jmid_error = env->GetMethodID(jclass1,"onError","(ILjava/lang/String;)V");
    jmid_prepera = env->GetMethodID(jclass1,"onPrepare","(I)V");
    jmid_progress = env->GetMethodID(jclass1,"onProgress","(JJ)V");
    jmid_load = env->GetMethodID(jclass1,"onLoad","(Z)V");
    jmid_timeInfo = env->GetMethodID(jclass1,"onTimeInfo","(II)V");
    jmid_complete = env->GetMethodID(jclass1,"onComplete","()V");
    jmid_playStop = env->GetMethodID(jclass1,"onPlayStop","()V");
    jmid_getDB = env->GetMethodID(jclass1,"onDB","(I)V");
    jmid_pcmToAac = env->GetMethodID(jclass1,"enCodecPCMToAAC","(I[B)V");
    jmid_renderyuv = env->GetMethodID(jclass1,"onCallRenderYUV","(II[B[B[BI)V");
    jmid_mediacodecsuper = env->GetMethodID(jclass1,"onCallIsSupportMediaCodec","(Ljava/lang/String;)Z");
    jmid_initMediaCodecVideo = env->GetMethodID(jclass1,"initMediaCodecVideo","(Ljava/lang/String;II[B[B)V");
    jmid_decodeAVPacket = env->GetMethodID(jclass1,"decodeAVPacket","(I[B)V");
    jmid_initMediaCodecVideoYUV = env->GetMethodID(jclass1,"initMediaCodecVideoByYUV","(Ljava/lang/String;II[B[B)V");
    jmid_decodeAVPacketYUV = env->GetMethodID(jclass1,"decodeAVPacketForYUV","(I[BIID)V");
    jmid_calljavaqueuesize = env->GetMethodID(jclass1,"onCallJavaQueueSize","()I");
    jmid_getVideoInfo = env->GetMethodID(jclass1,"onGetVideoInfo","(IJII)V");
    jmid_enablePlay = env->GetMethodID(jclass1,"onEnableStartPlay","(Z)V");
    jmid_onGetFrameInitSuccess = env->GetMethodID(jclass1,"onGetFrameInitSuccess","(Ljava/lang/String;II[B[BI)V");
    jmid_onGetFramePacket = env->GetMethodID(jclass1,"getFramePacket","(ID[BI)V");
    jmid_onCallYUVToBitmap = env->GetMethodID(jclass1,"onCallYUVToBitmap","(II[B[B[BIDI)V");
    jmid_onCallYUVToBitmap2 = env->GetMethodID(jclass1,"onCallYUVToBitmap2","(II[BID)V");
}

JavaCallHelper::~JavaCallHelper() {
    //env->DeleteGlobalRef(jobj);
    jobj = NULL;
}

void JavaCallHelper::onError(int code, char *msg,int thread) {

    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jstring jmsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj,jmid_error,code,jmsg);
        javaVm->DetachCurrentThread();
    } else{
        jstring jmsg = env->NewStringUTF(msg);
        env->CallVoidMethod(jobj,jmid_error,code,jmsg);
    }
}

void JavaCallHelper::onPrepare(int index,int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_prepera,index);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_prepera,index);
    }

}


void JavaCallHelper::onProgress(int64_t current,int64_t total, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_progress,current,total);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_progress,current,total);
    }
}

void JavaCallHelper::onLoad(bool isLoad, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_load,isLoad);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_load,isLoad);
    }
}

void JavaCallHelper::onTimeInfo(int curr, int total, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_timeInfo,curr,total);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_timeInfo,curr,total);
    }
}

void JavaCallHelper::onComplete(int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_complete);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_complete);
    }
}

void JavaCallHelper::onPlayStop(int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_playStop);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_playStop);
    }
}


void JavaCallHelper::onGetDB(int db, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_getDB,db);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_getDB,db);
    }
}

void JavaCallHelper::onPcmToAac(int size, void *data, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }
        jbyteArray byteArr = jniEnv->NewByteArray(size);
        jniEnv->SetByteArrayRegion(byteArr, 0, size, static_cast<const jbyte *>(data));

        jniEnv->CallVoidMethod(jobj,jmid_pcmToAac,size,byteArr);
        jniEnv->DeleteLocalRef(byteArr);
        javaVm->DetachCurrentThread();
    } else{

        jbyteArray byteArr = env->NewByteArray(size);
        env->SetByteArrayRegion(byteArr, 0, size, static_cast<const jbyte *>(data));
        env->CallVoidMethod(jobj,jmid_pcmToAac,size,byteArr);
        env->DeleteLocalRef(byteArr);
    }
}

void JavaCallHelper::onCallRenderYUV(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv,int practicalWidth,int thread ) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }

        jbyteArray y = jniEnv->NewByteArray(width * height);
        jniEnv->SetByteArrayRegion(y, 0,width * height, reinterpret_cast<const jbyte *>(fy));

        jbyteArray u = jniEnv->NewByteArray(width * height / 4);
        jniEnv->SetByteArrayRegion(u, 0,width * height / 4, reinterpret_cast<const jbyte *>(fu));

        jbyteArray v = jniEnv->NewByteArray(width * height / 4);
        jniEnv->SetByteArrayRegion(v, 0,width * height / 4, reinterpret_cast<const jbyte *>(fv));

        jniEnv->CallVoidMethod(jobj,jmid_renderyuv,width,height,y,u,v,practicalWidth);

        jniEnv->DeleteLocalRef(y);
        jniEnv->DeleteLocalRef(u);
        jniEnv->DeleteLocalRef(v);

        javaVm->DetachCurrentThread();
    } else{
        jbyteArray y = env->NewByteArray(width * height);
        env->SetByteArrayRegion(y, 0,width * height, reinterpret_cast<const jbyte *>(fy));

        jbyteArray u = env->NewByteArray(width * height / 4);
        env->SetByteArrayRegion(u, 0,width * height / 4, reinterpret_cast<const jbyte *>(fu));

        jbyteArray v = env->NewByteArray(width * height / 4);
        env->SetByteArrayRegion(v, 0,width * height / 4, reinterpret_cast<const jbyte *>(fv));
        env->CallVoidMethod(jobj,jmid_renderyuv,width,height,y,u,v,practicalWidth);

        env->DeleteLocalRef(y);
        env->DeleteLocalRef(u);
        env->DeleteLocalRef(v);
    }


}

bool JavaCallHelper::onCallIsSupperMediaCodec(const char *ffcodecname, int thread) {
    bool isSupper = false;
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return isSupper;
        }

        jstring codecName = jniEnv->NewStringUTF(ffcodecname);
        isSupper = jniEnv->CallBooleanMethod(jobj,jmid_mediacodecsuper,codecName);
        jniEnv->DeleteLocalRef(codecName);
        javaVm->DetachCurrentThread();
    } else{
        jstring codecName = env->NewStringUTF(ffcodecname);
        isSupper = env->CallBooleanMethod(jobj,jmid_mediacodecsuper,codecName);
        env->DeleteLocalRef(codecName);
    }
    return isSupper;
}

void
JavaCallHelper::onCallInitMediaCodec(const char *codecName, int width, int height, int csd0_size, int csd1_size, uint8_t *csd_0,
                                     uint8_t *csd_1) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }
    jstring name = jniEnv->NewStringUTF(reinterpret_cast<const char *>(codecName));
    jbyteArray csd0 = jniEnv->NewByteArray(csd0_size);
    jniEnv->SetByteArrayRegion(csd0, 0, csd0_size, reinterpret_cast<const jbyte *>(csd_0));
    jbyteArray csd1 = jniEnv->NewByteArray(csd1_size);
    jniEnv->SetByteArrayRegion(csd1, 0, csd1_size, reinterpret_cast<const jbyte *>(csd_1));

    jniEnv->CallVoidMethod(jobj,jmid_initMediaCodecVideo,name,width,height,csd0,csd1);
    jniEnv->DeleteLocalRef(name);
    jniEnv->DeleteLocalRef(csd0);
    jniEnv->DeleteLocalRef(csd1);
    javaVm->DetachCurrentThread();
}

void JavaCallHelper::onCallDecodeAVPacket(int dataSize, uint8_t *data) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }

    jbyteArray avpacket = jniEnv->NewByteArray(dataSize);
    jniEnv->SetByteArrayRegion(avpacket, 0, dataSize, reinterpret_cast<const jbyte *>(data));

    jniEnv->CallVoidMethod(jobj,jmid_decodeAVPacket,dataSize,avpacket);
    jniEnv->DeleteLocalRef(avpacket);
    javaVm->DetachCurrentThread();
}

void JavaCallHelper::onCallInitMediaCodecByYUV(const char *codecName, int width, int height,
                                               int csd0_size, int csd1_size, uint8_t *csd_0,
                                               uint8_t *csd_1) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }

    jstring name = jniEnv->NewStringUTF(reinterpret_cast<const char *>(codecName));
    jbyteArray csd0 = jniEnv->NewByteArray(csd0_size);
    jniEnv->SetByteArrayRegion(csd0, 0, csd0_size, reinterpret_cast<const jbyte *>(csd_0));
    jbyteArray csd1 = jniEnv->NewByteArray(csd1_size);
    jniEnv->SetByteArrayRegion(csd1, 0, csd1_size, reinterpret_cast<const jbyte *>(csd_1));

    jniEnv->CallVoidMethod(jobj,jmid_initMediaCodecVideoYUV,name,width,height,csd0,csd1);
    jniEnv->DeleteLocalRef(name);
    jniEnv->DeleteLocalRef(csd0);
    jniEnv->DeleteLocalRef(csd1);
    javaVm->DetachCurrentThread();
}

void JavaCallHelper::onCallDecodeAVPacketByYUV(int dataSize, uint8_t *data, int width, int height, double timstamp) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }

    jbyteArray avpacket = jniEnv->NewByteArray(dataSize);
    jniEnv->SetByteArrayRegion(avpacket, 0, dataSize, reinterpret_cast<const jbyte *>(data));

    jniEnv->CallVoidMethod(jobj,jmid_decodeAVPacketYUV,dataSize,avpacket,width,height,timstamp);
    jniEnv->DeleteLocalRef(avpacket);
    javaVm->DetachCurrentThread();
}

int JavaCallHelper::onCallJavaQueueSize(int thread) {
    int size = 0;
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return size;
        }

        size = jniEnv->CallIntMethod(jobj,jmid_calljavaqueuesize);
        javaVm->DetachCurrentThread();
    } else{
        size = env->CallIntMethod(jobj,jmid_calljavaqueuesize);
    }
    return size;
}

void
JavaCallHelper::onCallVideoInfo(int thread, int fps, int64_t duration, int width, int height) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
            return;
        }

        jniEnv->CallVoidMethod(jobj,jmid_getVideoInfo,fps,duration,width,height);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_getVideoInfo,fps,duration,width,height);
    }
}

void JavaCallHelper::onEnablePlay(bool enable, int thread) {
    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_enablePlay,enable);
        javaVm->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_enablePlay,enable);
    }
}

void JavaCallHelper::onGetFrameInitSuccess(const char *codecName,int width,int height, int csd0_size, int csd1_size,uint8_t *csd_0,uint8_t *csd_1,int index) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }
    jstring name = jniEnv->NewStringUTF(reinterpret_cast<const char *>(codecName));
    jbyteArray csd0 = jniEnv->NewByteArray(csd0_size);
    jniEnv->SetByteArrayRegion(csd0, 0, csd0_size, reinterpret_cast<const jbyte *>(csd_0));
    jbyteArray csd1 = jniEnv->NewByteArray(csd1_size);
    jniEnv->SetByteArrayRegion(csd1, 0, csd1_size, reinterpret_cast<const jbyte *>(csd_1));

    jniEnv->CallVoidMethod(jobj,jmid_onGetFrameInitSuccess,name,width,height,csd0,csd1,index);
    jniEnv->DeleteLocalRef(name);
    jniEnv->DeleteLocalRef(csd0);
    jniEnv->DeleteLocalRef(csd1);
    javaVm->DetachCurrentThread();
}

void JavaCallHelper::onGetFramePacket(int dataSize, double pts, uint8_t *data,int index) {
    JNIEnv *jniEnv;
    if(javaVm->AttachCurrentThread(&jniEnv,0) != JNI_OK || jobj == NULL){
        return;
    }

    jbyteArray avpacket = jniEnv->NewByteArray(dataSize);
    jniEnv->SetByteArrayRegion(avpacket, 0, dataSize, reinterpret_cast<const jbyte *>(data));

    jniEnv->CallVoidMethod(jobj,jmid_onGetFramePacket,dataSize,pts,avpacket,index);
    jniEnv->DeleteLocalRef(avpacket);
    javaVm->DetachCurrentThread();
}

void JavaCallHelper::onCallYUVToBitmap(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv,
                                       int practicalWidth,double pts,int index, int thread) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK || jobj == NULL) {
            return;
        }

        jbyteArray y = jniEnv->NewByteArray(width * height);
        jniEnv->SetByteArrayRegion(y, 0, width * height, reinterpret_cast<const jbyte *>(fy));

        jbyteArray u = jniEnv->NewByteArray(width * height / 4);
        jniEnv->SetByteArrayRegion(u, 0, width * height / 4, reinterpret_cast<const jbyte *>(fu));

        jbyteArray v = jniEnv->NewByteArray(width * height / 4);
        jniEnv->SetByteArrayRegion(v, 0, width * height / 4, reinterpret_cast<const jbyte *>(fv));

        jniEnv->CallVoidMethod(jobj, jmid_onCallYUVToBitmap, width, height, y, u, v, practicalWidth,
                               pts,index);

        jniEnv->DeleteLocalRef(y);
        jniEnv->DeleteLocalRef(u);
        jniEnv->DeleteLocalRef(v);

        javaVm->DetachCurrentThread();
    } else {
        jbyteArray y = env->NewByteArray(width * height);
        env->SetByteArrayRegion(y, 0, width * height, reinterpret_cast<const jbyte *>(fy));

        jbyteArray u = env->NewByteArray(width * height / 4);
        env->SetByteArrayRegion(u, 0, width * height / 4, reinterpret_cast<const jbyte *>(fu));

        jbyteArray v = env->NewByteArray(width * height / 4);
        env->SetByteArrayRegion(v, 0, width * height / 4, reinterpret_cast<const jbyte *>(fv));
        env->CallVoidMethod(jobj, jmid_onCallYUVToBitmap, width, height, y, u, v, practicalWidth,
                            pts,index);

        env->DeleteLocalRef(y);
        env->DeleteLocalRef(u);
        env->DeleteLocalRef(v);
    }
}

 void JavaCallHelper::onCallYUVToBitmap2(int width, int height, uint8_t *fyuv, int practicalWidth,double pts, int thread) {
        if (thread == THREAD_CHILD) {
            JNIEnv *jniEnv;
            if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK || jobj == NULL) {
                return;
            }

            jbyteArray yuv = jniEnv->NewByteArray(width * height * 3 / 2);
            jniEnv->SetByteArrayRegion(yuv, 0, width * height, reinterpret_cast<const jbyte *>(fyuv));
            jniEnv->CallVoidMethod(jobj, jmid_onCallYUVToBitmap2, width, height, yuv, practicalWidth, pts);
            jniEnv->DeleteLocalRef(yuv);
            javaVm->DetachCurrentThread();
        } else {
            jbyteArray yuv = env->NewByteArray(width * height * 3 / 2);
            env->SetByteArrayRegion(yuv, 0, width * height, reinterpret_cast<const jbyte *>(fyuv));
            env->CallVoidMethod(jobj, jmid_onCallYUVToBitmap2, width, height, yuv,practicalWidth, pts);
            env->DeleteLocalRef(yuv);
        }

}

