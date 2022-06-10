package com.sam.video.timeline.helper

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

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
    var isPause:Boolean
    //每一帧的间隔时间 单位：毫秒
    var itemFrameForTime:Long

    fun init()
    /**
     * 获取指定时间的帧数据，并显示到指定的view中
     *
     */
    fun loadAvFrame(view: RecyclerView.ViewHolder, timeMs:Long)

    fun addAvFrame(view:ImageView)

    fun loadAvFrame(view: ImageView, timeMs:Long)

    fun removeAvFrameTag(view:ImageView)

    fun removeAvFrame()

    fun release()

    fun seek()

    fun pause()

    interface DecodeFrameListener{
        fun onGetOneFrame()
    }
}