package com.example.ffmpegvideorange2.helper

import android.graphics.*
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import com.example.ffmpegvideorange2.TimeQueue
import com.example.ffmpegvideorange2.Utils
import com.example.myplayer.KzgPlayer
import com.example.myplayer.PacketQueue
import com.example.myplayer.mediacodec.KzglVideoSupportUtil
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.helper.OnGetFrameBitmapCallback
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.HashMap


class IMediaCodecFrameHelper(
    override var filePath:String = "",
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable {

    private var kzgPlayer:KzgPlayer? = null
    private var mediaFormat:MediaFormat? = null
    private var mediaCodec:MediaCodec? = null
    private var videoDecodeInfo: MediaCodec.BufferInfo? = null
    val packetQueue:PacketQueue = PacketQueue()
    val timeQueue:TimeQueue = TimeQueue()

    private var lastIDRIndex: Int = 0
    var targetViewMap:Hashtable<ImageView, TargetBean> = Hashtable()
    private var childThread:Thread? = null
    private var sendTargetTimeTask:Runnable? = null
    private var sendTargetTimeThread:Thread? = null
    private var isStop = false
    var pauseTimeQueue = false
    override var lastBitMap: Bitmap? = null


    override fun init() {
        childThread = Thread(this)
        childThread!!.start()

        //从队列取需要显示的帧的时间点
        /*sendTargetTimeTask = Runnable {
            var isNeedAddFrame = false
            while (!isStop){
                if (timeQueue.queueSize == 0){
                    Thread.sleep(10)
                    continue
                }
                if (pauseTimeQueue){
                    Thread.sleep(10)
                    continue
                }
                pauseTimeQueue = true
                val deQueue = timeQueue.deQueue()
                run task@{
                    targetViewMap.forEach {
                        if (it.value.timeUs == deQueue.timeUs && !it.value.isAddFrame){
                            isNeedAddFrame = true
                            sendFrame(it)
                            return@task
                        }
                    }
                }
                //如果没有找到需要抽的帧，就继续
                if (!isNeedAddFrame){
                    pauseTimeQueue = false
                }

            }
        }
        sendTargetTimeThread = Thread(sendTargetTimeTask)
        sendTargetTimeThread!!.start()*/

    }

    override fun loadAvFrame(view: ImageView, timeMs: Long) {
        targetViewMap[view] = TargetBean()
        Log.e("kzg","**************seekTime0:${timeMs} , $view , ${targetViewMap.size}")
        targetViewMap[view]?.timeUs = timeMs
        targetViewMap[view]?.isAddFrame = false
        kzgPlayer?.pauseGetPacket(false)

    }

    override fun release() {
        Log.e("kzg","*********************开始销毁IMediaCodecFrameHelper")
        isStop = true

        childThread?.join()

        kzgPlayer?.let {
            it.release()
            kzgPlayer = null
        }

        mediaFormat = null
        mediaCodec?.let {
            it.release()
            mediaCodec = null
        }

        targetViewMap.clear()

        packetQueue.clear()
        Log.e("kzg","*********************销毁IMediaCodecFrameHelper")
    }

    override fun seek() {
        /*Log.e("kzg","****************开始seek")
        targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>
        run task@{
            targetViewMap.forEach {
                Log.e("kzg","**************targetViewMap:${it.value.timeUs} , ${it.value.isAddFrame}")
            }
        }

        targetViewMap.forEach {
            sendFrame(it)
        }*/
    }

    private fun sendFrame(it:Map.Entry<ImageView, TargetBean>){
        if (!it.value.isAddFrame) {
            if (IFrameSearch.IframeUs.size <= 1 || lastIDRIndex == IFrameSearch.IframeUs.size - 1){
                Log.e("kzg","*************sendFrame:${(it.value.timeUs/1000_000.0).toDouble()}  , true")
                kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),true)
            }else{
                if (it.value.timeUs >= IFrameSearch.IframeUs[lastIDRIndex] && it.value.timeUs < IFrameSearch.IframeUs[lastIDRIndex + 1]){
                    Log.e("kzg","*************sendFrame:${(it.value.timeUs/1000_000.0).toDouble()}  , true")
                    kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),true)
                }else{
                    Log.e("kzg","**************sendFrame:${(it.value.timeUs/1000_000.0).toDouble()}  , false")
                    kzgPlayer?.seekFrame((it.value.timeUs/1000_000.0).toDouble(),false)
                }

            }
            Log.e("kzg","**************seekTo:${it.value.timeUs}")
        }
    }

    override fun pause() {

    }

    override fun run() {
        var times = 0
        var isPause = false
        var size = 0
        while (!isStop){
            if (packetQueue.queueSize  == 0){
                Thread.sleep(10)
                continue
            }

            /*if (times > 30){
                Thread.sleep(10)
                continue
            }*/

            var hasFind = false
            //按照时间从小到大排序
            //targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>

            /*if (targetViewMap.size != size){
                size = targetViewMap.size
                Log.e("kzg","**************targetViewMap.size:${targetViewMap.size}")
            }*/

            run task@{
                Utils.sortHashMap(targetViewMap).forEach {
                    if ((it.value.timeUs.toDouble() >= packetQueue.first.pts && !it.value.isAddFrame) || !it.value.isAddFrame){
                        if (packetQueue.queueSize < 20){
                            kzgPlayer?.pauseGetPacket(false)
                        }
                        packetQueue.deQueue().apply {
                            Log.e("kzg","**************packetQueue.size:${packetQueue.queueSize}")
                            Log.e("kzg","***************timeUs:${it.value.timeUs.toDouble()}  , pts:${this.pts}  ,isAddFrame:${it.value.isAddFrame}")
                            mediacodecDecode(this.data,this.dataSize,this.pts.toInt(),it)
                            times ++
                            return@task
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


    private fun mediacodecDecode(bytes: ByteArray?, size: Int, pts: Int,mapEntry:Map.Entry<ImageView, TargetBean>) {
        Log.e("kzg","************************mediacodec 开始解码帧：$pts  ,timeUs:${mapEntry.value.timeUs}")
        if (bytes != null && mediaCodec != null && videoDecodeInfo != null) {
            try {
                val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer: ByteBuffer? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaCodec!!.getInputBuffer(inputBufferIndex)
                    } else {
                        mediaCodec!!.inputBuffers[inputBufferIndex]
                    }
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(bytes)
                        mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, size, pts.toLong(), 0)
                    }
                }else{
                    Log.e("kzg","**************mediacodec dequeueInputBuffer 失败")
                }

                var index = mediaCodec!!.dequeueOutputBuffer(videoDecodeInfo, 10000)
                while (index >= 0) {
                    var buffer:ByteBuffer? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Log.e("kzg","**********************mediacodec 解码出一帧:${videoDecodeInfo!!.presentationTimeUs}")
                        buffer = mediaCodec!!.getOutputBuffer(index)
                        val image = mediaCodec!!.getOutputImage(index)
                        if (mapEntry.value.timeUs <= videoDecodeInfo!!.presentationTimeUs-30000 && mapEntry.value.timeUs<=videoDecodeInfo!!.presentationTimeUs+30000){
                            /*val fileName =   "${Environment.getExternalStorageDirectory()}/jpe/" + String.format("frame_%05d.jpg", it.value.timeUs)
                            KzgPlayer.compressToJpeg(fileName,image)*/
                            val rect = image.cropRect
                            val yuvImage = YuvImage(
                                KzgPlayer.getDataFromImage(image, KzgPlayer.COLOR_FormatNV21),
                                ImageFormat.NV21,
                                rect.width(),
                                rect.height(),
                                null
                            )
                            val stream = ByteArrayOutputStream()
                            yuvImage.compressToJpeg(Rect(0, 0, rect.width(), rect.height()), 80, stream)
                            // 检查bitmap的大小
                            val options = BitmapFactory.Options()
                            // 设置为true，BitmapFactory会解析图片的原始宽高信息，并不会加载图片
                            options.inJustDecodeBounds = true
                            var bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(),options)
                            //算出合适的缩放比例
                            options.inSampleSize = Utils.calculateInSampleSize(options,60,60)
                            Log.e("kzg","*************************inSampleSize:${options.inSampleSize}")
                            // 设置为false，加载bitmap
                            options.inJustDecodeBounds = false
                            bitmap =
                                BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(),options)
                            lastBitMap = bitmap
                            mapEntry.key.post {
                                bitmap?.let {
                                    Log.e("kzg","**********************展示一帧")
                                    mapEntry.key.setImageBitmap(it)

                                }
                            }

                            mapEntry.value.isAddFrame = true
                            pauseTimeQueue = false
                            Log.e("kzg","*******************mediacodec 解码出目标帧：${mapEntry.value.timeUs}")
                        }

                    }else{
                        buffer = mediaCodec!!.outputBuffers[index]
                    }
                    mediaCodec!!.releaseOutputBuffer(index, false)
                    buffer?.clear()
                    index = mediaCodec!!.dequeueOutputBuffer(videoDecodeInfo, 10)
                }
            } catch (e: Exception) {
                pauseTimeQueue = false
                e.printStackTrace()
            }finally {


            }
        }
    }
}