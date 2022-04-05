package com.sam.video.timeline.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.sam.video.R
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.bean.TagType
import com.sam.video.timeline.listener.TagSelectAreaMagnetOnChangeListener
import com.sam.video.util.SelectAreaEventHandle
import com.sam.video.util.dp2px
import com.sam.video.util.getScreenWidth
import com.sam.video.util.removeObj
import kotlin.math.abs


/**
 * 标签-线 view 标签显示文字或图片；线显示时间轴长度
 *
 * 多个标签同时放一个view里，方便处理绘制的层级关系
 * 场景：
 * 1. 贴纸
 * 2. 文字
 *
 * 设计图拆解：
 * 整个控件至少高 66dp
 * 同一组的标签，每一层加一个黑色各种透明度的 遮罩。。。。（20，40，60，90）
 * 分组规则，两个标签开始位置<4dp 就分为一组
 *
 *
 * 使用：
 * 数据更改后，要调用 dataChange刷新view
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-31
 */
open class TagLineView @JvmOverloads constructor(
        context: Context, paramAttributeSet: AttributeSet? = null,
        paramInt: Int = 0
) : View(context, paramAttributeSet, paramInt),
        TimeLineBaseValue.TimeLineBaseView , TagSelectAreaMagnetOnChangeListener.BaseTagView {

    /**
     * 如果外面直接对data进行批量增删，
     * 必须在之后调用 dataChange
     *
     * 如果增删数量少，可直接调用 addImgTag 等方法
     *
     */
    override val data: MutableList<TagLineViewData> = mutableListOf()
    /**
     * 已排序的数据
     */
    private val sortData: MutableList<TagLineViewData> = mutableListOf()
    /**
     * 每个分组的第一个位置
     */
    private val groupData: MutableList<TagLineViewData> = mutableListOf()

    protected val normalWidth = context.dp2px(24f) //tag宽度
    private val activeWidth = context.dp2px(28f) //tag宽度-选中时
    private val normalTagHeight = context.dp2px(39f)
    private val activeTagHeight = context.dp2px(43f)
    private val normalLineHeight = context.dp2px(1f) //线高
    private val activeLineHeight = context.dp2px(2f) //线高-选中项
    private val normalTagMarginBottom = context.dp2px(3f) //TAG尖角离底部的距离
    private val activeTagMarginBottom = context.dp2px(7f)

    private val triangleWidth = context.dp2px(7f) //小三角规格
    private val triangleHeight = context.dp2px(9f)
    private val cornerDiameter = context.dp2px(4f) //圆角的圆直径

    private val cornerLineRadius = context.dp2px(0.5f) //线圆角大小
    val cursorX = context.getScreenWidth() / 2
    private val groupDistance = normalWidth / 2  //组合的边界
    private val layerHeight = context.dp2px(6f)//隔一个层级的高度差
    private val activeOffsetHeight = context.dp2px(9f)//同一个层级，激活比没激活的高

    private val shadowBlur = context.dp2px(3f)
    private val shadowColor = ContextCompat.getColor(context, R.color.black_15)

    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.style = Paint.Style.FILL
        it.textAlign = Paint.Align.CENTER
        it.textSize = context.dp2px(14f)

    }

    val textBaseY = abs(paint.ascent() + paint.descent()) / 2 //字体的基线
    private val normalPath = Path()
    private val activePath = Path()
    private val activeLineRectF = RectF()
    val bitmapRectF = RectF()
    val activeBitmapRectF = RectF()

    private val imgColors: IntArray
    private val textColors: IntArray

    private val imgSize = context.dp2px(24f).toInt() //注意与弹窗处的一致可以减少bitmap内存占用
    private val imgPadding = context.dp2px(2f)
    private val imgCache = HashMap<String, Bitmap>()

    val eventHandle = SelectAreaEventHandle(context)


    init {
        val imgTypedArray = resources.obtainTypedArray(R.array.video_tag_img_colors)
        imgColors = IntArray(imgTypedArray.length())
        for (i in 0 until imgTypedArray.length()) {
            imgColors[i] = imgTypedArray.getColor(i, Color.WHITE)
        }
        imgTypedArray.recycle()

        val textTypedArray = resources.obtainTypedArray(R.array.video_tag_text_colors)
        textColors = IntArray(textTypedArray.length())
        for (i in 0 until textTypedArray.length()) {
            textColors[i] = textTypedArray.getColor(i, Color.WHITE)
        }
        textTypedArray.recycle()

        setLayerType(LAYER_TYPE_SOFTWARE, paint)

    }

    //从下往上不同层级的叠加 COLOR
    private val colorLayer = arrayOf(
            ContextCompat.getColor(context, R.color.black20),
            ContextCompat.getColor(context, R.color.black40),
            ContextCompat.getColor(context, R.color.black60),
            ContextCompat.getColor(context, R.color.black80)
    )

    override var timeLineValue: TimeLineBaseValue? = null
        set(value) {
            field = value
            eventHandle.timeLineValue = value
            invalidate()
        }

    override fun scaleChange() {
        eventHandle.timeJumpOffset = timeLineValue?.px2time(eventHandle.timeJumpOffsetPx) ?: 0
        dataChange()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateNormalPath()
        updateActivePath()
        updateGroup()
    }

    override fun updateTime() {
        updateGroup()
    }

    /** 数据更新时 刷新分组 */
    fun dataChange() {
        sortData.clear()
        sortData.addAll(data)
        sortData.sortBy {
            it.startTime
        }

        updateGroup()
    }

    /** 左边缘时间 */
    private val leftEdgeTime
        get() = timeLineValue?.let {
            it.time - it.px2time(cursorX.toFloat())
        } ?: 0L

    /** 右边缘时间 */
    private val rightEdgeTime
        get() =  timeLineValue?.let {
            it.time + it.px2time(width - cursorX.toFloat())
        } ?: 0L



    /**
     * 更新分组
     */
    private fun updateGroup() {
        groupData.clear()

        val timeLineValue = timeLineValue ?: return

        val leftEdgeTime = leftEdgeTime
        val rightEdgeTime = rightEdgeTime
        val groupMinTimeOffset = timeLineValue.px2time(groupDistance) //分组间最小的时间差
        var currentBaseItemTime: TagLineViewData? = null //当前的基准时间(分组第一个）

        for (item in sortData) {
            if (item.endTime < leftEdgeTime || item.startTime > rightEdgeTime) {
                item.groupHead = null //未分组
                continue //屏幕外的不处理
            }

            item.tagDrawStartTime =
                if (leftEdgeTime > 0 && item.startTime < leftEdgeTime && item.endTime > leftEdgeTime) {
                    leftEdgeTime
                } else {
                    item.startTime
                }

            if (currentBaseItemTime == null || (item.tagDrawStartTime - currentBaseItemTime.tagDrawStartTime) > groupMinTimeOffset) {
                currentBaseItemTime = item
                groupData.add(item)
            }
            item.groupHead = currentBaseItemTime
        }

        data.filter { it.groupHead != null }
            .groupBy { it.groupHead }
            .forEach {
                if (it.key != null) {
                    val list = it.value.reversed()
                    for ((index, item) in list.withIndex()) {
                        item.index = index
                    }
                }
            }

        invalidate()
    }

    /**
     * 添加图片标签
     */
    fun addImgTag(
        path: String, startTime: Long, endTime: Long,
        color: Int = getRandomColorForImg()
    ): TagLineViewData {
        return TagLineViewData(color, startTime, endTime, TagType.ITEM_TYPE_IMG, path).also {
            addTag(it)
        }
    }

    /**
     * 添加文字标签
     */
    fun addTextTag(
        text: String,
        startTime: Long,
        endTime: Long,
        color: Int = getRandomColorForText()
    ): TagLineViewData {
        return TagLineViewData(color, startTime, endTime, TagType.ITEM_TYPE_TEXT, text).also {
            addTag(it)
        }
    }
    /**
     * 获取文本的随机色
     */
    fun getRandomColorForImg(): Int {
        return imgColors[(Math.random() * imgColors.size).toInt()]
    }

    /**
     * 获取文本的随机色
     */
    fun getRandomColorForText(): Int {
        return textColors[(Math.random() * textColors.size).toInt()]
    }

    fun addTag(item: TagLineViewData) {
        clipTime(item)
        data.add(item)
        dataChange()
    }

    /**
     * 在时间轴范围内修剪时间范围
     */
    fun clipTime(item: TagLineViewData) {
        timeLineValue?.let {
            if (item.startTime < 0) {
                item.startTime = 0
            }
            if (item.endTime > it.duration) {
                item.endTime = it.duration
            }
        }

    }

    fun removeTag(item: TagLineViewData) {
        data.removeObj(item)
        dataChange()
    }

    /**
     * 标签移到顶部
     * 最后面的最后绘制，即为顶部
     */
    fun bringTagToTop(item: TagLineViewData) {
        if (data.indexOf(item) == data.size - 1) {
            return
        }
        data.removeObj(item)
        data.add(item)
        dataChange()
    }

    override var activeItem: TagLineViewData? = null
        set(value) {
            field = value
            if (value != null) {
                bringTagToTop(value)
            }
            invalidate()
        }

    /**
     * 加载图片
     */
    protected fun loadImg(path: String): Bitmap? {
        val bmp = imgCache[path]
        if (bmp == null) {
            Glide.with(this).asBitmap().load(path).into(
                    object : SimpleTarget<Bitmap>(imgSize, imgSize) {
                        override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                        ) {
                            imgCache[path] = resource
                            invalidate()
                        }
                    }
            )
        }
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (item in data) {
            if (item.groupHead == null) {
                continue
            }
            val isActiveGroup = activeItem != null && activeItem?.groupHead == item.groupHead
            drawItem(item, canvas, item.index, isActiveGroup)
        }
    }

    /**
     * 绘制item : path + content
     * @param zIndex 从前到后，0是最上层，4是最下层
     * @param isActive 这组是否是激活的状态
     */
    open fun drawItem(
            item: TagLineViewData,
            canvas: Canvas,
            zIndex: Int,
            isActive: Boolean = false
    ) {
        val timeLineValue = timeLineValue ?: return

        val startX = timeLineValue.time2X(item.tagDrawStartTime, cursorX)
        if (zIndex <= colorLayer.size) { //分组超过5个的就不显示了
            canvas.save()
            paint.color = item.color
            var dy = -layerHeight * zIndex
            if (isActive && zIndex != 0) { //激活组的非当前项也都上移了
                dy -= activeOffsetHeight
            }
            canvas.translate(startX, dy)
            val drawPath = if (isActive && zIndex == 0) {
                activePath
            } else {
                normalPath
            }
            paint.setShadowLayer(shadowBlur, 0f, 0f, shadowColor)
            canvas.drawPath(drawPath, paint)
            paint.clearShadowLayer()

            if (zIndex > 0) {
                paint.color = colorLayer[zIndex - 1]
                canvas.drawPath(drawPath, paint)
            }
            if (!TextUtils.isEmpty(item.content)) {
                drawContent(item, canvas, zIndex, isActive)
            }

            canvas.restore()

            paint.color = item.color
            if (isActive && zIndex == 0) {
                activeLineRectF.set(
                        startX,
                        height - activeLineHeight,
                        startX + timeLineValue.time2px(item.endTime - item.tagDrawStartTime),
                        height.toFloat()
                )
                canvas.drawRoundRect(
                        activeLineRectF,
                        cornerLineRadius,
                        cornerLineRadius,
                        paint
                )
            } else {
                canvas.drawRect(
                        startX,
                        height - normalLineHeight,
                        startX + timeLineValue.time2px(item.endTime - item.tagDrawStartTime),
                        height.toFloat(),
                        paint
                )
            }


        }


    }

    /**
     * 适应图片比例的 rectF
     */
    private val rectFFitImg = RectF()

    /**
     * 保持比例的绘制bmp
     */
    protected fun drawBitmapKeepRatio(canvas: Canvas, bitmap: Bitmap, rectF: RectF) {
        val rationW = bitmap.width / rectF.width()
        val rationH = bitmap.height / rectF.height()
        if (rationW == rationH) {
            canvas.drawBitmap(bitmap, null, rectF, null)
        } else {
            rectFFitImg.set(rectF)
            if (rationW > rationH) {
                val offset = (rectFFitImg.height() - bitmap.height / rationW) / 2
                rectFFitImg.top += offset
                rectFFitImg.bottom -= offset
            } else {
                val offset = (rectFFitImg.width() - bitmap.width / rationH) / 2
                rectFFitImg.left += offset
                rectFFitImg.right -= offset
            }
            canvas.drawBitmap(bitmap, null, rectFFitImg, null)
        }

    }

    /**
     * 绘制内容
     */
    open fun drawContent(
            item: TagLineViewData,
            canvas: Canvas,
            zIndex: Int,
            isActive: Boolean = false
    ) {
        val rect = if (isActive && zIndex == 0) activeBitmapRectF else bitmapRectF
        if (item.itemType == TagType.ITEM_TYPE_IMG) {
            loadImg(item.content)?.let {
                drawBitmapKeepRatio(canvas, it, rect)
            }

        } else if (item.itemType == TagType.ITEM_TYPE_TEXT) {
            paint.color = Color.WHITE
            canvas.drawText(item.content, 0, 1, rect.centerX(), rect.centerY() + textBaseY, paint)
        }

    }

    private val rectCorner = RectF()
    /** 更新路径 -> 以path 的左下为原点 */
    fun updateNormalPath(tagWidth: Float = normalWidth) {
        normalPath.reset()
        normalPath.moveTo(0f, height - normalTagMarginBottom)

        rectCorner.set(
                0f,
                height - normalTagMarginBottom - normalTagHeight,
                cornerDiameter,
                height - normalTagMarginBottom - normalTagHeight + cornerDiameter
        )
        normalPath.arcTo(rectCorner, 180f, 90f)

        rectCorner.offset(tagWidth - cornerDiameter, 0f)
        normalPath.arcTo(rectCorner, 270f, 90f)

        rectCorner.offset(0f, normalTagHeight - triangleHeight - cornerDiameter)
        normalPath.arcTo(rectCorner, 0f, 90f)
        normalPath.rLineTo(-(tagWidth - triangleWidth), 0f)
        normalPath.lineTo(0f, height - normalTagMarginBottom)

        bitmapRectF.left = imgPadding //padding 避免图标没留白时太满
        bitmapRectF.bottom = height - context.dp2px(15f)
        bitmapRectF.top = bitmapRectF.bottom - imgSize + imgPadding + imgPadding
        bitmapRectF.right = bitmapRectF.left + imgSize - imgPadding - imgPadding
    }

    fun updateActivePath(tagWidth: Float = activeWidth) {
        activePath.reset()
        activePath.moveTo(0f, height - activeTagMarginBottom)
        rectCorner.set(
                0f,
                height - activeTagMarginBottom - activeTagHeight,
                cornerDiameter,
                height - activeTagMarginBottom - activeTagHeight + cornerDiameter
        )
        activePath.arcTo(rectCorner, 180f, 90f)

        rectCorner.offset(tagWidth - cornerDiameter, 0f)
        activePath.arcTo(rectCorner, 270f, 90f)

        rectCorner.offset(0f, activeTagHeight - triangleHeight - cornerDiameter)
        activePath.arcTo(rectCorner, 0f, 90f)
        activePath.rLineTo(-(tagWidth - triangleWidth), 0f)
        activePath.lineTo(0f, height - activeTagMarginBottom)


        activeBitmapRectF.left = context.dp2px(2f)
        activeBitmapRectF.bottom = height - context.dp2px(21f)
        activeBitmapRectF.top = activeBitmapRectF.bottom - imgSize
        activeBitmapRectF.right = activeBitmapRectF.left + imgSize
    }


    private val gestureDetector: GestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        GestureDetector(context, gestureListener).also {
            it.setIsLongpressEnabled(false)
        }
    }

    /**
     * 通过X坐标找到是哪个分组
     * @return null为不是分组
     */
    open fun findGroupItemByX(x: Float): TagLineViewData? {
        val timeLineValue = timeLineValue ?: return null
        if (groupData.isEmpty()) {
            return null
        }

        for (item in sortData) {
            val startX = timeLineValue.time2X(item.tagDrawStartTime, cursorX)
            val endX = startX + getItemWidth(item)

            if (x in startX..endX) {
                return item
            }
        }

        return null
    }

    /**
     * item 宽度
     */
    open fun getItemWidth(item: TagLineViewData): Float {
        return if (item == activeItem) {
            activeWidth
        } else {
            normalWidth
        }
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        var downItem: TagLineViewData? = null
        override fun onDown(e: MotionEvent?): Boolean {
            eventHandle.removeLongPressEvent()
            if (e != null) {
                downItem = findGroupItemByX(e.x)
                downItem?.let {
                    activeItem?.let { activeItem ->
                        val timeLineValue = timeLineValue ?: return@let

                        val startX = timeLineValue.time2X(activeItem.tagDrawStartTime, cursorX)
                        val endX = startX + getItemWidth(activeItem)

                        if (e.x in startX..endX) { //点的刚好是激活的
                            eventHandle.sendLongPressAtTime(e.downTime)
                        }
                    }

                    return true
                }
            }
            return super.onDown(e)
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (e != null) {
                if (eventHandle.wholeMoveMode) {
                    return false
                }

                val downItem = downItem ?:return false

                val list = data.filter {
                    downItem.groupHead == it.groupHead
                }

                if (list.isEmpty()) {
                    return false
                }
                val selectItemCenterX =
                    normalWidth / 2 + (timeLineValue?.time2X(list[list.size - 1].tagDrawStartTime, cursorX)
                        ?: 0f)

                if (list.size == 1) {
                    onItemClickListener?.onItemClick(downItem, selectItemCenterX)
                } else {
                    val array = list.reversed().toList()
                    onItemClickListener?.onItemGroupClick(array, selectItemCenterX)
                }

            }
            return super.onSingleTapUp(e)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            eventHandle.onScroll(e1, e2, distanceX, distanceY)
            return true

        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        eventHandle.autoHorizontalScrollAnimator?.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val onTouchEvent = gestureDetector.onTouchEvent(event)
        eventHandle.onTouchEvent(event)
        return onTouchEvent
    }


    var onItemClickListener: OnItemClickListener? = null


    /**
     * 点击事件
     */
    interface OnItemClickListener {
        /**
         * 点了单个
         */
        fun onItemClick(item: TagLineViewData, x: Float)

        /**
         * 点了一组
         */
        fun onItemGroupClick(groupData: List<TagLineViewData>, x: Float)
    }

    /**
     * 重置
     */
    fun reset() {
        data.clear()
        activeItem = null
        dataChange()
    }

}