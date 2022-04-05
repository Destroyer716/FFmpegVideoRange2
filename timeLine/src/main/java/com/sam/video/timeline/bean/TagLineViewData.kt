package com.sam.video.timeline.bean

import androidx.annotation.ColorInt
import androidx.annotation.IntDef

/**
 * 标签线 需要的数据
 *
 * 具体的绘制内容由文字标签给出
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-03
 */
class TagLineViewData(
    @ColorInt var color: Int,
    var startTime: Long,
    var endTime: Long,
    @TagType val itemType: Int,
    var content: String, //内容。img->path, text->text
    var originData: Any? = null, // 原始数据
    var tagDrawStartTime: Long = 0L, //标签开始绘制的时间，用于左边缘停留
    var index: Int = 0, //所属层级
    var groupHead: TagLineViewData?= null //所属分组的第一个，null时未分组，在屏幕外
)


@IntDef(TagType.ITEM_TYPE_TEXT, TagType.ITEM_TYPE_IMG)
annotation class TagType {
    companion object {
        const val ITEM_TYPE_TEXT = 1
        const val ITEM_TYPE_IMG = 2
    }
}