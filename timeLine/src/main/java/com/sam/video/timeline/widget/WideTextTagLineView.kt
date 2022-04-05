package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.bean.TagType
import com.sam.video.util.dp
import com.sam.video.util.dp2px


/**
 * 文字限定一定的宽度；图片不变 标签-线 view
 *
 * 用于：内容都是文本，限制一定的宽度
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-31
 */
open class WideTextTagLineView @JvmOverloads constructor(
        context: Context, paramAttributeSet: AttributeSet? = null,
        paramInt: Int = 0
) : TagLineView(context, paramAttributeSet, paramInt),
        TimeLineBaseValue.TimeLineBaseView {

    protected val textPaint = TextPaint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 14f.dp
    }

    /**
     * 将传入的文本转换为要显示的文本
     */
    private val ellipsizeStringMap = HashMap<String, String>()
    /**
     * 后缀显示
     */
    private val maxTextWidth by lazy { initMaxTextWidth() }  //文本限制的最大宽度（3个汉字）
    protected val normalTextPadding = context.dp2px(6f) //文本左右Padding
    protected val activeTextPadding = context.dp2px(8f)

    protected open fun initMaxTextWidth(): Float {
        return context.dp2px(42f)
    }
    /**
     * 缩字
     */
    protected fun ellipsizeText(origin: String): String {
        ellipsizeStringMap[origin]?.let {
            return it
        }

        return TextUtils.ellipsize(origin, textPaint, maxTextWidth, TextUtils.TruncateAt.END)
            .toString().also {
            ellipsizeStringMap[origin] = it
        }
    }

    override fun drawItem(item: TagLineViewData, canvas: Canvas, zIndex: Int, isActive: Boolean) {
        updatePathBeforeDraw(item, canvas, zIndex, isActive)
        super.drawItem(item, canvas, zIndex, isActive)
    }

    /**
     * 根据item 更新path
     */
    open fun updatePathBeforeDraw(item: TagLineViewData, canvas: Canvas, zIndex: Int, isActive: Boolean) {
        if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            val text = ellipsizeText(item.content)
            val width = textPaint.measureText(text)
            //UPDATE PATH
            if (isActive && zIndex == 0) {
                updateActivePath(width + activeTextPadding * 2)
            } else {
                updateNormalPath(width + normalTextPadding * 2)
            }
        } else {
            if (isActive && zIndex == 0) {
                updateActivePath()
            } else {
                updateNormalPath()
            }
        }
    }

    override fun drawContent(
        item: TagLineViewData,
        canvas: Canvas,
        zIndex: Int,
        isActive: Boolean
    ) {
        val rect = if (isActive && zIndex == 0) activeBitmapRectF else bitmapRectF
        if (item.itemType == TagType.ITEM_TYPE_IMG) {
            loadImg(item.content)?.let {
                drawBitmapKeepRatio(canvas, it, rect)
            }
        } else if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            drawContentText(item.content,canvas, rect, isActive && zIndex == 0)
        }
    }


    /**
     * 绘制文本
     */
    open fun drawContentText(
        content: String,
        canvas: Canvas,
        rect: RectF,
        isActive: Boolean
    ) {
        val text = ellipsizeText(content)
        val paddingLeft = if (isActive) activeTextPadding else normalTextPadding
        canvas.drawText(text, paddingLeft, rect.centerY() + textBaseY, textPaint)
    }

    override fun getItemWidth(item: TagLineViewData): Float {
        val width = textPaint.measureText(ellipsizeText(item.content))

        return if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            if (item == activeItem) {
                width + activeTextPadding * 2
            } else {
                width + normalTextPadding * 2
            }
        } else {
            super.getItemWidth(item)
        }

    }
}