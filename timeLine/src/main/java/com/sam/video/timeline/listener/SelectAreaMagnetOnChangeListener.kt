package com.sam.video.timeline.listener

import android.content.Context
import com.sam.video.timeline.widget.SelectAreaView
import com.sam.video.timeline.widget.SelectAreaView.OnChangeListener.Companion.MODE_NONE
import com.sam.video.timeline.widget.TimeLineBaseValue
import com.sam.video.util.vibratorOneShot
import java.util.*
import kotlin.math.abs


/**
 * 区域选择变化监听
 * 封装磁吸功能通用代码
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-20
 */
abstract class SelectAreaMagnetOnChangeListener(
    private val context: Context
): SelectAreaView.OnChangeListener {
    protected val timeSet = TreeSet<Long>() //磁吸的时间点数据

    private var isJumpOffsetFilter = false //吸附后左右要超出一定的幅度才开始能继续拖动！！！
    private var jumpOffsetPx = 0f

    override var mode: Int = MODE_NONE

    /**
     * 最长距离，>0时限制
     */
    var maxDuration = 0
    /**
     * 可用的时间区间-开始
     */
    var startTimeEdge = 0L
    /**
     * 可用的时间区间-结束
     */
    var endTimeEdge = 0L

    /**
     * 更新磁吸数据
     * @param item 选中项
     */
    fun updateTimeSetData() {
        timeSet.clear()
    }

    /**
     * 过滤吸附范围
     * 如果被磁吸吃掉的事件不要再往上传递
     */
    fun filterOnChange(startOffset: Long, endOffset: Long):Boolean {
        if (isJumpOffsetFilter) {
            //过滤
            jumpOffsetPx += startOffset + endOffset
            if (abs(jumpOffsetPx) < timeJumpOffset * 2) { //这个系数也是自己瞎填的,系数越大就越难从吸附中挣脱出来
                //左右范围内不响应
                return true
            } else {
                jumpOffsetPx = 0f
                isJumpOffsetFilter = false
            }

        }
        return false
    }

    /**
     * 检查时间吸附（跳跃到合适的时间点）
     * @param left 左移
     * @param triggerJumpEvent 触发吸附效果，子类可以 false，自己再返回值决定是否触发
     */
    fun checkTimeJump(time: Long, left: Boolean, triggerJumpEvent: Boolean = true): Long {

        var newTime = -1L
        for (t in timeSet) {
            if (left) {
                if (t > time) { //往左，吸附的时间只会是比它还小的时间
                    break
                }

                if (time - t <= timeJumpOffset) {
                    newTime = t
                }
            } else {
                //往右移，吸附的时间只会是比它还大的时间
                if (t > time) {
                    if (t - time > timeJumpOffset) {
                        break
                    } else {
                        newTime = t
                    }
                }
            }
        }

        //当前光标吸附
        if (newTime == -1L) {
            timeLineValue?.time?.let { t ->

                if (left) {
                    if (t > time) { //往左，吸附的时间只会是比它还小的时间
                        return@let
                    }
                    if (time - t <= timeJumpOffset) {
                        newTime = t
                    }
                } else {
                    //往右移，吸附的时间只会是比它还大的时间
                    if (t > time) {
                        if (t - time > timeJumpOffset) {
                            return@let
                        } else {
                            newTime = t
                        }
                    }
                }
            }
        }

        if (newTime >= 0 && newTime != time) {
//                    Log.d("Sam", "----- $newTime : $time ")
            if (newTime > endTimeEdge) {
                return endTimeEdge
            }else if (newTime < startTimeEdge) {
                return startTimeEdge
            }

            triggerJumpEvent()
            return newTime
        }

        return time
    }

    /**
     * 触发吸附事件
     */
    fun triggerJumpEvent() {
        context.vibratorOneShot()
        jumpOffsetPx = 0f
        isJumpOffsetFilter = true
    }

    override fun onTouchUp() {
        isJumpOffsetFilter = false
        jumpOffsetPx = 0f
        mode = MODE_NONE
    }

    abstract val timeLineValue: TimeLineBaseValue?

    abstract val timeJumpOffset: Long
}