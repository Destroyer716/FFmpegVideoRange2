package com.sam.video.timeline.widget

import com.sam.video.TimeLineApp
import com.sam.video.util.dp2px
import com.sam.video.util.getScreenWidth

/**
 * 时间轴共同数据
 */
class TimeLineBaseValue {
    /** 总时间长度 - 毫秒*/
    public var duration: Long = 0L

    /** 时间 - 毫秒 */
    var time: Long = 0L
        set(value) {
            field = when {
                value < 0 -> 0
                value > duration -> duration
                else -> value
            }
//            Log.d("TimeLineBaseValue", "update Time : $time")
        }

    /**
     *  seek的时间  与时间轴时间不一样
     *  这个值有效时，用于视频预览seek到指定位置，而[time]用于时间轴定位，实现时间轴与视频预览差异seek
     * */
    var seekTime = -1L

    /** 时间轴比例 缩放倍数 */
    var scale: Float = 1.0F
        set(value) {
            field = when {
                value < minScale -> minScale
                value > maxScale -> maxScale
                else -> value
            }
            pxInSecond = standPxInSecond * field
//            Log.d("TimeLineBaseValue", "wds scaleChange() called $field")
        }

    /** 标准一秒几像素 */
    var standPxInSecond: Float = 1.0f
        set(value) {
            field = value
            pxInSecond = field * scale
        }

    /** 每秒长度对应几像素，会用于分母要保证不会0 */
    var pxInSecond: Float = 1.0f // pxInSecond = standPxInSecond * scale
        private set //始终通过内部运算得到

    /** 时间转成像素 */
    fun time2px(timeMs: Long): Float {
        return timeMs * pxInSecond / 1000
    }

    /** 像素转成时间*/
    fun px2time(px: Float): Long {
        return (px * 1000 / pxInSecond).toLong()
    }

    //
    /** 时间 -> X坐标
     * @param time 目标时间
     * @param cursorX 竖线（光标）的位置，一般在屏幕中央
     * @param currentTime 光标指向的时间
     * */
    fun time2X(time: Long, cursorX: Int, currentTime: Long = this.time): Float {
        val offsetTime = time - currentTime
        return cursorX + time2px(offsetTime)
    }

    /**
     * 重置标准选区
     * @param holdPxInSecond 尽量保持pxInSecond不变
     */
    fun resetStandPxInSecond(holdPxInSecond: Boolean = false) {

        //默认 65dp = 1s
        standPxInSecond = TimeLineApp.instance.dp2px(65f)
        //单段视频是一帧宽48dp 多段视频
        val frameTime = frameWidth * 1000 / standPxInSecond

        //fixme 产品或UI要给出更合理的缩放。。。
        maxScale = frameTime / minFrameTime //最大精度就是 0.25s 1帧
        minScale = frameWidth * 1000f / duration / standPxInSecond //最小精度为总长度到一帧

        scale = if (holdPxInSecond) {
            pxInSecond / standPxInSecond
        } else {
            1.0f
        }
    }

    private val frameWidth = TimeLineApp.instance.dp2px(48f)
    private val screenWidth = TimeLineApp.instance.getScreenWidth()

    /**
     * 将scale整理为适合屏幕大小的值，解决片断增删
     * 调整区域后整个时间轴显示区域过小的问题
     *
     * - 尽量保持原有的 pxInSecond 比例不变
     * -
     */
    fun fitScaleForScreen() {
//        val currentDurationPx = duration * standPxInSecond / 1000
//        if (currentDurationPx < App.instance.dp2px(48f) || currentDurationPx > screenWidth) {
        resetStandPxInSecond(true)
//        }
    }

    var maxScale = 1.0f
    var minScale = 0.5f//最小是默认的一半，占半屏


    //最小一帧对应的时间，用于直接分割帧，避免缩放的时候产生无数的图片
    var minFrameTime = 250L // frameTime / maxScale
    //最小时间切片的时间
    val minClipTime = MIN_CLIP_TIME
    val minStandPxInSecond = 1000 * TimeLineApp.instance.dp2px(8f) / minClipTime //最小切片占-帧边框的长度，标准是两倍，此时缩放到最小刚好满足


    /**
     * 含时间线数据 功能标识接口
     */
    interface TimeLineBaseView {
        var timeLineValue: TimeLineBaseValue?
        fun scaleChange()
        fun updateTime()
    }


    companion object {
        /**
         * 视频片段最小时间
         */
        const val MIN_CLIP_TIME = 100L
    }

}