package com.sam.video.timeline.widget

import android.content.Context
import android.util.AttributeSet

/**
 * 限制最大高度的rv
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-07
 */
class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.recyclerview.widget.RecyclerView(context, attrs, defStyleAttr) {

    var maxHeight = 0
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val newHeightSpec = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightSpec
        }
        super.onMeasure(widthSpec, newHeightSpec)

    }
}
