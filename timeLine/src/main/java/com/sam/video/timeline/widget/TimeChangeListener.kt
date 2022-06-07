package com.sam.video.timeline.widget

interface TimeChangeListener {
    /** 因滑动 更新时间 单位时毫秒*/
    fun updateTimeByScroll(time: Long)

}