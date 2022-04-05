package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.*
import androidx.recyclerview.widget.RecyclerView
import com.sam.video.util.dp2px


/**
 * 标签弹窗的item间隔
 *
 * 间隔的数据 为间隔前的item position
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-22
 */
class TagItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val colorBg = Color.parseColor("#202020")

    private val triangleWidth = context.dp2px(7f) //小三角规格
    val triangleHeight = context.dp2px(9f)
    private val cornerRadius = context.dp2px(2f)

    var triangleXOffset = 0
    private var trianglePath = Path()
    private var coverPath = Path()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val rectF = RectF()
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        paint.color = colorBg
        rectF.set(0f, 0f, parent.width.toFloat(), parent.height - triangleHeight)
        val startX = parent.width / 2 - triangleWidth + triangleXOffset

        trianglePath.reset()
        trianglePath.moveTo(startX, parent.height - triangleHeight)
        trianglePath.rLineTo(triangleWidth, triangleHeight)
        trianglePath.rLineTo(triangleWidth, -triangleHeight)
        trianglePath.close()

        c.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        c.drawPath(trianglePath, paint)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        coverPath.reset()
        coverPath.addRect(0f, parent.height - triangleHeight, parent.width.toFloat(), parent.height.toFloat(), Path.Direction.CCW)
        coverPath.op(trianglePath, Path.Op.DIFFERENCE)
        paint.color = Color.BLACK
        c.drawPath(coverPath, paint)
    }
}