package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.sam.video.timeline.adapter.TagAdapter
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.listener.Click
import com.sam.video.util.dp2px
import com.sam.video.util.getScreenWidth
import com.sam.video.util.removeObj

/**
 * 重叠的标签组 选择弹窗
 * 更新的时候水平位置要有偏移，小三角的位置更新
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-08
 */
class TagPopWindow(val context: Context) : PopupWindow(context) {
    private val rv = MaxHeightRecyclerView(context)
    private val padding = context.dp2px(8f).toInt()
    private val data = mutableListOf<TagLineViewData>()
    private val adapter = TagAdapter(context, data)
    private val tagItemDecoration = TagItemDecoration(context)
    private val screenWidth = context.getScreenWidth()

    init {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rv.maxHeight = context.dp2px(184f).toInt()
        rv.addItemDecoration(tagItemDecoration)

        //无法设置左右padding 会影响 FlexboxLayoutManager 排列，该库在layout时未考虑padding，真正绘制时又会考虑，
        //导致行数不统一异常留白、滑动等问题，UI要是有需求可以考虑在上一层 FrameLayout 上做样式
        rv.setPadding(0, padding, 0,
            (padding + tagItemDecoration.triangleHeight).toInt()
        )
        rv.clipToPadding = false
        rv.layoutManager = FlexboxLayoutManager(context).apply {
            flexWrap = FlexWrap.WRAP
            flexDirection = FlexDirection.ROW
        }

        rv.adapter = adapter
        adapter.onClickListener = View.OnClickListener {
            val position = rv.getChildAdapterPosition(it)
            if (position in 0 until data.size) {
                onItemClickListener?.onItemClick(it, data[position], position)
            }

        }

        val fl = FrameLayout(context) //低版本兼容。。
        fl.addView(rv)
        fl.setOnClickListener { dismiss() }
        contentView = fl

        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isOutsideTouchable = true


    }

    fun updateData(tagList: List<TagLineViewData>, selectItem: TagLineViewData?) {
        data.clear()
        data.addAll(tagList)
        selectItem?.let {
            if (data.isNotEmpty() && it != data[0]) {
                //选中置顶
                data.removeObj(it)
                data.add(0, it)
            }
        }
        adapter.selectedItem = selectItem
        adapter.notifyDataSetChanged()
    }

    var onItemClickListener: Click.OnItemViewClickListener<TagLineViewData>? = null

    /**
     * UI设定，比较复杂，指定三角形尖角的位置，整体popupwindow居中，或居中时部分超出屏幕，则靠近屏幕某边
     * @param x 三角形尖角的位置
     * @param y 底部Y的绝对坐标
     *
     * 关键：x 要换算成 popwindow位移 + popwindow内三角的偏移
     */
    fun showAtTriangleX(parent: View, x: Int, y: Int) {
        showAtLocation(parent, Gravity.BOTTOM or Gravity.START, 0, y)
        rv.post { //为了准确获取w,h
            val halfWidth = rv.width / 2

            val newX = if (x >= halfWidth && x + halfWidth <= screenWidth) {
                tagItemDecoration.triangleXOffset = 0
                // 尖角居中的情况
                x - halfWidth
            } else if (x < halfWidth) {
                // 屏幕左边, 三角形往左偏
                tagItemDecoration.triangleXOffset = x - halfWidth
                0
            } else {
                //屏幕右边
                tagItemDecoration.triangleXOffset =  x + halfWidth - screenWidth
                screenWidth - rv.width
            }
            update(newX, y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        }


    }
}