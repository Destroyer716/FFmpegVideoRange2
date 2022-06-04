package com.sam.video.timeline.helper

import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView

/**
 * 视频抽帧接口
 */
interface IAvFrameHelper {
    var filePath:String
    var onGetFrameBitmapCallback:OnGetFrameBitmapCallback?

    var lastBitMap : Bitmap?
    var decodeFrameListener:DecodeFrameListener?
    var isSeekBack:Boolean
    var isScrolling:Boolean
    var isInitItem:Boolean

    fun init()
    /**
     * 获取指定时间的帧数据，并显示到指定的view中
     *
     */
    fun loadAvFrame(view:ImageView,timeMs:Long)

    fun setTargetTime(timeUs:Long)

    fun clearTargetTimeMap()

    fun release()

    fun seek()

    fun pause()

    interface DecodeFrameListener{
        fun onGetOneFrame()
    }
}