package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.bean.TagType
import com.sam.video.util.getScreenWidth


/**
 *
 * 文字 激活时最多半屏，非激活时一字
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-31
 */
class ActiveWideTextTagLineView @JvmOverloads constructor(
        context: Context, paramAttributeSet: AttributeSet? = null,
        paramInt: Int = 0
) : WideTextTagLineView(context, paramAttributeSet, paramInt),
        TimeLineBaseValue.TimeLineBaseView {

    override fun initMaxTextWidth(): Float {
        return (context.getScreenWidth() / 2).toFloat()
    }
    override fun updatePathBeforeDraw(
        item: TagLineViewData,
        canvas: Canvas,
        zIndex: Int,
        isActive: Boolean
    ) {
        //UPDATE PATH
        if (isActive && zIndex == 0) {
            if (item.itemType == TagType.ITEM_TYPE_TEXT) {
                val text = ellipsizeText(item.content)
                val width = textPaint.measureText(text)
                updateActivePath(width + activeTextPadding * 2)
            } else {
                updateActivePath()
            }
        }
        //这里的场景，未激活的都是同样的尺寸
    }

    override fun drawContentText(content: String, canvas: Canvas, rect: RectF, isActive: Boolean) {
        if (isActive) {
            super.drawContentText(content, canvas, rect, isActive)
        } else {
            paint.color = Color.WHITE
            canvas.drawText(content, 0, 1, rect.centerX(), rect.centerY() + textBaseY, paint)

        }

    }


    override fun getItemWidth(item: TagLineViewData): Float {
        return if (item == activeItem && item.itemType == TagType.ITEM_TYPE_TEXT) {
            val width = textPaint.measureText(ellipsizeText(item.content))
            width + activeTextPadding * 2
        } else {
            normalWidth
        }
    }
}