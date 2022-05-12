package com.example.ffmpegvideorange2.helper

import android.graphics.*
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.widget.ImageView
import com.example.ffmpegvideorange2.TimeQueue
import com.example.ffmpegvideorange2.Utils
import com.example.ffmpegvideorange2.VideoUtils
import com.example.myplayer.KzgPlayer
import com.example.myplayer.PacketQueue
import com.example.myplayer.mediacodec.KzglVideoSupportUtil
import com.example.myplayer.mediacodecframes.VideoToFrames
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.IFrameSearch
import com.sam.video.timeline.helper.OnGetFrameBitmapCallback
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*


class IMediaCodecFrameHelper(
    override var filePath:String = "",
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable {

    private var kzgPlayer:KzgPlayer? = null
    private var mediaFormat:MediaFormat? = null
    private var mediaCodec:MediaCodec? = null
    private var videoDecodeInfo: MediaCodec.BufferInfo? = null
    //存放从ffmpeg传过来的AVPacket数据
    val packetQueue:PacketQueue = PacketQueue()

    private var lastIDRIndex: Int = 0
    var targetViewMap:Hashtable<ImageView, TargetBean> = Hashtable()
    private var childThread:Thread? = null
    //是否停止解码线程
    private var isStop = false
    //当前最后解码出来的一个帧，用来作为还没有来得及解码的预览帧
    override var lastBitMap: Bitmap? = null
    override var decodeFrameListener: IAvFrameHelper.DecodeFrameListener? = null
    //是初始化了recyclerView的Item
    private var isInitItem = false
    private var lastCodecFramePts = 0L


    override fun init() {
        childThread = Thread(this)
        childThread!!.start()


    }

    override fun loadAvFrame(view: ImageView, timeMs: Long) {
        targetViewMap[view] = TargetBean()
        Log.e("kzg","**************seekTime0:${timeMs} , $view , ${targetViewMap.size}")
        targetViewMap[view]?.timeUs = timeMs
        targetViewMap[view]?.isAddFrame = false
        if (!isInitItem) {
            isInitItem = true
            kzgPlayer?.pauseGetPacket(false)
        }


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
        Log.e("kzg","****************开始seek")
        packetQueue.clear()
        Utils.sortHashMap(targetViewMap).apply {
            var i=0
            var j=0
            val func =  {
                Log.e("kzg","********************isCurrentGop11111111:${this[0].value.timeUs}")
                for((index,frame) in IFrameSearch.IframeUs.withIndex()){
                    //当前recyclerView最小的item帧的时间戳所属的gop index
                    if (index > 0 && this[0].value.timeUs >=IFrameSearch.IframeUs[index - 1] && this[0].value.timeUs < frame){
                        i = index
                    }
                    //已解码的帧的pts所属的gop
                    if (index > 0 && lastCodecFramePts >=IFrameSearch.IframeUs[index - 1] && lastCodecFramePts < frame ){
                        j = index
                    }
                }
                i == j

            }
            val isCurrentGop =func().apply {
                Log.e("kzg","********************isCurrentGop:$this")
            }
            //如果还在一个gop内，就取需要显示的帧的时间（这种情况其实不需要用到这个），如果不在同一个gop,就取要显示的的帧的pts所在的gop
            val pts = (if (isCurrentGop) this[0].value.timeUs/1000_000.0 else IFrameSearch.IframeUs[i-1]/1000_000.0).apply {
                Log.e("kzg","********************pts:$this")
            }
            kzgPlayer?.seekFrame(pts.toDouble(),isCurrentGop)
        }
        //targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>
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
        Log.e("kzg","**************pause:")
        kzgPlayer?.pauseGetPacket(true)
    }

    override fun run() {
        var times = 0
        var isPause = false
        var size = 0
        while (!isStop){
            if (packetQueue.queueSize != size){
                size = packetQueue.queueSize
                Log.e("kzg","*******************queueSize:$size")
            }
            if (packetQueue.queueSize  == 0){
                Thread.sleep(10)
                continue
            }


            var hasFind = false
            //按照时间从小到大排序
            //targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>

            /*if (targetViewMap.size != size){
                size = targetViewMap.size
                Log.e("kzg","**************targetViewMap.size:${targetViewMap.size}")
            }*/

            run task@{
                Utils.sortHashMap(targetViewMap).forEach {
                    packetQueue.first?.let {bean ->

                        if ((it.value.timeUs.toDouble() >= bean.pts && !it.value.isAddFrame) || !it.value.isAddFrame){
                            if (packetQueue.queueSize < 20){
                                kzgPlayer?.pauseGetPacket(false)
                            }
                            packetQueue.deQueue().apply {
                                Log.e("kzg","***************timeUs:${it.value.timeUs.toDouble()}  , pts:${this.pts}  ,isAddFrame:${it.value.isAddFrame}")
                                mediacodecDecode(this.data,this.dataSize,this.pts.toInt(),it)
                                times ++
                                return@task
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

    fun initMediaCodec(codecName: String?,
                               width: Int,
                               height: Int,
                               csd_0: ByteArray?,
                               csd_1: ByteArray?,
                    surface: Surface
    ){
        Log.e("kzg","*******************初始化获取预览帧的解码器：$codecName ,  $width  ,  $height")
        val mime = KzglVideoSupportUtil.findVideoCodecName(codecName)
        //创建MediaFormat
        mediaFormat = MediaFormat.createVideoFormat(mime, width, height)
        //设置参数
        mediaFormat!!.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height)
        mediaFormat!!.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
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


    var startTime = 0L
    private fun mediacodecDecode(bytes: ByteArray?, size: Int, pts: Int,mapEntry:Map.Entry<ImageView, TargetBean>) {
        Log.e("kzg","************************mediacodec 开始解码帧：$pts  ,timeUs:${mapEntry.value.timeUs}")
        if (startTime == 0L){
            startTime = System.currentTimeMillis()
        }

        if (bytes != null && mediaCodec != null && videoDecodeInfo != null) {
            try {
                val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(10)
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

                var index = mediaCodec!!.dequeueOutputBuffer(videoDecodeInfo, 3000)
                while (index >= 0) {
                    var buffer:ByteBuffer? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        lastCodecFramePts = videoDecodeInfo!!.presentationTimeUs
                        Log.e("kzg","**********************mediacodec 解码出一帧:${videoDecodeInfo!!.presentationTimeUs}")
                        //buffer = mediaCodec!!.getOutputBuffer(index)
                        val image = mediaCodec!!.getOutputImage(index)
                        // TODO 这里需要优化，将具体需要放宽的时间范围，根据帧率来计算，比如这里的40_000 和 60_000，需要根据实际帧率来算每帧间隔实际
                        if (((mapEntry.value.timeUs >= videoDecodeInfo!!.presentationTimeUs-20_000 && mapEntry.value.timeUs<=videoDecodeInfo!!.presentationTimeUs+20_000)
                            || videoDecodeInfo!!.presentationTimeUs-mapEntry.value.timeUs>=30_000  ||(mapEntry.value.timeUs < 30_000 && videoDecodeInfo!!.presentationTimeUs > mapEntry.value.timeUs))
                            && !mapEntry.value.isAddFrame){
                            val rect = image.cropRect
                            val yuvImage = YuvImage(
                                VideoUtils.YUV_420_888toNV21(image),
                                ImageFormat.NV21,
                                rect.width(),
                                rect.height(),
                                null
                            )
                            val stream = ByteArrayOutputStream()
                            yuvImage.compressToJpeg(Rect(0, 0, rect.width(), rect.height()), 100, stream)
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
                                    mapEntry.key.setImageBitmap(it)
                                    //decodeFrameListener?.onGetOneFrame()
                                }
                            }
                            //Utils.saveBitmap("${Environment.getExternalStorageDirectory()}/jpe/",String.format("frame_%05d.jpg", mapEntry.value.timeUs),bitmap)
                            mapEntry.value.isAddFrame = true
                            Log.e("kzg","**********************展示一帧 timeUs: ${mapEntry.value.timeUs} ,pts:${videoDecodeInfo!!.presentationTimeUs}  ,耗时：${System.currentTimeMillis() - startTime}")
                            startTime = System.currentTimeMillis()
                        }
                        image.close()
                    }else{
                        buffer = mediaCodec!!.outputBuffers[index]
                    }
                    mediaCodec!!.releaseOutputBuffer(index, false)
                    buffer?.clear()
                    index = mediaCodec!!.dequeueOutputBuffer(videoDecodeInfo, 10)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }finally {


            }
        }
    }



}