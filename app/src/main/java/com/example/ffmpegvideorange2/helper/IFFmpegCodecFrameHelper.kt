package com.example.ffmpegvideorange2.helper

import android.graphics.*
import android.media.*
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.ffmpegvideorange2.*
import com.example.myplayer.KzgPlayer
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.helper.DiskCacheAssist
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.helper.OnGetFrameBitmapCallback
import com.sam.video.util.md5
import com.sam.video.util.notNull
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class IFFmpegCodecFrameHelper(
    override var filePath:String = "",
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable {

    private var kzgPlayer:KzgPlayer? = null
    var yuvQueue: YuvQueue = YuvQueue()

    var targetViewMap: ConcurrentHashMap<ImageView, TargetBean> = ConcurrentHashMap()
    private var childThread:Thread? = null
    //是否停止解码线程
    private var isStop = false
    override var isPause = false
    //当前最后解码出来的一个帧，用来作为还没有来得及解码的预览帧
    override var lastBitMap: Bitmap? = null
    override var isSeekBack: Boolean = true
    override var isScrolling: Boolean = false
    override var decodeFrameListener: IAvFrameHelper.DecodeFrameListener? = null
    override var itemFrameForTime:Long= 0
    override var iframeSearch:IFrameSearch? = null
    override var videoIndex:Int = 0
    //是初始化了recyclerView的Item
    private var isInitItem = false
    private var lastCodecFramePts = 0L
    private var startTime = 0L
    private var diskCache:DiskCacheAssist? = null
    private val mainKey = md5(filePath)
    //是否应该暂停 循环线程
    private var hasPause = false
    //是否还有需要显示的抽帧
    private var hasNeedShowFrame = true



    override fun init() {
        iframeSearch = IFrameSearch(filePath)
        diskCache = DiskCacheAssist(TextUtils.concat(
            Environment.getExternalStorageDirectory().path,
            "/frameCache/", mainKey).toString(), 1, 1, 121)
        childThread = Thread(this)
        childThread!!.start()

    }

    override fun loadAvFrame(view: RecyclerView.ViewHolder, timeMs: Long) {
        hasPause = false
    }

    override fun loadAvFrame(view: ImageView, timeMs: Long) {
        targetViewMap[view] = targetViewMap[view]?:TargetBean()
        Log.e("kzg","**************添加一个view:${view} , ${targetViewMap[view]?.timeUs}  ,${timeMs}   index:$videoIndex")
        lastBitMap?.let {
            if (targetViewMap[view]?.isAddFrame == false){
                view.setImageBitmap(it)
            }
        }

        targetViewMap[view]?.timeUs = timeMs
        targetViewMap[view]?.isAddFrame = false
        targetViewMap[view]?.isRemoveTag = false
        hasPause = false
        /*diskCache?.asyncReadBitmap("${filePath}_${timeMs}",timeMs,{bp,us ->
            if (targetViewMap[view]?.timeUs == us){
                //Log.e("kzg","**************取一帧bitmap成功：${timeMs}")
                bp?.let {
                    targetViewMap[view]?.isAddFrame = true
                    view.setImageBitmap(it)
                }
            }

        },{
            Log.e("kzg","**************取一帧bitmap失败：${timeMs}")
        })*/
        if (targetViewMap[view]?.timeUs != timeMs){

        }
        if (!isInitItem) {
            isInitItem = true
            isPause = false
            kzgPlayer?.pauseGetPacket(false,videoIndex)
        }
    }

    override fun addAvFrame(view: ImageView) {
        targetViewMap[view]?.isRemoveTag = false
    }

    override fun removeAvFrameTag(view: ImageView) {
        Log.e("kzg","**************移除一个view:${view} , ${targetViewMap[view]?.timeUs}  ，index:$videoIndex")
        targetViewMap[view]?.isRemoveTag = true
        if (hasNeedShowFrame){
            //判断是否有需要解码的帧
            targetViewMap.forEach {
                if (!it.value.isAddFrame){
                    hasNeedShowFrame = true
                    return@forEach
                }
            }
        }
        //Log.e("kzg","********************hasNeedShowFrame:$hasNeedShowFrame")
    }

    override fun removeAvFrame() {
        //Log.e("kzg","*****************removeAvFrame size:${targetViewMap.size}")
        targetViewMap.forEach {
            if (it.value.isRemoveTag){
                targetViewMap.remove(it.key)
            }
        }
    }


    override fun release() {
        isStop = true
        childThread?.join()
        kzgPlayer?.let {
            it.release()
            kzgPlayer = null
        }
        targetViewMap.clear()
        yuvQueue?.let {
            for (index in 0 until it.queueSize){
                it.deQueue().apply {
                    y = null
                    u = null
                    v = null
                }
            }
        }
        yuvQueue.clear()
        iframeSearch?.release()
    }


    override fun pause() {
        //Log.e("kzg","**************pause:")
        isPause = true
        kzgPlayer?.pauseGetPacket(true,videoIndex)
    }

    override fun run() {
        var index = 0
        while (!isStop){

            if (yuvQueue.queueSize  == 0){
                Thread.sleep(10)
                if (!isScrolling && hasNeedShowFrame){
                    isPause = false
                    //Log.e("kzg","******************pauseGetPacket")
                    kzgPlayer?.pauseGetPacket(false,videoIndex)
                }
                continue
            }
            if (isScrolling){
                Thread.sleep(10)
                continue
            }
            if (hasPause){
                Thread.sleep(10)
                continue
            }


            //遍历ImageView 匹配时间，转换yuv为bitmap
            hasPause = true
            index = 0
            run task@{
                Utils.sortHashMap(targetViewMap).forEach {
                    index ++
                    if (isScrolling){
                        return@task
                    }

                    yuvQueue.first?.let {bean ->
                        //需要的展示的视频帧时间 大于 当前解码的帧的时间 并且 需要展示的view 还没有展示帧
                        if ((it.value.timeUs.toDouble() >= bean.timeUs && !it.value.isAddFrame && !it.value.isRemoveTag) || (!it.value.isAddFrame && !it.value.isRemoveTag)){
                            hasPause = false
                            yuvQueue.deQueue()?.apply {
                                lastCodecFramePts = timeUs
                                if (((it.value.timeUs >= this.timeUs-20_000 && it.value.timeUs<=this.timeUs+20_000)
                                    || (this.timeUs-it.value.timeUs>=30_000) || (it.value.timeUs < 30_000 && this.timeUs > it.value.timeUs))
                                    && !it.value.isAddFrame){
                                        //Log.e("kzg","**************timeUs:$timeUs  ,view timeUs:${it.value.timeUs}")
                                    if (isScrolling){
                                        return@task
                                    }
                                    isPause = false
                                    kzgPlayer?.pauseGetPacket(false,videoIndex)
                                    notNull(y,u,v){
                                        val bitmap = VideoUtils.rawByteArray2RGBABitmap2(VideoUtils.YUVToNv21(y,u,v),width,height,practicalWidth)
                                        val newBitmap = VideoUtils.compressBySampleSize(bitmap,100,100,true)
                                        if (isScrolling){
                                            return@task
                                        }
                                        Log.e("kzg","**************找到一帧:${it.key}  ,timeUs:$timeUs  ,view timeUs:${it.value.timeUs}  ,index:$videoIndex  ，isRemoveTag：${it.value.isRemoveTag}")

                                        newBitmap?.let { bp ->
                                            lastBitMap = bp
                                            it.value.isAddFrame = true
                                           /* diskCache?.writeBitmap("${filePath}_${it.value.timeUs}",bp,{bitmap
                                                Log.e("kzg","**************缓存一帧bitmap成功：${it.value.timeUs}")
                                            },{ e ->
                                                Log.e("kzg","**************缓存一帧bitmap失败：${it.value.timeUs}")
                                            })*/
                                            it.key.post {
                                                it.key.setImageBitmap(bp)
                                                targetViewMap.forEach { mp ->
                                                    if (!mp.value.isAddFrame && !mp.value.isRemoveTag){
                                                        mp.key.setImageBitmap(lastBitMap)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        Log.e("kzg","*******************结束预览条解码线程")
    }


    fun setKzgPlayer(player: KzgPlayer){
        this.kzgPlayer = player
    }




    override fun seek() {
        Log.e("kzg","****************开始seek   index:$videoIndex")
        startTime = System.currentTimeMillis()
        Utils.sortHashMapForHolder(targetViewMap).apply {
            var i=0
            var j=0
            var minTimeUs = Long.MAX_VALUE
            var hasNoAddFrame = false
            var index = 0
            this.forEach {
                Log.e("kzg","****************开始seek isAddFrame:${it.value.isAddFrame}  timeus:${it.value.timeUs} , size:${this.size}  , index:$videoIndex")
                if (!it.value.isAddFrame && it.value.timeUs >=0){
                    if (!isSeekBack){
                        //判断前后两帧时间间隔，大于每帧实际间隔就跳过这一帧
                        if ((index + 1) < this.size && this[index + 1].value.timeUs - it.value.timeUs > itemFrameForTime * 1000){
                            Log.e("kzg","*********************跳过一帧1")
                            it.value.isAddFrame = true
                        }else{
                            minTimeUs = if (minTimeUs < it.value.timeUs) minTimeUs else it.value.timeUs
                            hasNoAddFrame = true
                        }

                    }else{
                        //判断前后两帧时间间隔，大于每帧实际间隔就跳过这一帧
                        if ((index + 1) < this.size && this[index + 1].value.timeUs - it.value.timeUs > itemFrameForTime * 1000){
                            Log.e("kzg","*********************跳过一帧2")
                            it.value.isAddFrame = true
                        }else{
                            minTimeUs = if (minTimeUs < it.value.timeUs) minTimeUs else it.value.timeUs
                            hasNoAddFrame = true
                        }
                    }
                }
                index ++
            }
            //如果没有需要解码的帧，就直接返回
            if (!hasNoAddFrame){
                return
            }
            val func =  {
                val ite = iframeSearch!!.IframeUs.iterator()
                var index = 0
                Log.e("kzg","**************minTimeUs:$minTimeUs")
                while (ite.hasNext()){
                    val frame = ite.next()
                    //当前recyclerView最小的item帧的时间戳所属的gop index
                    if (index > 0 && minTimeUs >=iframeSearch!!.IframeUs[index - 1] && minTimeUs < frame){
                        i = index
                    }

                    //已解码的帧的pts所属的gop
                    if (index > 0 && lastCodecFramePts >=iframeSearch!!.IframeUs[index - 1] && lastCodecFramePts < frame ){
                        j = index
                    }
                    index ++
                }
                //如果是回退，那么是肯定需要ffmpeg去seek的
                if (isSeekBack){
                    false
                }else{
                    //如果将要解码的帧所属的gop与已经解码出来的最后一帧所属的是同一个gop 或者 将要解码的帧的时间小于 avpacket队列的最大帧的时间，就认为是同一个gop
                    i == j /*|| minTimeUs <= kzgPlayer?.getAvPacketQueueMaxPts()?.toLong()?:0*/
                }
            }

            val isCurrentGop =func().apply {
                Log.e("kzg","********************isCurrentGop:$this")
                if (!this){
                    yuvQueue.clear()
                }
            }
            i = if (i <= 0) iframeSearch!!.IframeUs.size else i
            //如果还在一个gop内，就取需要显示的帧的时间（这种情况其实不需要用到这个），如果不在同一个gop,就取要显示的的帧的pts所在的gop
            val pts = (if (isCurrentGop) minTimeUs/1000_000.0 else iframeSearch!!.IframeUs[i-1]/1000_000.0).apply {
                Log.e("kzg","********************需要seek的I帧:$this  ， 实际需要展示的时间最小帧：${minTimeUs}   index:$videoIndex")
            }
            Log.e("kzg","****************seek结束   index:$videoIndex")
            kzgPlayer?.seekFrame(pts.toDouble(),isCurrentGop,videoIndex)

            targetViewMap.forEach {
                Log.e("kzg","*****************targetViewMap.forEach:${it.key.tag}  ,${it.value}")
            }
        }
    }



}