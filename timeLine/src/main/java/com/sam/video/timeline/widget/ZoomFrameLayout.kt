package com.sam.video.timeline.widget

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import com.sam.video.timeline.listener.VideoPlayerOperate

/**
 * 拦截最外层，统一调度缩放事件、滑动事件
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-19
 */
class ZoomFrameLayout : FrameLayout,
    DynamicAnimation.OnAnimationUpdateListener,
    ValueAnimator.AnimatorUpdateListener {

    var onScrollVelocityChangeListener:OnScrollVelocityChangeListener? = null
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    /** 滑动数据更新*/
    override fun onAnimationUpdate(
        animation: DynamicAnimation<out DynamicAnimation<*>>?,
        vlaue: Float,
        velocity: Float
    ) {
        timeLineValue.time = timeLineValue.px2time(vlaue)
        dispatchUpdateTime()
        Log.e("kzg","***************onAnimationUpdate:${velocity}  ,vlaue:$vlaue")
        timeChangeListener?.updateTimeByScroll(timeLineValue.time)
        onScrollVelocityChangeListener?.onVelocityChange(velocity)
    }


    /** 双击动画更新 */
    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (doubleTapEnable){
            scaleChange(animation.animatedValue as Float)
        }
    }

    var timeLineValue = TimeLineBaseValue()
    /** 保存滑动事件*/
    private var timeValueHolder = FloatValueHolder()
    val flingAnimation = FlingAnimation(timeValueHolder).apply {
        addUpdateListener(this@ZoomFrameLayout)
        addEndListener { _, _, _, _ -> timeChangeListener?.stopTrackingTouch(timeLineValue.time) }
    }

    /**
     * 双指缩放是否可用，默认true
     */
    var doubleFingerEnable = false
    /**
     * 双击缩放是否可用，默认true
     */
    var doubleTapEnable = false

    var scaleEnable = false

    /** 额外的事件监听 */
    var timeChangeListener: VideoPlayerOperate? = null


    private fun scaleChange(scale: Float) {
        flingAnimation.cancel()
        timeLineValue.scale = scale
        dispatchScaleChange()
    }

    fun scroll(x: Float, y: Float) {
        val offsetTime = (x * 1000 / timeLineValue.pxInSecond).toLong()
        if (offsetTime != 0L) {
            flingAnimation.cancel()
            //Log.d("kzg", "scroll $x $offsetTime ${timeLineValue.time}")
            timeLineValue.time += offsetTime
            timeValueHolder.value = timeLineValue.time.toFloat() * timeLineValue.pxInSecond / 1000
            updateTimeByScroll(timeLineValue.time)
        }
    }

    fun scrollByTime(offsetTime: Long) {
        //val offsetTime = (x * 1000 / timeLineValue.pxInSecond).toLong()
        //Log.e("kzg","****************scrollByTime:$x  ,offsetTime:${offsetTime}")
        if (offsetTime != 0L) {
            flingAnimation.cancel()
//            Log.d("Sam", "scroll $x $offsetTime ${timeLineValue.time}")
            timeLineValue.time += offsetTime
            timeValueHolder.value = timeLineValue.time.toFloat() * timeLineValue.pxInSecond / 1000
            updateTimeByScroll(timeLineValue.time)
        }
    }

    /**
     * 动画改变缩放倍数
     */
    private fun scaleChangeWithAnimation(target: Float) {
        ValueAnimator.ofFloat(timeLineValue.scale, target).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 500
            addUpdateListener(this@ZoomFrameLayout)
            start()
        }
    }
    var lastScaleEventTime = 0L

    /**
     * 双指缩放监听 - 识别手势并处理
     */
    val scaleGestureListener = object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            lastScaleEventTime = detector.eventTime
//            scaleListener?.onScaleEnd(detector) //手指未离开就end了
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (doubleTapEnable){
                scaleChange(timeLineValue.scale * detector.scaleFactor * detector.scaleFactor) //加强灵敏度
            }
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            scaleListener?.onScaleBegin(detector)
            return super.onScaleBegin(detector)
        }
    }


    /**
     * 额外缩放监听
     *  - 用于通知外部view变化
     */
    var scaleListener: ScaleListener? = null

    /**
     * 滑动、双击监听
     */
    val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            if (!scaleEnable || e?.pointerCount ?: 1 > 1) {
                return false
            }
//            Log.d("ZoomFrameLayout", "onDoubleTap() called")
            val targetScale = when (timeLineValue.scale) {
                timeLineValue.maxScale -> {
                    timeLineValue.minScale
                }
                1f -> {
                    timeLineValue.maxScale
                }
                else -> {
                    1f
                }
            }
            scaleChangeWithAnimation(targetScale)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            performClick()
            return super.onSingleTapUp(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (hasEventWithScale(e1, e2)) {
                return true
            }
            scroll(distanceX, distanceY)
            onScrollVelocityChangeListener?.onScrollZoomFl(distanceX.toInt())
            return true
        }

        //按下后有过缩放事件就不处理滑动
        private fun hasEventWithScale(
            e1: MotionEvent?,
            e2: MotionEvent?
        ): Boolean {
            if (scaleGestureDetector.isInProgress) {
                return true
            }
            if (e1 != null && (lastScaleEventTime > e1.downTime || e1.pointerCount > 1)) {
                return true
            }

            if (e2 != null && (lastScaleEventTime > e2.downTime || e2.pointerCount > 1)) {
                return true
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {


            if (hasEventWithScale(e1, e2) ) {
                return true
            }
            flingAnimation.apply {
                cancel()
                val max = timeLineValue.pxInSecond * timeLineValue.duration / 1000
                if (max > 0f && timeValueHolder.value in 0f..max) {

                    setStartVelocity(-velocityX)
                    setMinValue(0f)
                    setMaxValue(max)
                    start()
                    timeChangeListener?.startTrackingTouch()
                }
            }

            return true
        }


    }

    fun isScrollIng(): Boolean {
        return flingAnimation.isRunning
    }

    private val gestureDetector: GestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        GestureDetector(context, gestureListener)
    }


    private val scaleGestureDetector: ScaleGestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        ScaleGestureDetector(context, scaleGestureListener)
    }


    fun updateTime(time: Long) {
//        Log.d("Sam" , "ZoomFrameLayout updateTime $time ")
        timeLineValue.time = time
        dispatchUpdateTime()
    }

    fun updateTimeByScroll(time: Long) {
        updateTime(time)
        timeChangeListener?.updateTimeByScroll(timeLineValue.time)
    }

    /**
     * 分发时间线数据
     */
    fun dispatchTimeLineValue() = dispatchTimeLineEvent(false) {
        it.timeLineValue = timeLineValue
    }

    /**
     * 分发缩放数据
     */
    fun dispatchScaleChange() = dispatchTimeLineEvent {
        it.scaleChange()
    }

    /** 传递时间轴事件 */
    private fun dispatchTimeLineEvent(
        filterHidden: Boolean = true, //隐藏view不用分发事件
        event: (TimeLineBaseValue.TimeLineBaseView) -> Unit
    ) {
        for (i in 0..childCount) {
            val childAt = getChildAt(i)
            if (childAt is TimeLineBaseValue.TimeLineBaseView && (!filterHidden || childAt.visibility == View.VISIBLE)) {
                event(childAt)
            }
        }

    }

    /**
     * 分发时间更新
     */
    fun dispatchUpdateTime() = dispatchTimeLineEvent {
        it.updateTime()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return ev.pointerCount > 1 || super.onInterceptTouchEvent(ev)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.d("Sam", " $event ")
        if (!scaleEnable) {
            return super.onTouchEvent(event)
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            timeChangeListener?.startTrackingTouch()
        }else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            timeChangeListener?.stopTrackingTouch(timeLineValue.time)
            scaleListener?.touchEventEnd()
        }

        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
             gestureDetector.onTouchEvent(event)
        }

        return true
    }

    abstract class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        var isScaled = false

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            isScaled = true
            return super.onScaleBegin(detector)
        }

        fun touchEventEnd() {
            if (isScaled) {
                onScaleTouchEnd()
            }
        }

        /**
         * 有过缩放后的事件结束
         */
        abstract fun onScaleTouchEnd()

    }

    interface OnScrollVelocityChangeListener{
        //监听滑动速度，只有在手指离开的时候才会触发
        fun onVelocityChange(v:Float)
        //监听回退
        fun onScrollZoomFl(x:Int)
    }
}