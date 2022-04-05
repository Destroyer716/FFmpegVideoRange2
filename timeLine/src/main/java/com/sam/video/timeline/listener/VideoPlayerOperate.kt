package com.sam.video.timeline.listener

import com.sam.video.timeline.widget.TimeChangeListener

/**
 * 视频美化首页 视频播放控件的操作接口
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-24
 */
interface VideoPlayerOperate : TimeChangeListener {
    /** 增删、排序后 更新视频信息 */
    fun updateVideoInfo()

    fun startTrackingTouch()

    fun stopTrackingTouch(ms: Long)
}