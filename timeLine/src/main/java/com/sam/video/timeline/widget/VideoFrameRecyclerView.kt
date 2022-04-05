package com.sam.video.timeline.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sam.video.timeline.adapter.VideoFrameAdapter
import com.sam.video.timeline.bean.VideoClip
import com.sam.video.timeline.bean.VideoFrameData
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.MediaCodecAvFrameHelper
import com.sam.video.timeline.helper.VideoDecoder2
import com.sam.video.timeline.listener.OnFrameClickListener
import com.sam.video.util.dp2px
import kotlin.math.min

/**
 * 帧列表
 * 单视频：没有间隔，直接按视频长度换算
 * 多视频，首尾两个 + 一半间隔，中间的 + 完整间隔
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-29
 */
class VideoFrameRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), TimeLineBaseValue.TimeLineBaseView {
    /** 视频数据 */
    var videoData: List<VideoClip>? = null
    /** 帧数据 */
    val listData = mutableListOf<VideoFrameData>()
    private val frameWidth by lazy(LazyThreadSafetyMode.NONE) { context.dp2px(48f).toInt() }
    private val decorationWidth by lazy(LazyThreadSafetyMode.NONE) { context.dp2px(2f).toInt() }
    val halfDurationSpace = decorationWidth / 2
    private val videoFrameItemDecoration: VideoFrameItemDecoration

    init {
        adapter = VideoFrameAdapter(listData, frameWidth)
        videoFrameItemDecoration = VideoFrameItemDecoration(context)
        addItemDecoration(videoFrameItemDecoration)

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (scrollState == SCROLL_STATE_IDLE) {
                    //过滤updateTime 导致的滚动
                    return
                }

                (parent as? ZoomFrameLayout)?.let { zoomLayout ->
                    if (zoomLayout.isScrollIng()) {
                        zoomLayout.flingAnimation.cancel()
                    }

                    getCurrentCursorTime()?.let {
                        zoomLayout.updateTimeByScroll(it)
                    }
                }


            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val timeLineValue = timeLineValue ?: return
                val timeChangeListener = (parent as? ZoomFrameLayout)?.timeChangeListener ?: return

                when (newState) {
                    SCROLL_STATE_DRAGGING -> {
                        timeChangeListener.startTrackingTouch()
                    }
                    SCROLL_STATE_SETTLING ->{}
                    SCROLL_STATE_IDLE -> {
                        timeChangeListener.stopTrackingTouch(timeLineValue.time)
                        if (needUpdateTimeWhenScrollEnd) {
                            updateTime()
                        }
                    }
                }
            }
        })
    }

    var hasBorder: Boolean
        set(value) {
            videoFrameItemDecoration.hasBorder = value
            invalidate()
        }
        get() = videoFrameItemDecoration.hasBorder

    override var timeLineValue: TimeLineBaseValue? = null

    override fun scaleChange() {
        Log.d("Sam", "wds : scaleChange")
        rebindFrameInfo()
    }

    /**
     * 重新绑定视频的帧信息
     * 间隔：前后间隔都当作自己一帧
     * */
    fun rebindFrameInfo() {
        listData.clear()
        val timeLineValue = timeLineValue ?: return
        val videoData = videoData ?: return
        //        videoHelper?.buildSuccessWidthCurveSpeed = false
        if (videoData.isEmpty()) {
            adapter?.notifyDataSetChanged()
            return
        }
        //一帧多少ms
        val frameTime = (frameWidth * 1000 / timeLineValue.pxInSecond).toLong()
        //间隔对应多少ms
        val decorationTime = (decorationWidth * 1000 / timeLineValue.pxInSecond).toLong()

        var fitOffsetTime = 0f //修正多个视频的精度偏移值

        for ((index, item) in videoData.withIndex()) {

            var time = 0L //该视频片断的，时间轴时间，从0开始；
            var clipTime = 0L
            var duration = item.durationMs //视频的持续时间
            val speed = 1
            val clipFrameTime = (frameTime * speed).toLong() //一帧对应的文件时间
            //曲线变速生效
            val curveSpeedEffect = false
            if (index < videoData.size - 1) {
                //分后一半间隔，把间隔当作这一帧的一部分
                duration -= decorationTime / 2
            }

            if (index > 0) {
                //分前一半间隔，把间隔自己当作一帧
                time += decorationTime / 2
            }

            var element: VideoFrameData? = null
            val durationPx = timeLineValue.time2px(duration).toInt()

            fitOffsetTime += timeLineValue.time2px(time) //将上一个视频的精度遗留到下一个视频中
            var widthCount = fitOffsetTime.toInt() //距离之和
            fitOffsetTime -= widthCount

//            Log.d("Sam", "----- item.startAtMs: ${item.startAtMs} $frameTime")
            while (widthCount < durationPx) {
                val isFirst = element == null
                var itemWidth = if (widthCount + frameWidth <= durationPx) {
                    frameWidth
                } else {
                    durationPx - widthCount //最后一个应该把误差都算进去: 视频的总长度，减去已经分割的长度
                }

                var left = 0
                if (isFirst) { //第一帧可能不是完整帧宽度
                    left = timeLineValue.time2px(((item.startAtMs % clipFrameTime) / speed).toLong()).toInt()
                    itemWidth = min(
                        itemWidth,
                        frameWidth - left
                    )
//                    Log.d("wds", "wds  ------------------ left:$left leftTime: ${timeLineValue.px2time(left.toFloat())}")
                    //第一帧固定，防止剪辑时闪
                    clipTime = item.startAtMs - (item.startAtMs % clipFrameTime)
                } else {
                    if (clipTime + clipFrameTime <= item.endAtMs) {
                        clipTime += clipFrameTime
                    } else {
                        clipTime = item.endAtMs
                    }

                }

                element = VideoFrameData(item, time, clipTime, itemWidth, isFirst, false, left)
                listData.add(element)
//                Log.d("Sam", "-----: $time, $clipTime")

                time += if (isFirst) {
                    frameTime - timeLineValue.px2time(left.toFloat())
                } else {
                    frameTime
                }
                widthCount += itemWidth
            }
            element?.isLastItem = true
        }
        adapter?.notifyDataSetChanged()
        updateTime()
    }

    private var needUpdateTimeWhenScrollEnd = false //标记一些更新时间时正在滚动，等滚动完成后重新校正
    override fun updateTime() {
        if (listData.isEmpty()) {
            return
        }

        if (scrollState == SCROLL_STATE_IDLE) {
            val timeLineValue = timeLineValue ?: return
            var position = listData.size - 1 //精度问题会导致遍历完了还没找到合适的Item,此时进度条一定是在最后一个item的最右边
            var offsetX = listData[listData.size - 1].frameWidth

            var offsetTime = 0L //偏移时间

            var lastVideo: VideoClip? = null //上一个视频
            var lastFrame: VideoFrameData? = null //上一帧

            for ((index, item) in listData.withIndex()) {
                if (lastVideo === item.videoData) { //这个视频还没超过
                    continue
                } else {
                    if (offsetTime + item.videoData.durationMs < timeLineValue.time) {
                        //加上当前这个完整视频还不够，则可以跳过这个视频的所有帧

                        offsetTime += item.videoData.durationMs
                        lastVideo = item.videoData
                    } else {
                        //定位到该视频

                        if (item.isLastItem || offsetTime + item.time >= timeLineValue.time) {
                            //已经是这个视频的最后一帧的项了，或开始时间已经超过光标了

                            //光标在这个视频的的相对时间
                            offsetTime = timeLineValue.time - offsetTime
                            offsetTime = if (lastFrame == null) {
                                position = index
                                offsetTime - item.time
                            } else {
                                offsetTime - lastFrame.time
                            }

                            offsetX = timeLineValue.time2px(offsetTime).toInt()

                            break
                        } else {
                            position = index
                            lastFrame = item
                            continue
                        }
                    }
                }
            }
            //offset 负就向左，
            (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, -offsetX)
            needUpdateTimeWhenScrollEnd = false
        } else {
            needUpdateTimeWhenScrollEnd = true
        }
    }

    var frameClickListener: OnFrameClickListener? = null

    override fun addOnItemTouchListener(listener: OnItemTouchListener) {
        super.addOnItemTouchListener(listener)
        (listener as? OnFrameClickListener)?.let {
            frameClickListener = it
        }
    }

    var scaleGestureDetector: ScaleGestureDetector? = null

    private fun disableLongPress() {
        frameClickListener?.gestureDetector?.setIsLongpressEnabled(false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
//        Log.d("Sam", " $e ")
        if (e.pointerCount > 1) {
            // todo 找更合适的地方禁用长按
            disableLongPress()
        }

        if (scrollState != SCROLL_STATE_IDLE) {
            disableLongPress()
            return super.onTouchEvent(e)
        }

        if (scaleGestureDetector == null) {
            (parent as? ZoomFrameLayout)?.scaleGestureListener?.let {
                scaleGestureDetector =
                    ScaleGestureDetector(this@VideoFrameRecyclerView.context, it)
            }
        }

        scaleGestureDetector?.let {
            if (scrollState == SCROLL_STATE_IDLE && e.pointerCount > 1) {
                val scaleEvent = it.onTouchEvent(e)
                if (it.isInProgress) {
                    return@onTouchEvent scaleEvent
                }
            }
        }

        if (e.pointerCount > 1) {
            return true
        }

        return super.onTouchEvent(e)

    }

    /** 通过X坐标找到对应的视频 */
    fun findVideoByX(x: Float): VideoClip? {
        val child = findChildViewByX(x) ?: return null
        val position = getChildAdapterPosition(child)
        return listData.getOrNull(position)?.videoData
    }

    /**
     * 通过x坐标找到对应的view
     */
    private fun findChildViewByX(x: Float): View? {
        val findChildViewUnder = findChildViewUnder(x, height / 2f)
        findChildViewUnder?.let {
            return it
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val position = getChildAdapterPosition(child)
            if (position !in 0 until listData.size) {
                continue
            }
            val item = listData[position]
            val left = if (item.isFirstItem && position > 0) {
                child.left - halfDurationSpace
            } else {
                child.left
            }

            val right = if (item.isLastItem && position < listData.size - 1) {
                child.right + halfDurationSpace
            } else {
                child.right
            }

            if (left <= x && x <= right) {
                return child
            }
        }

        return null
    }

    /**
     * 当前游标指定的View
     * 用时间去找，比用坐标找更精确！可以精确到1ms,坐标只能精确到1px
     */
    private fun getCurrentCursorView(): View? {
        return findChildViewByX(paddingLeft.toFloat())
    }

    private val cursorX
        get() = paddingLeft

    private fun isAtEnd(): Boolean {
        return if (listData.isNotEmpty()) {
            val lastVH = findViewHolderForAdapterPosition(listData.size - 1) ?: return false
            lastVH.itemView.right <= cursorX
        } else {
            false
        }
    }

    /**
     * 当前游标指定的时间
     */
    private fun getCurrentCursorTime(): Long? {
        val child = getCurrentCursorView() ?: return null
        val videoData = videoData ?: return null
        val timeLineValue = timeLineValue ?: return null
        layoutManager?.canScrollHorizontally()
        if (isAtEnd()) {
            return timeLineValue.duration
        }

        val position = getChildAdapterPosition(child)
        if (position in 0 until listData.size) {
            val item = listData[position]
            val indexVideo = videoData.indexOfFirst { it === item.videoData }

            var time = 0L
            for (i in 0 until indexVideo) {
                time += videoData[i].durationMs
            }

            time += item.time
            var itemWidth = item.frameWidth

            if (indexVideo > 0 && item.isFirstItem) {
                itemWidth += halfDurationSpace
            }
            if (indexVideo < videoData.size - 1 && item.isLastItem) {
                itemWidth -= halfDurationSpace
            }

            val offsetX = paddingLeft - child.left
            val offsetTime = timeLineValue.px2time(offsetX.toFloat())
            time += offsetTime
            return time
        }
        return null

    }

    /**
     * 获取当前光标的指定的视频的范围
     *  当前item,
     * */
    fun getCurrentCursorVideoRect(rect: RectF) {

        val child = getCurrentCursorView() ?: return
        val videoData = videoData ?: return
        val timeLineValue = timeLineValue ?: return

        val position = getChildAdapterPosition(child)
        if (position in 0 until listData.size) {
            val item = listData[position]
            val indexVideo = videoData.indexOfFirst { it === item.videoData }

            var offset = 0f //手动计算偏移值，防止 timeLineValue.time2px(item.time) 有误差
            for (i in position - 1 downTo 0) {
                val itemCountWidth = listData[i]
                if (itemCountWidth.videoData !== item.videoData) {
                    break
                }
                offset += itemCountWidth.frameWidth
            }

            rect.top = child.top.toFloat()
            rect.bottom = child.bottom.toFloat()

            rect.left = child.left - offset //第一帧的左边

            rect.right = rect.left + timeLineValue.time2px(item.videoData.durationMs)

            if (indexVideo > 0) {
                rect.right -= halfDurationSpace
            }
            if (indexVideo < videoData.size - 1) {
                rect.right -= halfDurationSpace
            }

        }
    }


    fun setAvFrameHelper(helper: IAvFrameHelper){
        helper.init()
        (adapter as VideoFrameAdapter).setAvframeHelper(helper)
    }
    fun getAvFrameHelper():IAvFrameHelper?{
        return (adapter as VideoFrameAdapter).getAvframeHelper()
    }

    fun setVideoDecoder(decover:VideoDecoder2){
        (adapter as VideoFrameAdapter).videoDecoder2 = decover
    }

    fun release(){
        (adapter as VideoFrameAdapter).getAvframeHelper()?.release()
    }

}