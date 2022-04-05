package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.bean.TagType


/**
 *
 * 文字 激活时全部显示，非激活时限制长度
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-31
 */
class ActiveFullTextTagLineView @JvmOverloads constructor(
        context: Context, paramAttributeSet: AttributeSet? = null,
        paramInt: Int = 0
) : WideTextTagLineView(context, paramAttributeSet, paramInt),
        TimeLineBaseValue.TimeLineBaseView {

    override fun updatePathBeforeDraw(
        item: TagLineViewData,
        canvas: Canvas,
        zIndex: Int,
        isActive: Boolean
    ) {
        //UPDATE PATH
        if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            if (zIndex == 0 && isActive) {
                updateActivePath(getItemWidth(item))
            } else {
                updateNormalPath(getItemWidth(item))
            }
        } else {
            super.updatePathBeforeDraw(item, canvas, zIndex, isActive)
        }
    }

    override fun drawContentText(content: String, canvas: Canvas, rect: RectF, isActive: Boolean) {
        val paddingLeft = if (isActive) activeTextPadding else normalTextPadding
        val text = if(isActive) content else ellipsizeText(content)
        canvas.drawText(text, paddingLeft, rect.centerY() + textBaseY, textPaint)
    }


    override fun getItemWidth(item: TagLineViewData): Float {
        return if (item == activeItem && item.itemType == TagType.ITEM_TYPE_TEXT) {
            val width = textPaint.measureText(item.content)
            width + activeTextPadding * 2
        } else if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            val width = textPaint.measureText(ellipsizeText(item.content))
            width + activeTextPadding * 2
        } else {
            normalWidth
        }
    }
}