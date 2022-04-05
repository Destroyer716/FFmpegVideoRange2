package com.sam.video.timeline.bean

data class VideoFrameData(
        var videoData: VideoClip,
        var time: Long, //这个帧片断 开始时间; 与整个视频的开始时间为-“相对时间”；例：任何视频片断的第一帧为时间为0
        var frameClipTime: Long, //这个片断 第一个合适的最小时间轴精度时间（避免片断开始时间一直变，图片一直刷）,这边是考虑裁剪后的时间-“本视频文件的绝对时间”；例：不同视频的第一帧时间为截断开始的时间附近
        var frameWidth: Int, //这一帧的宽度
        var isFirstItem: Boolean = false,
        var isLastItem: Boolean = false,
        var offsetX: Int = 0 //左边偏移，用于第一帧不是从起始位置开始时的开始位置
)