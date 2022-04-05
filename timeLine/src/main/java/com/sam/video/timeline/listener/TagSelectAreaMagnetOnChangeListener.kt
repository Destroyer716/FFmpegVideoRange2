package com.sam.video.timeline.listener

import android.content.Context
import android.util.Log
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.widget.SelectAreaView.OnChangeListener.Companion.MODE_END
import com.sam.video.timeline.widget.SelectAreaView.OnChangeListener.Companion.MODE_START
import com.sam.video.timeline.widget.SelectAreaView.OnChangeListener.Companion.MODE_WHOLE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * 区域选择变化监听 - 标签
 * 封装磁吸功能通用代码
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-20
 */
abstract class TagSelectAreaMagnetOnChangeListener(
    private val tagView: BaseTagView, context: Context
) : SelectAreaMagnetOnChangeListener(context) {

    /**
     * 获取按下时应该seek的时间
     * null时，无需seek
     */
    fun getDownSeekTime(): Long? {
        val item = tagView.activeItem ?: return null
        return when (mode) {
            MODE_START, MODE_WHOLE -> item.startTime
            MODE_END -> item.endTime
            else -> null
        }
    }
    /**
     * 更新磁吸数据
     * @param item 选中项
     */
    open fun updateTimeSetData(item: TagLineViewData) {
        updateTimeSetData()
        for (datum in tagView.data) {
            if (datum != item) {
                timeSet.add(datum.startTime)
                timeSet.add(datum.endTime)
            }
        }
    }

    override fun onChange(startOffset: Long, endOffset: Long, fromUser: Boolean): Boolean {
        if (filterOnChange(startOffset, endOffset)) {
            return true
        }

        val tagLineViewData = tagView.activeItem ?: return false
        val timeLineValue = timeLineValue ?: return false
        if (startOffset != 0L) {
            val oldTime = tagLineViewData.startTime

            val minStartTime = if (maxDuration > 0) {
                max(startTimeEdge, tagLineViewData.endTime - maxDuration)
            } else {
                startTimeEdge
            }

            tagLineViewData.startTime =
                checkTimeJump(tagLineViewData.startTime + startOffset, startOffset < 0)


            if (tagLineViewData.startTime < minStartTime) {
                tagLineViewData.startTime = minStartTime
            }

            if (tagLineViewData.startTime > tagLineViewData.endTime - timeLineValue.minClipTime) {
                tagLineViewData.startTime = tagLineViewData.endTime - timeLineValue.minClipTime
            }
            Log.d("Sam", "onChange : startTime $oldTime ${tagLineViewData.startTime}")
            val realOffset = tagLineViewData.startTime - oldTime
            afterSelectAreaChange(realOffset, fromUser)
            return realOffset != 0L
        } else if (endOffset != 0L) {
            //   - 结束位置移动时，范围的起始位置也不变，结束位置会变。
            val oldTime = tagLineViewData.endTime
            tagLineViewData.endTime =
                checkTimeJump(tagLineViewData.endTime + endOffset, endOffset < 0)

            val maxEndTime = if (maxDuration > 0) {
                min(endTimeEdge, tagLineViewData.startTime + maxDuration)
            } else {
                endTimeEdge
            }
            if (tagLineViewData.endTime > maxEndTime) {
                tagLineViewData.endTime = maxEndTime
            }

            if (tagLineViewData.endTime < tagLineViewData.startTime + timeLineValue.minClipTime) {
                tagLineViewData.endTime = tagLineViewData.startTime + timeLineValue.minClipTime
            }
//            Log.d("Sam", "onChange : startTime $oldTime ${tagLineViewData.endTime} ")
            val realOffset = tagLineViewData.endTime - oldTime
            afterSelectAreaChange(realOffset, fromUser)
            return realOffset != 0L
        }

        return false
    }


    override fun onMove(offset: Long, fromUser: Boolean): Boolean {
        if (filterOnChange(offset, 0) || offset == 0L) {
            return true
        }

        val tagLineViewData = tagView.activeItem ?: return false
        val timeLineValue = timeLineValue ?: return false

        //开始位置check
        var realOffset = offset
        val oldStartTime = tagLineViewData.startTime
        tagLineViewData.startTime += offset

        val oldEndTime = tagLineViewData.endTime
        tagLineViewData.endTime += offset

        //吸附
        val left = offset < 0
        val jumpStartOffset = checkTimeJump(tagLineViewData.startTime, left, false) - tagLineViewData.startTime
        val jumpEndOffset = checkTimeJump(tagLineViewData.endTime, left, false) - tagLineViewData.endTime
        //取较小的偏移吸附点
        val minJumpOffset = when {
            jumpStartOffset == 0L -> jumpEndOffset
            jumpEndOffset == 0L -> jumpStartOffset
            abs(jumpStartOffset) >= abs(jumpEndOffset) -> jumpEndOffset
            else -> jumpStartOffset
        }
        //判断这个吸附点是否可用
        if (minJumpOffset != 0L && tagLineViewData.startTime + minJumpOffset >= startTimeEdge && tagLineViewData.endTime + minJumpOffset <= endTimeEdge) {
            tagLineViewData.startTime += minJumpOffset
            tagLineViewData.endTime += minJumpOffset
            realOffset = minJumpOffset
            triggerJumpEvent()
        }

        if(tagLineViewData.startTime < startTimeEdge){
            tagLineViewData.startTime = startTimeEdge
            realOffset = -oldStartTime
            tagLineViewData.endTime = oldEndTime + realOffset
        }

        //结束位置check
        if (tagLineViewData.endTime > endTimeEdge) {
            tagLineViewData.endTime = timeLineValue.duration
            realOffset = tagLineViewData.endTime - oldEndTime
            tagLineViewData.startTime = oldStartTime + realOffset
        }

        val change = realOffset != 0L

        if (change) {
            afterSelectAreaChange(realOffset, fromUser)
        }
//        Log.d("Sam", "onMove : $oldStartTime $oldEndTime - ${tagLineViewData.startTime} ${tagLineViewData.endTime} $realOffset ")
        return change

    }

    abstract fun afterSelectAreaChange(realOffset: Long, fromUser: Boolean)

    interface BaseTagView {
        val data: MutableList<TagLineViewData>

        var activeItem: TagLineViewData?
    }

}