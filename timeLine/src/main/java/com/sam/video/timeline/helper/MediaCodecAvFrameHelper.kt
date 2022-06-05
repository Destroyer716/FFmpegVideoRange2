package com.sam.video.timeline.helper

import android.graphics.*
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Executors
import com.sam.video.timeline.bean.TargetBean
import com.sam.video.timeline.common.OutputImageFormat
import com.sam.video.util.FUtils
import com.sam.video.util.notNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.log

class MediaCodecAvFrameHelper(
    override var filePath: String,
    override var onGetFrameBitmapCallback: OnGetFrameBitmapCallback?
) : IAvFrameHelper,Runnable{

    private val COLOR_FormatI420 = 1
    private val COLOR_FormatNV21 = 2
    private val VERBOSE = false
    private val TAG = "VideoToFrames"
    private val DEFAULT_TIMEOUT_US: Long = 10_000

    private val decodeColorFormat = CodecCapabilities.COLOR_FormatYUV420Flexible
    private var childThread:Thread? = null
    private var extract:MediaExtractor? = null
    private var mSeekTime: Long = 0
    var targetViewMap:MutableMap<ImageView, TargetBean> = mutableMapOf()
    private var OUTPUT_DIR: String? = null

    private var stopDecode = false
    private var startDecode = false
    private var waitSeek = false
    override var isSeekBack: Boolean = false

    private var throwable:Throwable? = null
    override var decodeFrameListener: IAvFrameHelper.DecodeFrameListener? = null
    override var lastBitMap: Bitmap? = null
    override var isScrolling: Boolean = false

    override fun init(){
        OUTPUT_DIR = Environment.getExternalStorageDirectory().toString() + "/jpe/"
        val theDir: File = File(OUTPUT_DIR)
        if (!theDir.exists()) {
            theDir.mkdirs()
        }
        childThread = Thread(this,"avFrameDecode")
        childThread?.start()
        throwable?.let {
            throw it
        }
    }


    override fun loadAvFrame(view: ImageView, timeMs: Long) {
        if (timeMs == 0L){
            startDecode = true
        }
        Log.e("kzg","**************seekTime0:${timeMs} , $view")
        targetViewMap[view] = targetViewMap[view]?:TargetBean()
        targetViewMap[view]!!.timeUs = timeMs
        targetViewMap[view]!!.isAddFrame = false


        /*IFrameSearch.IframeUs?.let { iFrame ->
            var tempTimeus = timeMs
            var frontIFramUs = 0L
            waitSeek = true
            iFrame.forEach {
                if (it > tempTimeus && frontIFramUs < tempTimeus){
                    mSeekTime = frontIFramUs
                    Log.e("kzg","**************seekTime1:${mSeekTime}")
                    extract?.seekTo(frontIFramUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    waitSeek = false
                    return
                }
                frontIFramUs = it
            }
        }*/
        //extract?.seekTo(timeMs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    override fun release() {
        childThread?.join()
        childThread?.stop()
        childThread = null
        extract?.let {
            it.release()
            extract = null
        }

    }

    override fun seek() {
        Log.e("kzg","**************seekstart:${System.currentTimeMillis()}")
        targetViewMap = targetViewMap.entries.sortedBy { it.value.timeUs }.associateBy ({it.key},{it.value}) as MutableMap<ImageView, TargetBean>
        targetViewMap.forEach {
            Log.e("kzg","**************targetViewMap:${it.value.timeUs} , ${it.value.isAddFrame}")
        }
        targetViewMap.forEach {
            if (!it.value.isAddFrame) {
                extract?.seekTo(it.value.timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                Log.e("kzg","**************seekTo:${it.value.timeUs}")
                waitSeek = false
                return
            }
        }

    }

    override fun pause() {
        waitSeek = true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun run() {
        try {
            videoDecode()
        }catch (t:Throwable){
            throwable = t
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun videoDecode(){
        extract = null
        var decodec:MediaCodec? = null
        try {
            val file = File(filePath)
            extract = MediaExtractor()
            notNull(extract){
                extract!!.setDataSource(file.toString())
                val trackIndex = selectTrack(extract!!)
                if (trackIndex < 0){
                    Log.e(TAG,"未发现视频通道")
                }
                extract!!.selectTrack(trackIndex)
                val trackFormat = extract!!.getTrackFormat(trackIndex)
                val mine = trackFormat.getString(MediaFormat.KEY_MIME)
                decodec = MediaCodec.createDecoderByType(mine)
                decodec?.let {
                    showSupportedColorFormat(it.codecInfo.getCapabilitiesForType(mine))
                    if (isColorFormatSupported(decodeColorFormat,it.codecInfo.getCapabilitiesForType(mine))){
                        trackFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,decodeColorFormat)
                        Log.e(TAG, "set decode color format to type $decodeColorFormat")
                    }else{
                        Log.e(TAG,
                            "unable to set decode color format, color format type $decodeColorFormat not supported"
                        )
                    }
                    decodeFramesToImage(it,extract!!,trackFormat)
                }

            }

        }catch (e:Exception){
            e.printStackTrace()
        } finally {
            decodec?.let {
                it.stop()
                it.release()
                decodec = null
            }
            extract?.let {
                it.release()
                extract = null

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun decodeFramesToImage(decoder:MediaCodec, extractor: MediaExtractor, mediaFormat: MediaFormat){
        val info = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        var lastFrameIsShow = false
        var outputFrameCount = 0
        decoder.configure(mediaFormat,null,null,0)
        decoder.start()
        val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
        var hasNeedDecode = true
        var imageView:ImageView? = null
        var targetBean:TargetBean? = null
        var startItemTime = 0L
        var jumpTimes = 0;
        while (!sawOutputEOS && !stopDecode){
            if (waitSeek || !startDecode){
                Thread.sleep(10)
                continue
            }
            hasNeedDecode = false
            run outsite@{
                targetViewMap.forEach {
                    hasNeedDecode = !it.value.isAddFrame
                    if (hasNeedDecode){
                        mSeekTime = it.value.timeUs
                        imageView = it.key
                        targetBean = it.value
                        return@outsite
                    }
                }
            }

            if (!hasNeedDecode){
                Thread.sleep(10)
                continue
            }

            //Log.e("kzg","**************seekTime2:${seekTime}")
            var presentationTimeUs: Long = 0
            if (!sawInputEOS) {
                val inputBufferId =
                    decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        sawInputEOS = true
                        Log.e("kzg","**************sawInputEOS")
                    } else {
                        presentationTimeUs = extractor.sampleTime
                        //Log.e("kzg","**********************presentationTimeUs:"+presentationTimeUs);
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            0
                        )
                        extractor.advance()
                    }
                    mSeekTime = presentationTimeUs
                }
            }
            val outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US)
            if (outputBufferId >= 0) {
                if (outputFrameCount < 1) {
                    //判断首帧
                    mSeekTime = getValidSampleTime((outputFrameCount * 1_000_000).toLong(), extractor)
                    if (info.presentationTimeUs != mSeekTime) {
                        decoder.releaseOutputBuffer(outputBufferId, false)
                        continue
                    }
                }else{
                    if (targetBean?.timeUs?:0 >= (info.presentationTimeUs + 100_000)){
                        jumpTimes ++
                        decoder.releaseOutputBuffer(outputBufferId, false)
                        continue
                    }
                    Log.e("kzg","**************jumpTimes:$jumpTimes")
                    jumpTimes = 0
                }


                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                }
                val doRender = info.size != 0
                //Log.e("kzg","**************seekTime4:${info.presentationTimeUs}")
                if (doRender) {
                    Log.e("kzg","**************tag：${imageView?.tag}  , timeUs：${targetBean?.timeUs} ， presentationTimeUs：${info.presentationTimeUs}, seekEnd1:${System.currentTimeMillis() - startItemTime}")
                    outputFrameCount++
                    val image = decoder.getOutputImage(outputBufferId)
                    val yuvImage = YuvImage(YUV_420_888toNV21(image), ImageFormat.NV21, width, height, null)
                    val stream = ByteArrayOutputStream()
                    yuvImage.compressToJpeg(Rect(0, 0, width, height), 30, stream)
                    if (onGetFrameBitmapCallback != null) {

                    }
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(), options)
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    val sampleSize: Int = FUtils.calculateInSampleSize(options, 60, 80)
                    options.inSampleSize = sampleSize
                    options.inJustDecodeBounds = false
                    var bitmap =
                        BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
                    val frameAtTime2 = Bitmap.createScaledBitmap(bitmap, 60, 80, false)
                    targetBean?.isAddFrame = true
                    Executors.mainThreadExecutor().execute {
                        val endtime = System.currentTimeMillis()
                        Log.e("kzg","**************tag：${imageView?.tag}  , timeUs：${targetBean?.timeUs} ， presentationTimeUs：${info.presentationTimeUs}, seekEnd2:${endtime - startItemTime}")
                        startItemTime = endtime
                        Glide.with(imageView!!).load(frameAtTime2).into(imageView!!)
                    }
                    bitmap!!.recycle()
                    bitmap = null
                    //onGetFrameBitmapCallback?.onGetBitmap(frameAtTime2, info.presentationTimeUs)
                    lastFrameIsShow = true

                    /*val buffer = image.planes[0].buffer;
                    var arr = ByteArray(buffer.remaining())
                    buffer.get(arr);


                    if (OutputImageFormat.values()[2] != null) {
                        var fileName = ""
                        when(OutputImageFormat.values()[2]){
                            OutputImageFormat.I420 ->{
                                Log.e("kzg","**************I420")
                                fileName = OUTPUT_DIR + String.format("frame_%05d_I420_%dx%d.yuv", outputFrameCount, width, height);
                                dumpFile(fileName, getDataFromImage(image, COLOR_FormatI420));
                            }

                            OutputImageFormat.NV21 ->{
                                Log.e("kzg","**************NV21")
                                fileName = OUTPUT_DIR + String.format("frame_%05d_NV21_%dx%d.yuv", outputFrameCount, width, height);
                                dumpFile(fileName, getDataFromImage(image, COLOR_FormatNV21));
                            }

                            OutputImageFormat.JPEG ->{
                                Log.e("kzg","**************JPEG")
                                fileName = OUTPUT_DIR + String.format("frame_%05d.jpg", outputFrameCount);
                                compressToJpeg(fileName, image);
                            }
                        }
                    }*/

                    stream.close()
                    image.close()
                    decoder.releaseOutputBuffer(outputBufferId, false)
                }
            }
        }
    }

    fun getValidSampleTime(time: Long, extractor: MediaExtractor): Long {
        extractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var sampleTime = extractor.sampleTime
        val topTime = time + 2000000
        var isFind = false
        while (!isFind) {
            extractor.advance()
            val s = extractor.sampleTime
            Log.d("getValidSampleTime", "advance \$s")
            if (s != -1L) {
                // 选取和目标时间差值最小的那个
                sampleTime = FUtils.minDifferenceValue(sampleTime, s, time)
                isFind = s >= topTime
            } else {
                isFind = true
            }
        }
        Log.d("getValidSampleTime", "final time is  \$sampleTime")
        return sampleTime
    }

    private fun selectTrack(extractor: MediaExtractor): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track $i ($mime): $format")
                }
                return i
            }
        }
        return -1
    }

    private fun showSupportedColorFormat(caps: CodecCapabilities) {
        print("supported color format: ")
        for (c in caps.colorFormats) {
            print(c.toString() + "\t")
        }
        println()
    }

    private fun isColorFormatSupported(colorFormat: Int, caps: CodecCapabilities): Boolean {
        for (c in caps.colorFormats) {
            if (c == colorFormat) {
                return true
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun YUV_420_888toNV21(image: Image): ByteArray? {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        if (VERBOSE) Log.v(
            "YUV_420_888toNV21",
            "get data from " + planes.size + " planes"
        )
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            if (VERBOSE) {
                Log.v("YUV_420_888toNV21", "pixelStride $pixelStride")
                Log.v("YUV_420_888toNV21", "rowStride $rowStride")
                Log.v("YUV_420_888toNV21", "width $width")
                Log.v("YUV_420_888toNV21", "height $height")
                Log.v("YUV_420_888toNV21", "buffer size " + buffer.remaining())
            }
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            if (VERBOSE) Log.v(
                "",
                "Finished reading data from plane $i"
            )
        }
        return data
    }


    private fun saveBitMap(){

    }

    private fun dumpFile(fileName: String, data: ByteArray) {
        val outStream: FileOutputStream
        outStream = try {
            FileOutputStream(fileName)
        } catch (ioe: IOException) {
            throw RuntimeException("Unable to create output file $fileName", ioe)
        }
        try {
            outStream.write(data)
            outStream.close()
        } catch (ioe: IOException) {
            throw RuntimeException("failed writing data to file $fileName", ioe)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun compressToJpeg(fileName: String, image: Image) {
        val outStream: FileOutputStream
        outStream = try {
            FileOutputStream(fileName)
        } catch (ioe: IOException) {
            throw RuntimeException("Unable to create output file $fileName", ioe)
        }
        val rect = image.cropRect
        val yuvImage = YuvImage(
            getDataFromImage(
                image,
                COLOR_FormatNV21
            ), ImageFormat.NV21, rect.width(), rect.height(), null
        )
        yuvImage.compressToJpeg(rect, 100, outStream)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
        if (!isImageFormatSupported(image)) {
            throw java.lang.RuntimeException("can't convert Image to byte array, format " + image.format)
        }
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        if (VERBOSE) Log.v(
            TAG,
            "get data from " + planes.size + " planes"
        )
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            if (VERBOSE) {
                Log.v(
                    TAG,
                    "pixelStride $pixelStride"
                )
                Log.v(
                    TAG,
                    "rowStride $rowStride"
                )
                Log.v(
                    TAG,
                    "width $width"
                )
                Log.v(
                    TAG,
                    "height $height"
                )
                Log.v(
                    TAG,
                    "buffer size " + buffer.remaining()
                )
            }
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            if (VERBOSE) Log.v(
                TAG,
                "Finished reading data from plane $i"
            )
        }
        return data
    }

    private fun isImageFormatSupported(image: Image): Boolean {
        val format = image.format
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
        }
        return false
    }
}