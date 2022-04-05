package com.sam.video.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import com.sam.video.timeline.widget.SelectAreaView
import com.sam.video.timeline.widget.SelectAreaView.OnChangeListener.Companion.MODE_WHOLE
import com.sam.video.timeline.widget.TimeLineBaseValue

/**
 * 时间选择事件处理
 * [startEndBothMoveEnable]：控制整个区域移动
 * 复用：
 * 区域选择 [com.sam.video.timeline.widget.SelectAreaView]；
 * tag 移动 [com.sam.video.timeline.widget.TagLineView]
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-09-12
 */
class SelectAreaEventHandle(context: Context) {

    private val horizontalScrollSpeedMaxScaleRate = 9f // 横向自动滚动速度区间，因为是0.1-1.0，所以区间为9倍
    private val horizontalScrollSpeedMinScale = 1f
    private var horizontalScrollSpeedScale = horizontalScrollSpeedMinScale // 当前速度倍数
    private val horizontalScrollMinSpeed = 0.1f // 最小横向自动滚动速度，1毫秒0.1dp
    private val autoHorizontalScrollAreaWidth = context.getScreenWidth() / 8f

    var onChangeListener: SelectAreaView.OnChangeListener? = null
    private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
    private val LONG_PRESS_EVENT = 1
    private val width = context.getScreenWidth()
    /**
     * 整个区域同时移动（即起始和结束位置同时移动）
     */
    var startEndBothMoveEnable = false

    /**
     * 整个一起移动的模式
     */
    var wholeMoveMode = false
        set(value) {
            field = value
            wholeMoveModeChange?.onWholeMoveModeChange()
            if (value) {
                onChangeListener?.mode = MODE_WHOLE
                onChangeListener?.onTouchDown()
            }
        }

    /**
     * 吸附时的偏移距离
     * 产品没给，自己瞎设置4dp
     */
    val timeJumpOffsetPx = context.dp2px(4f)
    /**
     * 吸附偏移时间范围
     */
    var timeJumpOffset = 0L
    var timeLineValue: TimeLineBaseValue? = null
        set(value) {
            field = value
            timeJumpOffset = value?.px2time(timeJumpOffsetPx) ?: 0
        }

    private val longPressHandler = Handler {
        if (startEndBothMoveEnable) {
            wholeMoveMode = true
            context.vibratorOneShot()
        }
        true
    }

    fun removeLongPressEvent() {
        longPressHandler.removeMessages(LONG_PRESS_EVENT)
    }
    fun sendLongPressAtTime(now: Long) {
        longPressHandler.sendEmptyMessageAtTime(
            LONG_PRESS_EVENT,
            now + LONG_PRESS_TIMEOUT
        )
    }

    /**
     * 手指滑动后所处的位置
     */
    private fun changeStart(distanceX: Float, event: MotionEvent):Boolean {
        val timeLineValue = timeLineValue?: return false
        // < 0 右， >0 左
        val startOffset = -timeLineValue.px2time(distanceX)
        val consume = onChangeListener?.onChange(startOffset, 0, true) ?: false //是可移

        if (consume) {
            startCountDown(startOffset, event.x)
        } else {
            autoHorizontalScrollAnimator?.cancel()
        }

        return consume
    }

    private fun startCountDown(offsetTime: Long, eventX: Float) {
        horizontalScrollSpeedScale = when {
            eventX < autoHorizontalScrollAreaWidth -> {
                horizontalScrollSpeedMinScale + (autoHorizontalScrollAreaWidth - eventX) / autoHorizontalScrollAreaWidth * horizontalScrollSpeedMaxScaleRate
            }
            eventX > width - autoHorizontalScrollAreaWidth -> {
                horizontalScrollSpeedMinScale + (eventX - (width - autoHorizontalScrollAreaWidth)) / autoHorizontalScrollAreaWidth * horizontalScrollSpeedMaxScaleRate
            }
            else -> {
                horizontalScrollSpeedMinScale
            }
        }
        if (offsetTime <= 0 && eventX <= autoHorizontalScrollAreaWidth) {
            startCountDown(true)
        } else if (offsetTime >= 0 && eventX >= width - autoHorizontalScrollAreaWidth) {
            startCountDown(false)
        } else if (eventX > autoHorizontalScrollAreaWidth && eventX < width - autoHorizontalScrollAreaWidth) {
            autoHorizontalScrollAnimator?.cancel()
        }
    }

    private fun changeEnd(distanceX: Float, event: MotionEvent):Boolean {
        val timeLineValue = timeLineValue?: return false
        // < 0 右， >0 左
        val endOffset = -timeLineValue.px2time(distanceX)
        val consume = onChangeListener?.onChange(0, endOffset, true) ?: false
        if (consume) {
            startCountDown(endOffset, event.x)
        } else {
            autoHorizontalScrollAnimator?.cancel()
        }

        return consume

    }


    /** 操作的是开始的标记 */
    var touchStartCursor = false
    /** 操作的是结束的标记*/
    var touchEndCursor = false

    /**
     * 消耗事件
     */
    val consumeEvent
        get() = touchStartCursor || touchEndCursor || wholeMoveMode

    var autoHorizontalScrollAnimator: ValueAnimator? = null
    private var moveLeft = false // true 左，false 右
    private fun startCountDown(moveLeft: Boolean) {
        this.moveLeft = moveLeft
        if (autoHorizontalScrollAnimator != null) {
            return
        }
        val valueAnimator = ValueAnimator.ofInt(1, 10000)
        valueAnimator.duration = 10000
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.repeatMode = ValueAnimator.RESTART
        valueAnimator.repeatCount = ValueAnimator.INFINITE

        valueAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {

            private var lastAnimationValue = 0

            override fun onAnimationUpdate(animation: ValueAnimator?) {
                val timeLineValue = timeLineValue ?: return
                val value = animation?.animatedValue as Int
                val time = value - lastAnimationValue
                lastAnimationValue = if (value == 10000) 0 else value
                var offsetPx = (time * horizontalScrollMinSpeed * horizontalScrollSpeedScale).dp
                if (moveLeft) {
                    offsetPx = -offsetPx
                }
                val offsetTime = timeLineValue.px2time(offsetPx)
                val result = when {
                    wholeMoveMode -> onChangeListener?.onMove(offsetTime, false)
                    touchStartCursor -> onChangeListener?.onChange(offsetTime, 0, false)
                    touchEndCursor -> onChangeListener?.onChange(0, offsetTime, false)
                    else -> false
                }
                if (result != true) {
                    animation.cancel()
                }
            }
        })
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (autoHorizontalScrollAnimator == valueAnimator) {
                    autoHorizontalScrollAnimator = null
                }
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })

        autoHorizontalScrollAnimator = valueAnimator
        autoHorizontalScrollAnimator?.start()
    }

    fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return if (wholeMoveMode) {
            move(distanceX, e2)
        } else {
            (touchStartCursor && changeStart(distanceX, e2)) || (touchEndCursor && changeEnd(
                distanceX,
                e2
            )) //事件传递
        }
    }


    /**
     * 区域移动
     */
    private fun move(distanceX: Float, event: MotionEvent):Boolean {
        val timeLineValue = timeLineValue?: return false
        // < 0 右， >0 左
        val offsetTime = -timeLineValue.px2time(distanceX)
        val consume = onChangeListener?.onMove(offsetTime,  true) ?: false //是可移

        if (consume) {
            startCountDown(offsetTime, event.x)
        } else {
            autoHorizontalScrollAnimator?.cancel()
        }

        return consume
    }

    fun onTouchEvent(event: MotionEvent) {
        if ((event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL)
        ) {
            longPressHandler.removeMessages(LONG_PRESS_EVENT)
            if (wholeMoveMode || touchStartCursor || touchEndCursor) {
                onChangeListener?.onTouchUp()
                autoHorizontalScrollAnimator?.cancel()
                wholeMoveMode = false
            }
        }
    }

    var wholeMoveModeChange: WholeMoveModeChange? = null
    interface WholeMoveModeChange {
        fun onWholeMoveModeChange()
    }


}