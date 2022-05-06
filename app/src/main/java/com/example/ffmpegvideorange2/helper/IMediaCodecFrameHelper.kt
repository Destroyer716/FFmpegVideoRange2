package com.example.ffmpegvideorange2.helper

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.widget.ImageView
import com.example.myplayer.KzgPlayer
import com.example.myplayer.PacketQueue
import com.example.myplayer.mediacodec.KzglVideoSupportUtil
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.helper.OnGetFrameBitmapCallback
import java.io.IOException
import java.nio.ByteBuffer


class IMediaCodecFrameHelper(
    override var filePath:String = "",
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable {

    private var kzgPlayer:KzgPlayer? = null
    private var mediaFormat:MediaFormat? = null
    private var mediaCodec:MediaCodec? = null
    private var videoDecodeInfo: MediaCodec.BufferInfo? = null
    val queue:PacketQueue = PacketQueue()

    private var lastIDRIndex: Int = 0
    var targetViewMap:MutableMap<ImageView, TargetBean> = mutableMapOf()
    private var childThread:Thread? = null
    private var isStop = false


    override fun init() {
        childThread = Thread()
        childThread!!.start()
    }

    override fun loadAvFrame(view: ImageView, timeMs: Long) {

        Log.e("kzg","**************seekTime0:${timeMs} , $view")
        targetViewMap[view] = targetViewMap[view]?:TargetBean()
        targetViewMap[view]!!.timeUs = timeMs
        targetViewMap[view]!!.isAddFrame = false
    }

    override fun release() {
        kzgPlayer?.let {
            it.release()
            kzgPlayer = null
        }

        mediaFormat = null
        mediaCodec?.let {
            it.release()
            mediaCodec = null
        }
    }

    override fun seek() {
        Log.e("kzg","****************开始seek")
        targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>
        run task@{
            targetViewMap.forEach {
                Log.e("kzg","**************targetViewMap:${it.value.timeUs} , ${it.value.isAddFrame}")
            }
        }

        targetViewMap.forEach {
            if (!it.value.isAddFrame) {
                if (IFrameSearch.IframeUs.size <= 1 || lastIDRIndex == IFrameSearch.IframeUs.size - 1){
                    Log.e("kzg","**************seekTo:${(it.value.timeUs/1000_000.0).toDouble()}  , true")
                    kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),true)
                }else{
                    if (it.value.timeUs >= IFrameSearch.IframeUs[lastIDRIndex] && it.value.timeUs < IFrameSearch.IframeUs[lastIDRIndex + 1]){
                        Log.e("kzg","**************seekTo:${(it.value.timeUs/1000_000.0).toDouble()}  , true")
                        kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),true)
                    }else{
                        Log.e("kzg","**************seekTo:${(it.value.timeUs/1000_000.0).toDouble()}  , false")
                        kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),false)
                    }

                }
                Log.e("kzg","**************seekTo:${it.value.timeUs}")
            }
        }
    }

    override fun pause() {

    }

    override fun run() {
        while (!isStop){
            if (queue.queueSize  == 0){
                Thread.sleep(10)
            }

            var hasFind = false
            val deQueue = queue.deQueue()
            run task@{
                targetViewMap.forEach {
                    if (it.value.timeUs.toDouble() == deQueue.pts){
                        mediacodecDecode(deQueue.data,deQueue.dataSize,deQueue.pts.toInt(),it.key)
                    }
                }
            }

        }
    }


    fun setKzgPlayer(player: KzgPlayer){
        this.kzgPlayer = player
    }

    fun initMediaCodec(codecName: String?,
                               width: Int,
                               height: Int,
                               csd_0: ByteArray?,
                               csd_1: ByteArray?){
        Log.e("kzg","*******************初始化获取预览帧的解码器：$codecName ,  $width  ,  $height")
        val mime = KzglVideoSupportUtil.findVideoCodecName(codecName)
        //创建MediaFormat
        //创建MediaFormat
        mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        //设置参数
        //设置参数
        mediaFormat!!.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height)
        mediaFormat!!.setByteBuffer("cds-0", ByteBuffer.wrap(csd_0))
        mediaFormat!!.setByteBuffer("cds-1", ByteBuffer.wrap(csd_1))
        Log.e("kzg", "**************mediaFormat:" + mediaFormat.toString())
        videoDecodeInfo = MediaCodec.BufferInfo()

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime)
            mediaCodec!!.configure(mediaFormat, null, null, 0)
            mediaCodec!!.start()
            kzgPlayer?.startGetFrame()
            kzgPlayer?.getFrameListener?.onStarGetFrame()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun mediacodecDecode(bytes: ByteArray?, size: Int, pts: Int,target:ImageView) {
        if (bytes != null && mediaCodec != null && videoDecodeInfo != null) {
            try {
                val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val byteBuffer = mediaCodec!!.inputBuffers[inputBufferIndex]
                    byteBuffer.clear()
                    byteBuffer.put(bytes)
                    mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, size, pts.toLong(), 0)
                }
                val index = mediaCodec!!.dequeueOutputBuffer(videoDecodeInfo, 10000)
                if (index >= 0) {
                    val buffer = mediaCodec!!.outputBuffers[index]
                    buffer.position(videoDecodeInfo!!.offset)
                    buffer.limit(videoDecodeInfo!!.offset + videoDecodeInfo!!.size)
                    mediaCodec!!.releaseOutputBuffer(index, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}