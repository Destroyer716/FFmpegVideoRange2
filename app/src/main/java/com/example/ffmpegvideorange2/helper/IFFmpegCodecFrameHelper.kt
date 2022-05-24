package com.example.ffmpegvideorange2.helper

import android.graphics.*
import android.media.*
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.widget.ImageView
import com.example.ffmpegvideorange2.*
import com.example.myplayer.KzgPlayer
import com.example.myplayer.PacketQueue
import com.example.myplayer.mediacodec.KzglVideoSupportUtil
import com.example.myplayer.mediacodecframes.VideoToFrames
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.helper.OnGetFrameBitmapCallback
import com.sam.video.util.notNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*


class IFFmpegCodecFrameHelper(
    override var filePath:String = "",
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable {

    private var kzgPlayer:KzgPlayer? = null
    var yuvQueue: YuvQueue = YuvQueue()

    var targetViewMap:Hashtable<ImageView, TargetBean> = Hashtable()
    private var childThread:Thread? = null
    //是否停止解码线程
    private var isStop = false
    //当前最后解码出来的一个帧，用来作为还没有来得及解码的预览帧
    override var lastBitMap: Bitmap? = null
    override var isSeekBack: Boolean = true
    override var isScrolling: Boolean = false
    override var decodeFrameListener: IAvFrameHelper.DecodeFrameListener? = null
    //是初始化了recyclerView的Item
    private var isInitItem = false
    private var lastCodecFramePts = 0L
    private var startTime = 0L


    override fun init() {
        childThread = Thread(this)
        childThread!!.start()

    }

    override fun loadAvFrame(view: ImageView, timeMs: Long) {
        targetViewMap[view] = targetViewMap[view]?:TargetBean()
        Log.e("kzg","**************seekTime0:${timeMs} , $view , ${view.tag}")
        lastBitMap?.let {
            if (targetViewMap[view]?.isAddFrame == false){
                view.setImageBitmap(it)
            }
        }
        if (targetViewMap[view]?.timeUs != timeMs){
            targetViewMap[view]?.timeUs = timeMs
            targetViewMap[view]?.isAddFrame = false
        }


        if (!isInitItem) {
            isInitItem = true
            kzgPlayer?.pauseGetPacket(false)
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

    }


    override fun pause() {
        Log.e("kzg","**************pause:")
        kzgPlayer?.pauseGetPacket(true)
    }

    override fun run() {
        while (!isStop){
            if (yuvQueue.queueSize  == 0 || isScrolling){
                Thread.sleep(1)
                continue
            }

            //遍历ImageView 匹配时间，转换yuv为bitmap
            run task@{
                Utils.sortHashMap(targetViewMap).forEach {
                    yuvQueue.first?.let {bean ->
                        if ((it.value.timeUs.toDouble() >= bean.timeUs && !it.value.isAddFrame) || !it.value.isAddFrame){
                            yuvQueue.deQueue().apply {
                                if (((it.value.timeUs >= this.timeUs-20_000 && it.value.timeUs<=this.timeUs+20_000)
                                    || (this.timeUs-it.value.timeUs>=30_000)  ||(it.value.timeUs < 30_000 && this.timeUs > it.value.timeUs))
                                    && !it.value.isAddFrame){
                                        Log.e("kzg","**************timeUs:$timeUs")
                                    if (isScrolling){
                                        return@task
                                    }
                                    notNull(yuv){
                                        //val byte = y!! + u!! + v!!
                                        //val bitmap = VideoUtils.rawByteArray2RGBABitmap2(VideoUtils.I420Tonv21(byte,width,height),width,height)
                                        val bitmap = VideoUtils.rawByteArray2RGBABitmap2(VideoUtils.YUVToNv21(y,u,v),width,height)
                                        bitmap?.let { bp ->
                                            it.value.isAddFrame = true
                                            it.key.post {
                                                it.key.setImageBitmap(bp)
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
        Log.e("kzg","****************开始seek")
        startTime = System.currentTimeMillis()
        Utils.sortHashMap(targetViewMap).apply {
            var i=0
            var j=0
            var minTimeUs = Long.MAX_VALUE
            var hasNoAddFrame = false
            this.forEach {
                //这里做两个判断，一个是这个imageview 并没有被填充需要的帧，还有就是当前需要的帧与需要显示的最大的那个帧的时间相差不能超过12秒
                //这是为了进一步精确，因为可能会存在当前imageview标记的时间不是当前需要的最小的时间
                if (!it.value.isAddFrame ){
                    if (!isSeekBack&& (this[this.size - 1].value.timeUs - it.value.timeUs <= 12_000_000)){
                        minTimeUs = if (minTimeUs < it.value.timeUs) minTimeUs else it.value.timeUs
                        hasNoAddFrame = true
                    }else if(!isSeekBack&& (this[this.size - 1].value.timeUs - it.value.timeUs < 12_000_000)){
                        it.value.isAddFrame = true
                    }else if (isSeekBack){
                        //回退的时候不需要判断最小帧与最大帧
                        minTimeUs = if (minTimeUs < it.value.timeUs) minTimeUs else it.value.timeUs
                        hasNoAddFrame = true
                    }
                }
            }
            //如果没有需要解码的帧，就直接返回
            if (!hasNoAddFrame){
                return
            }
            val func =  {
                val ite = IFrameSearch.IframeUs.iterator()
                var index = 0
                while (ite.hasNext()){
                    val frame = ite.next()
                    //当前recyclerView最小的item帧的时间戳所属的gop index
                    if (index > 0 && minTimeUs >=IFrameSearch.IframeUs[index - 1] && minTimeUs < frame){
                        i = index
                    }

                    //已解码的帧的pts所属的gop
                    if (index > 0 && lastCodecFramePts >=IFrameSearch.IframeUs[index - 1] && lastCodecFramePts < frame ){
                        j = index
                    }
                    index ++
                }
                //如果是回退，那么是肯定需要ffmpeg去seek的
                if (isSeekBack){
                    false
                }else{
                    //如果将要解码的帧所属的gop与已经解码出来的最后一帧所属的是同一个gop 或者 将要解码的帧的时间小于 avpacket队列的最大帧的时间，就认为是同一个gop
                    i == j || minTimeUs <= kzgPlayer?.getAvPacketQueueMaxPts()?.toLong()?:0
                }
            }

            val isCurrentGop =func().apply {
                Log.e("kzg","********************isCurrentGop:$this")
                if (!this){
                    yuvQueue.clear()
                }
            }
            //如果还在一个gop内，就取需要显示的帧的时间（这种情况其实不需要用到这个），如果不在同一个gop,就取要显示的的帧的pts所在的gop
            val pts = (if (isCurrentGop) minTimeUs/1000_000.0 else IFrameSearch.IframeUs[i-1]/1000_000.0).apply {
                Log.e("kzg","********************需要seek的I帧:$this  ， 实际需要展示的时间最小帧：${minTimeUs}")
            }
            kzgPlayer?.seekFrame(pts.toDouble(),isCurrentGop)
        }
    }



}