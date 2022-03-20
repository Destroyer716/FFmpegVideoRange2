//
// Created by Administrator on 2020/5/10.
//

#ifndef MYCSDNMUSICPLAYER_KZGPLAYERSTATUS_H
#define MYCSDNMUSICPLAYER_KZGPLAYERSTATUS_H


class KzgPlayerStatus {

public:
    KzgPlayerStatus();
    ~KzgPlayerStatus();

public:
    bool exit;
    bool loading = true;
    bool seeking  = false;
    //是否初始化了
    bool isInited = false;
    bool frameSeeking = true;
    bool isPause = false;
    //是否是预览模式
    bool isFramePreview = false;
    //视频帧的linesize[0] 是否大于 视频帧的width，大于的话就需要裁剪
    bool isCrop = false;
    //是否是通过seek的方式来逐帧预览
    bool isBackSeekFramePreview = false;
    //是否处于seek预览下的暂停状态，也就是即没有正常播放，也没有在seek,并且已经显示了目标帧图像
    bool isSeekPause = true;
    //是否显示了seek的那一帧图片，主要是为了控制能一边后退seek，一边解码，加快显示的速度
    bool isShowSeekFrame = true;

};


#endif //MYCSDNMUSICPLAYER_KZGPLAYERSTATUS_H
