package com.sam.video.timeline.bean


/**
 * 视频美化视频片段
 * @author WangYingHao
 * @since 2019-08-03
 */
data class VideoClip(
    var id: String, //唯一标识
    var originalFilePath: String,//文件路径
    var originalDurationMs: Long = 0,//原始文件时长
    var startAtMs: Long = 0, //视频有效起始时刻
    var endAtMs: Long = 0//视频有效结束时刻
){
    val durationMs: Long //视频有效播放时长，受节选、速度、转场吃掉时间影响
        get() {
            return endAtMs - startAtMs
        }
}