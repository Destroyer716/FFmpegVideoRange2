package com.example.ffmpegvideorange2

import android.content.pm.ActivityInfo
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.ffmpegvideorange2.helper.IMediaCodecFrameHelper
import com.example.ffmpegvideorange2.scrollVelocity.RecyclerVelocityHandler
import com.example.ffmpegvideorange2.scrollVelocity.VelocityTrackListener
import com.example.ffmpegvideorange2.scrollVelocity.ViewVelocityHandler
import com.example.myplayer.KzgPlayer
import com.example.myplayer.KzgPlayer.PlayerListener
import com.example.myplayer.PacketBean
import com.example.myplayer.TimeInfoBean
import com.sam.video.timeline.bean.VideoClip
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.listener.OnFrameClickListener
import com.sam.video.timeline.listener.SelectAreaMagnetOnChangeListener
import com.sam.video.timeline.listener.VideoPlayerOperate
import com.sam.video.timeline.widget.TimeLineBaseValue
import com.sam.video.timeline.widget.ZoomFrameLayout
import com.sam.video.util.VideoUtils
import com.sam.video.util.getScreenWidth
import kotlinx.android.synthetic.main.activity_range_time_line.*
import kotlinx.android.synthetic.main.activity_range_time_line.rulerView
import kotlinx.android.synthetic.main.activity_range_time_line.rvFrame
import kotlinx.android.synthetic.main.activity_range_time_line.selectAreaView
import kotlinx.android.synthetic.main.activity_range_time_line.tagView
import kotlinx.android.synthetic.main.activity_range_time_line.zoomFrameLayout
import kotlinx.android.synthetic.main.activity_time_line.*
import java.util.*


class RangeTimeLineActivity : AppCompatActivity(){

    var inputPath = Environment.getExternalStorageDirectory().toString() + "/video5.mp4"

    private val PLAY_ENABLE_CHANGE_HANDLER = 1001

    private var kzgPlayer: KzgPlayer? = null
    //记录上次更新播放时间的时间点
    private var lastTime: Long = 0
    private var lastDx = 0

    private val videos = mutableListOf<VideoClip>()
    val timeLineValue = TimeLineBaseValue()
    private var lastScrollTime = 0L
    private var keyFramesTime:IFrameSearch? = null

    private val handler = Handler { msg ->
        when (msg.what) {
            PLAY_ENABLE_CHANGE_HANDLER -> if (iv_play_stop_video != null && iv_play_stop_video.getVisibility() == View.GONE && msg.obj as Boolean) {
                iv_play_stop_video.setVisibility(View.VISIBLE)
                av_loading.hide()
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_range_time_line)
        inputPath = intent.getStringExtra("filePath")
        av_loading.show()
        initView()
        initAction()
    }


    private fun initView(){
        kzgPlayer = KzgPlayer()
        kzgPlayer!!.setKzgGLSurfaceView(sv_video_view)
        iv_play_stop_video.postDelayed({ begin()},1000)



        val halfScreenWidth = rvFrame.context.getScreenWidth() / 2
        rvFrame.setPadding(halfScreenWidth, 0, halfScreenWidth, 0)
        rvFrame.addOnItemTouchListener(object : OnFrameClickListener(rvFrame) {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onLongClick(e: MotionEvent): Boolean {
                return false

            }

            override fun onClick(e: MotionEvent): Boolean {
                //点击的位置
                rvFrame.findVideoByX(e.x)?.let {
                    if (rvFrame.findVideoByX(rvFrame.paddingLeft.toFloat()) == it) {
                        //已选中，切换状态
                        selectVideo = if (selectVideo == it) {
                            null
                        } else {
                            it
                        }
                    } else {
                        //移动用户点击的位置到中间
                        rvFrame.postDelayed(
                            {
                                if (selectVideo != null) {
                                    selectVideo = rvFrame.findVideoByX(e.x)
                                }
                                rvFrame.smoothScrollBy((e.x - rvFrame.paddingLeft).toInt(), 0)
                            },
                            100
                        )
                    }
                } ?: run {
                    selectVideo?.let { selectVideo = null }
                    return false
                }

                return true
            }
        })

        val handler = RecyclerVelocityHandler(this)
        handler.setVelocityTrackerListener(object :VelocityTrackListener{
            override fun onVelocityChanged(velocity: Int) {
                Log.e("kzg","*************************VelocityTrackerListener onVelocityChanged:$velocity")
                //如果是向后滑动，只有当速度停下来才开始解码
                if (rvFrame.getAvFrameHelper()?.isSeekBack == true && velocity == 0){
                    rvFrame.getAvFrameHelper()?.isScrolling = false
                    rvFrame.getAvFrameHelper()?.seek()
                }

                //如果是向后滑动，就停止解码
                if (velocity < 0){
                    if (rvFrame.getAvFrameHelper()?.isScrolling == false){
                        rvFrame.getAvFrameHelper()?.isScrolling = true
                        rvFrame.getAvFrameHelper()?.pause()
                    }
                }

                if (velocity > 0){
                    //预览条向前滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == true){
                        rvFrame.getAvFrameHelper()?.isSeekBack = false
                    }
                }else if(velocity < 0){
                    //预览条向后滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == false){
                        rvFrame.getAvFrameHelper()?.isSeekBack = true
                    }
                }
            }

            override fun onScrollFast() {
                Log.e("kzg","*************************VelocityTrackerListener onScrollFast")
                if (rvFrame.getAvFrameHelper()?.isSeekBack == false){
                    //快速向前滚动，暂停解码
                    rvFrame.getAvFrameHelper()?.isScrolling = true
                    rvFrame.getAvFrameHelper()?.pause()
                }
            }

            override fun onScrollSlow() {
                Log.e("kzg","*************************VelocityTrackerListener onScrollSlow")
                clearSelectVideoIfNeed()
                //向前滚动速度慢下来，开始解码
                if (rvFrame.getAvFrameHelper()?.isSeekBack == false){
                    rvFrame.getAvFrameHelper()?.isScrolling = false
                    rvFrame.getAvFrameHelper()?.seek()
                }
            }
        })
        rvFrame.addOnScrollListener(handler)
/*        rvFrame.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Log.e("kzg","**************onScrollStateChanged SCROLL_STATE_IDLE")

                    lastDx = 0


                }else if (newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    Log.e("kzg","**************onScrollStateChanged SCROLL_STATE_DRAGGING")
                }else if (newState == RecyclerView.SCROLL_STATE_SETTLING){
                    Log.e("kzg","**************onScrollStateChanged SCROLL_STATE_SETTLING")
                }

            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dx > 0){
                    //预览条向前滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == true){
                        rvFrame.getAvFrameHelper()?.isSeekBack = false
                    }
                    //当滑动的像素大于30的时候暂停解码抽帧
                    if(dx > 10){
                        if (rvFrame.getAvFrameHelper()?.isScrolling == false) {
                            rvFrame.getAvFrameHelper()?.isScrolling = true
                        }
                    }else{
                        if (rvFrame.getAvFrameHelper()?.isScrolling == true) {
                            rvFrame.getAvFrameHelper()?.isScrolling = false
                        }
                    }

                }else if (dx < 0){
                    //预览条向后滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == false){
                        rvFrame.getAvFrameHelper()?.isSeekBack = true
                    }
                    //当滑动的像素小于-20的时候暂停解码抽帧
                    if(dx < 0){
                        if (rvFrame.getAvFrameHelper()?.isScrolling == false) {
                            rvFrame.getAvFrameHelper()?.isScrolling = true
                        }
                    }else{
                        if (rvFrame.getAvFrameHelper()?.isScrolling == true && dx == 0) {
                            rvFrame.getAvFrameHelper()?.isScrolling = false
                        }
                    }
                }
                val ndx = abs(dx)
                Log.e("kzg","*********************isSeekBack:${rvFrame.getAvFrameHelper()?.isSeekBack}  , dx:$dx  , lastDx:$lastDx")
                //速度大于30的时候暂停解码抽帧
                if ( ndx > 10 && lastDx <= 10 && rvFrame.getAvFrameHelper()?.isSeekBack == false){
                    lastDx = ndx
                    rvFrame.getAvFrameHelper()?.pause()
                }else if(rvFrame.getAvFrameHelper()?.isSeekBack == true && ndx > 0 *//*&& lastDx <= 20*//*){
                    lastDx = ndx
                    rvFrame.getAvFrameHelper()?.pause()
                }else if ( ndx <= 10 && lastDx > 10 && rvFrame.getAvFrameHelper()?.isSeekBack == false ){
                    Log.e("kzg","**************seek")
                    lastDx = ndx
                    rvFrame.getAvFrameHelper()?.seek()
                }else if (rvFrame.getAvFrameHelper()?.isSeekBack == true && ndx == 0*//* && lastDx > 20*//* ){
                    lastDx = ndx
                    rvFrame.getAvFrameHelper()?.seek()
                }
            }

        })*/
        bindVideoData()

        val duration = VideoUtils.getVideoDuration(this, inputPath)
        videos.add(
            VideoClip(
                UUID.randomUUID().toString(), inputPath,
                duration, 0, duration
            )
        )
        updateVideos()

        keyFramesTime = IFrameSearch(inputPath)
    }

    private fun initAction(){

        //开始或者暂停播放
        iv_play_stop_video.setOnClickListener {

            if (kzgPlayer == null) {
                return@setOnClickListener
            }
            if (KzgPlayer.playModel == KzgPlayer.PLAY_MODEL_DEFAULT) {
                //停止
                kzgPlayer!!.playModel = KzgPlayer.PLAY_MODEL_FRAME_PREVIEW
                iv_play_stop_video.setImageResource(R.drawable.play_ico)
            } else if (KzgPlayer.playModel == KzgPlayer.PLAY_MODEL_FRAME_PREVIEW) {
                //播放
                if (!kzgPlayer!!.enablePlay) {
                    return@setOnClickListener
                }
                kzgPlayer!!.playModel = KzgPlayer.PLAY_MODEL_DEFAULT
                iv_play_stop_video.setImageResource(R.drawable.stop_ico)
            }
        }


        //预览图滑动监听
        zoomFrameLayout.timeChangeListener = object :VideoPlayerOperate {
            override fun updateVideoInfo() {

            }

            override fun startTrackingTouch() {

            }

            override fun stopTrackingTouch(ms: Long) {

            }

            override fun updateTimeByScroll(time: Long) {
                if (time > lastScrollTime){
                    kzgPlayer?.showFrame(time.toDouble()/1000, KzgPlayer.seek_advance)
                }else if(time < lastScrollTime) {
                    kzgPlayer?.showFrame(time.toDouble()/1000, KzgPlayer.seek_back)
                }
                lastScrollTime = time
            }

        }

        zoomFrameLayout.onScrollVelocityChangeListener = object :ZoomFrameLayout.OnScrollVelocityChangeListener{
            override fun onVelocityChange(v: Float) {
                if (v > 0F){
                    //预览条向前滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == true){
                        rvFrame.getAvFrameHelper()?.isSeekBack = false
                    }

                    Log.e("kzg","**********************onVelocityChange:$v  ,isScrolling:${rvFrame.getAvFrameHelper()?.isScrolling}")
                    //如果向前滑动，并且速度大于60 并且isScrolling=false,就暂停解码
                    if (v > 50F && rvFrame.getAvFrameHelper()?.isScrolling == false){
                        rvFrame.getAvFrameHelper()?.isScrolling = true
                        rvFrame.getAvFrameHelper()?.pause()
                    }

                    //如果是向前滑动，并且速度小于60 并且isScrolling=true 就开始解码
                    if (v < 50F && rvFrame.getAvFrameHelper()?.isScrolling == true){
                        rvFrame.getAvFrameHelper()?.isScrolling = false
                        rvFrame.getAvFrameHelper()?.seek()
                    }

                }else if (v < 0F){
                    //预览条向后滑动
                    if (rvFrame.getAvFrameHelper()?.isSeekBack == false){
                        rvFrame.getAvFrameHelper()?.isSeekBack = true
                    }
                    //如果是向后滑动，并且isScrolling=false,速度大于50 就暂停解码
                    if (rvFrame.getAvFrameHelper()?.isScrolling == false && v < -50){
                        rvFrame.getAvFrameHelper()?.isScrolling = true
                        rvFrame.getAvFrameHelper()?.pause()
                    }
                }
                //如果是向后滑动，并且速度为0，并且isScrolling=true,就开始解码
                if (v == 0F && rvFrame.getAvFrameHelper()?.isScrolling == true){
                    rvFrame.getAvFrameHelper()?.isScrolling = false
                    rvFrame.getAvFrameHelper()?.seek()
                }
            }

        }

    }


    fun begin() {
        kzgPlayer!!.setSource(inputPath)
        kzgPlayer!!.playModel = KzgPlayer.PLAY_MODEL_FRAME_PREVIEW
        kzgPlayer!!.parpared()
        kzgPlayer!!.setPlayerListener(object : PlayerListener {
            override fun onError(code: Int, msg: String) {
                Log.e("kzg", "************************error:$msg")
                runOnUiThread { av_loading.hide() }
            }

            override fun onPrepare() {
                Log.e("kzg", "*********************onPrepare success")
                kzgPlayer!!.start()
            }

            override fun onLoadChange(isLoad: Boolean) {
                if (isLoad) {
                    Log.e("kzg", "开始加载")
                } else {
                    Log.e("kzg", "加载结束")
                }
            }

            override fun onProgress(currentTime: Long, totalTime: Long) {
                //Log.e("kzg","******************onProgress:"+currentTime + " ,totalTime:"+totalTime);
                //播放时间大于等于500毫米才更新一次播放时间
                if (currentTime - lastTime > 500 * 1000) {
                    lastTime = currentTime
                    runOnUiThread {
                        tv_video_range_play_time.text = Utils.MilliToMinuteTime(
                            currentTime / 1000
                        )
                    }
                }
                /*if (videoRangeView != null && kzgPlayer!!.playModel == KzgPlayer.PLAY_MODEL_DEFAULT) {
                    videoRangeView.setPlayPercent(currentTime.toFloat() / totalTime)
                }*/
            }

            override fun onTimeInfo(timeInfoBean: TimeInfoBean) {
                Log.e("kzg", "*********************timeInfoBean:$timeInfoBean")
            }

            override fun onEnablePlayChange(enable: Boolean) {
                if (handler != null) {
                    val message = Message()
                    message.what = PLAY_ENABLE_CHANGE_HANDLER
                    message.obj = enable
                    handler.sendMessage(message)
                }
            }

            override fun onPlayStop() {
                kzgPlayer!!.playModel = KzgPlayer.PLAY_MODEL_FRAME_PREVIEW
                iv_play_stop_video.setImageResource(R.drawable.play_ico)
            }

            override fun onComplete() {
                Log.e("kzg", "*********************onComplete:")
            }

            override fun onDB(db: Int) {
                //Log.e("kzg","**********************onDB:"+db);
            }

            override fun onGetVideoInfo(fps: Int, duration: Long, widht: Int, height: Int) {
                runOnUiThread { changeSurfaceViewSize(widht, height) }
            }
        })


        val avFrameHelper = IMediaCodecFrameHelper("",null)
        avFrameHelper.setKzgPlayer(kzgPlayer!!)
        rvFrame.setAvFrameHelper(avFrameHelper)

        kzgPlayer!!.initGetFrame(inputPath);
        kzgPlayer!!.setGetFrameListener(object :KzgPlayer.GetFrameListener{
            override fun onInited(
                codecName: String?,
                width: Int,
                height: Int,
                csd_0: ByteArray?,
                csd_1: ByteArray?
            ) {
                (rvFrame.getAvFrameHelper() as IMediaCodecFrameHelper).initMediaCodec(codecName,width, height, csd_0, csd_1,sv_video_test.holder.surface)
            }

            override fun onStarGetFrame() {
                runOnUiThread {
                    rvFrame.adapter?.notifyDataSetChanged()
                }

            }

            override fun getFramePacket(dataSize: Int, pts: Double, data: ByteArray?) {
                val packetBean = PacketBean()
                packetBean.data = data
                packetBean.pts = pts
                packetBean.dataSize = dataSize
                //Log.e("kzg","**************getFramePacket 入队一帧")
                (rvFrame.getAvFrameHelper() as IMediaCodecFrameHelper).packetQueue.enQueue(packetBean)
                if ((rvFrame.getAvFrameHelper() as IMediaCodecFrameHelper).packetQueue.queueSize >= 30){
                    rvFrame.getAvFrameHelper()?.pause()
                }
            }

        })
    }


    private fun changeSurfaceViewSize(widht: Int, height: Int) {
        var widht = widht
        var height = height
        val surfaceWidth: Int = sv_video_view.getWidth()
        val surfaceHeight: Int = sv_video_view.getHeight()

        //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        val max: Float
        max = if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //竖屏模式下按视频宽度计算放大倍数值
            Math.max(
                widht.toFloat() / surfaceWidth.toFloat(),
                height.toFloat() / surfaceHeight.toFloat()
            )
        } else {
            //横屏模式下按视频高度计算放大倍数值
            Math.max(
                widht.toFloat() / surfaceHeight.toFloat(),
                height.toFloat() / surfaceWidth.toFloat()
            )
        }

        //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        widht = Math.ceil((widht.toFloat() / max).toDouble()).toInt()
        height = Math.ceil((height.toFloat() / max).toDouble()).toInt()

        //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        val layoutParams: ViewGroup.LayoutParams = sv_video_view.getLayoutParams()
        layoutParams.width = widht
        layoutParams.height = height
        sv_video_view.layoutParams = layoutParams
    }


    private val videoSelectAreaChangeListener by lazy {
        object : SelectAreaMagnetOnChangeListener(selectAreaView.context) {
            override val timeJumpOffset: Long
                get() = selectAreaView.eventHandle.timeJumpOffset

            override val timeLineValue = (this@RangeTimeLineActivity).timeLineValue

            var downStartAtMs: Long = 0L
            var downEndAtMs: Long = 0L
            var downSpeed: Float = 1f
            override fun onTouchDown() {
                isOperateAreaSelect = true
                val selectVideo = selectVideo ?: return

                //更新边缘，此处边缘不限
                startTimeEdge = 0
                endTimeEdge = Long.MAX_VALUE

                downStartAtMs = selectVideo.startAtMs
                downEndAtMs = selectVideo.endAtMs
            }

            override fun onTouchUp() {
                isOperateAreaSelect = false
            }

            override fun onChange(
                startOffset: Long,
                endOffset: Long,
                fromUser: Boolean
            ): Boolean {
                if (filterOnChange(startOffset, endOffset)) {
                    return true
                }
                val selectVideo = selectVideo ?: return false
                if (startOffset != 0L) {
                    //    - 起始位置移动时，相对时间轴的开始位置其实是不变的，变的是当前选择视频的开始位置+长度 （此时因为总的时间轴变长，所以区域变化了）
                    val oldStartTime = selectVideo.startAtMs
                    selectVideo.startAtMs += (downSpeed * startOffset).toLong()
                    //起始位置 + 吸附产生的时间差
                    selectVideo.startAtMs += checkTimeJump(
                        selectAreaView.startTime,
                        startOffset < 0
                    ) - selectAreaView.startTime

                    if (selectVideo.startAtMs < 0) {
                        selectVideo.startAtMs = 0
                    }
                    if (selectVideo.startAtMs > selectVideo.endAtMs - timeLineValue.minClipTime) {
                        selectVideo.startAtMs = selectVideo.endAtMs - timeLineValue.minClipTime
                    }


                    selectAreaView.endTime =
                        selectAreaView.startTime + selectVideo.durationMs //这样是经过换算的
                    val realOffsetTime = selectVideo.startAtMs - oldStartTime
                    if (fromUser) { //光标位置反向移动，保持时间轴和手的相对位置
                        timeLineValue.time -= (realOffsetTime / downSpeed).toLong()
                        if (timeLineValue.time < 0) {
                            timeLineValue.time = 0
                        }
                    }
                    updateVideoClip()
                    return realOffsetTime != 0L
                } else if (endOffset != 0L) {
                    //   - 结束位置移动时，范围的起始位置也不变，结束位置会变。
                    val oldEndMs = selectVideo.endAtMs
                    selectVideo.endAtMs += (downSpeed * endOffset).toLong()
                    selectAreaView.endTime = selectAreaView.startTime + selectVideo.durationMs

                    selectVideo.endAtMs += checkTimeJump(
                        selectAreaView.endTime,
                        endOffset < 0
                    ) - selectAreaView.endTime
                    if (selectVideo.endAtMs < selectVideo.startAtMs + timeLineValue.minClipTime) {
                        selectVideo.endAtMs = selectVideo.startAtMs + timeLineValue.minClipTime
                    }
                    if (selectVideo.endAtMs > selectVideo.originalDurationMs) {
                        selectVideo.endAtMs = selectVideo.originalDurationMs
                    }
                    selectAreaView.endTime = selectAreaView.startTime + selectVideo.durationMs
                    val realOffsetTime = selectVideo.endAtMs - oldEndMs
                    if (!fromUser) {
                        //结束位置，如果是动画，光标需要跟着动画
                        timeLineValue.time += (realOffsetTime / downSpeed).toLong()
                        if (timeLineValue.time < 0) {
                            timeLineValue.time = 0
                        }
                    }
                    updateVideoClip()
                    return realOffsetTime != 0L
                }
                return false
            }
        }
    }


    /** 选段 */
    var selectVideo: VideoClip? = null
        set(value) {
            field = value
            if (value == null) {
                //取消选中
                rvFrame.hasBorder = true
                selectAreaView.visibility = View.GONE
            } else {
                clearTagSelect()
                //选中视频
                selectAreaView.startTime = 0
                selectAreaView.onChangeListener = videoSelectAreaChangeListener
                for ((index, item) in videos.withIndex()) {
                    if (item === value) {
                        selectAreaView.offsetStart = if (index > 0) {
                            rvFrame.halfDurationSpace
                        } else {
                            0
                        }
                        selectAreaView.offsetEnd = if (index < videos.size - 1) {
                            rvFrame.halfDurationSpace
                        } else {
                            0
                        }
                        break
                    }
                    selectAreaView.startTime += item.durationMs
                }
                selectAreaView.endTime = selectAreaView.startTime + value.durationMs
                rvFrame.hasBorder = false
                selectAreaView.visibility = View.VISIBLE
            }
        }

    private fun bindVideoData() {
        zoomFrameLayout.scaleEnable = true
        rvFrame.videoData = videos

        zoomFrameLayout.timeLineValue = timeLineValue
        zoomFrameLayout.dispatchTimeLineValue()
        zoomFrameLayout.dispatchScaleChange()
    }

    /**
     * 是否正在操作区域选择
     */
    private var isOperateAreaSelect = false

    private fun clearSelectVideoIfNeed() {
        if (selectVideo != null && !selectAreaView.timeInArea()
            && !isOperateAreaSelect //未操作区域选择时
        ) {
            selectVideo = null
        }
    }

    /**
     * 清除选中模式
     */
    private fun clearTagSelect() {
        selectAreaView.visibility = View.GONE
        rvFrame.hasBorder = true
        tagView.activeItem = null
    }

    /**
     * 更新视频的截取信息
     * update and dispatch
     * */
    private fun updateVideoClip() {
        updateTimeLineValue(true)
        rvFrame.rebindFrameInfo()
        rulerView.invalidate()
        selectAreaView.invalidate()
    }

    /**
     * 更新全局的时间轴
     * @param fromUser 用户操作引起的，此时不更改缩放尺度
     */
    private fun updateTimeLineValue(fromUser: Boolean = false) {
        /**
        1、UI定一个默认初始长度（约一屏或一屏半），用户导入视频初始都伸缩为初始长度；初始精度根据初始长度和视频时长计算出来；
        2、若用户导入视频拉伸到最长时，总长度还短于初始长度，则原始视频最长能拉到多长就展示多长；
        3、最大精度：即拉伸到极限时，一帧时长暂定0.25秒；
         */
        timeLineValue.apply {
            val isFirst = duration == 0L
            duration = totalDurationMs
            if (time > duration) {
                time = duration
            }

//            if (fromUser || duration == 0L ) {
//                return
//            }
            if (isFirst) {//首次
                resetStandPxInSecond()
            } else {
                fitScaleForScreen()
            }
            zoomFrameLayout.dispatchTimeLineValue()
            zoomFrameLayout.dispatchScaleChange()
        }
    }

    private val totalDurationMs: Long //当前正在播放视频的总时长
        get() {
            var result = 0L
            for (video in videos) {
                result += video.durationMs
            }
            return result
        }

    private fun updateVideos() {
        rvFrame.rebindFrameInfo()
        updateTimeLineValue(false)
    }


    override fun onDestroy() {
        super.onDestroy()
        rvFrame.release()
        handler.removeCallbacksAndMessages(null)
        if (kzgPlayer != null) {
            kzgPlayer!!.stop()
            kzgPlayer!!.release()
            kzgPlayer!!.setPlayerListener(null)
            kzgPlayer!!.getFrameListener= null
            kzgPlayer = null
        }
        keyFramesTime?.release()

        if (sv_video_view != null) {
            sv_video_view.removeCallbacks(null)

        }
    }


    fun isMainThread(): Boolean {
        return Looper.getMainLooper() == Looper.myLooper()
    }
}