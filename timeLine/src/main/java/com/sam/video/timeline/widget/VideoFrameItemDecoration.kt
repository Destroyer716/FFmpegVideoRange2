package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sam.video.R
import com.sam.video.timeline.adapter.VideoFrameAdapter
import com.sam.video.util.dp2px


/**
 * 多个视频 帧列表的视频间隔
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-22
 */
class VideoFrameItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val itemDecorationSpace = context.dp2px(2f).toInt()
    /**圆角半径 */
    private val rect = RectF()
    private val strokeWidth = context.dp2px(2f)
    private val halfStrokeWidth = strokeWidth / 2
    private val radius = context.dp2px(4f) - strokeWidth

    private val colorStart = ContextCompat.getColor(context, R.color.colorSelectedBlur)
    private val colorMiddle = ContextCompat.getColor(context, R.color.colorSelectedBlurLight)
    private val colorEnd = ContextCompat.getColor(context, R.color.colorSelectedPink)

    private val shaderColors = intArrayOf(colorStart, colorMiddle, colorEnd)
    private val shaderPositions = floatArrayOf(0f, 0.39f, 1.0f)

    var hasBorder = true //是否有渐变描边

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = this@VideoFrameItemDecoration.strokeWidth
    }

    override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter
        var hasDecoration = false
        if (adapter is VideoFrameAdapter) {
            //不是全列表的最后一项 && 是这个视频的最后一项
            hasDecoration = position < adapter.data.size - 1 && adapter.data[position].isLastItem
        }
        if (hasDecoration) {
            outRect.set(0, 0, itemDecorationSpace, 0)
        } else {
            outRect.set(0, 0, 0, 0)
        }

    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)
        if (!hasBorder) {
            return
        }
        val adapter = parent.adapter
        if (adapter == null || adapter.itemCount == 0) {
            return
        }
        (parent as? VideoFrameRecyclerView)?.getCurrentCursorVideoRect(rect)
//        Log.d("SAM", "onDrawOver " +rect.toShortString())
        if (rect.width() == 0f) {
            return
        }
        rect.left += halfStrokeWidth
        rect.right -= halfStrokeWidth

        rect.top += halfStrokeWidth
        rect.bottom -= halfStrokeWidth

        paint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
            shaderColors, shaderPositions, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, paint)

    }
}