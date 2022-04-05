package com.sam.video.timeline.helper

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.sam.video.util.dequeueValidInputBuffer
import com.sam.video.util.disposeOutput
import com.sam.video.util.isSupportColorFormat
import com.sam.video.util.mime
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 *@version:
 *@FileDescription: 解码指定帧
 *@Author:jing
 *@Since:2019/2/18
 *@ChangeList:
 */
class VideoDecoder2(private val dataSource: String) {

    private val TAG = this.javaClass.name

    private val videoAnalyze by lazy {
        VideoAnalyze(dataSource)
    }

    private val DEF_TIME_OUT = 2000L
    private val defDecoderColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    private val frameCache by lazy {
        Log.e("kzg","******************frameCache init")
        FrameCache(dataSource)
    }
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    private val thread by lazy {
        Executors.newSingleThreadExecutor()
    }

    private var curFrame = 0L
    private var isStarted = false

    private val decoder: MediaCodec
    private val decodeCore by lazy {
        //GLCore()
    }
    private val info by lazy {
        MediaCodec.BufferInfo()
    }

    init {
        val mediaFormat = videoAnalyze.mediaFormat
        val mime = mediaFormat.mime
        decoder = MediaCodec.createDecoderByType(mime)
        // 指定帧格式COLOR_FormatYUV420Flexible,几乎所有的解码器都支持
        if (decoder.codecInfo.getCapabilitiesForType(mediaFormat.mime)
                        .isSupportColorFormat(defDecoderColorFormat)) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, defDecoderColorFormat)
        } else {
            throw RuntimeException("this mobile not support YUV 420 Color Format")
        }
    }

    /**
     * 得到指定时间的帧，秒级别，异步回调
     *
     * @param second 秒
     *
     * @param success 成功回调
     *
     * @param failed 失败回调
     */
    fun getFrame(second: Float, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit) {
        getFrameMs((second * 1000).toLong(), success, failed)
    }

    /**
     * 得到指定时间的帧，ms,毫秒级别，异步回调
     *
     * @param second ms
     *
     * @param success 成功回调
     *
     * @param failed 失败回调
     */
    fun getFrameMs(ms: Long, success: (Bitmap) -> Unit, failed: (Throwable) -> Unit) {
        val target = videoAnalyze.getValidSampleTime(videoAnalyze.getSafeTimeUs(ms))
        Log.d(TAG, "getFrame ms $ms sampleTime $target: ")
        getInternalFrame(target, success, failed)
    }

    private fun getInternalFrame(target: Long, success: (Bitmap) -> Unit,
                                 failed: (Throwable) -> Unit) {
        // 是否需要缓存，不需要的话就直接解码。需要缓存则优先从缓存中读取
        frameCache.asyncGetTarget(target, true, { time, bitmap ->
            success.invoke(bitmap)
        }, {
            // 如果此时正在取这一帧，就不作任何处理
            val isSameFrame = curFrame != 0L && curFrame == target
            if (!isSameFrame) {
                curFrame = target
                val disposable = Single.fromCallable(DecoderCallable(target))
                        .subscribeOn(Schedulers.from(thread))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            it?.apply {
                                frameCache.cacheFrame(time, bitmap!!)
                                success.invoke(bitmap)
                            }
                        }, {
                            failed.invoke(it)
                        })
                compositeDisposable.add(disposable)
            }
        })

    }

    private fun prepareCodec() {
        if (!isStarted) {
            isStarted = true
            decoder.configure(videoAnalyze.mediaFormat,
                    null,
                    null, 0)
            decoder.start()
        }
    }

    fun release() {
        //decodeCore.release()
        decoder.release()

        videoAnalyze.release()
        frameCache.release()
        compositeDisposable.clear()
    }

    class Info(val time: Long, val bitmap: Bitmap?)

    inner class DecoderCallable(private val target: Long) : Callable<Info?> {

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun call(): Info? {
            Log.d(TAG, "decoder2 thread is ${Thread.currentThread().name}")
            val s = System.currentTimeMillis()
            prepareCodec()

            var b: Bitmap? = null
            var outputDone = false
            var inputDone = false
            videoAnalyze.mediaExtractor.seekTo(target, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            Log.d(TAG, "seek to cost :${System.currentTimeMillis() - s} ")
            while (!outputDone) {
                if (!inputDone) {
                    val (inputBufferId, inputBuffer1) = decoder.dequeueValidInputBuffer(DEF_TIME_OUT)
                    inputBuffer1?.apply {
                        val sampleSize = videoAnalyze.mediaExtractor.readSampleData(this, 0)
                        if (sampleSize >= 0) {
                            // 将数据压入到输入队列
                            val presentationTimeUs = videoAnalyze.mediaExtractor.sampleTime
                            decoder.queueInputBuffer(inputBufferId, 0,
                                    sampleSize, presentationTimeUs, 0)
                            videoAnalyze.mediaExtractor.advance()
                        } else {
                            inputDone = true
                            decoder.queueInputBuffer(inputBufferId, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        }
                    }
                }
                decoder.disposeOutput(info, DEF_TIME_OUT, {
                    outputDone = true
                    inputDone = true
                }) { id ->
                    Log.d(TAG, "out time ${info.presentationTimeUs} ")
                    if (info.presentationTimeUs == target) {
                        // 遇到目标时间帧，才生产Bitmap
                        outputDone = true
                        inputDone = true
                        Log.d(TAG, "decode target $target cost ${System.currentTimeMillis() - s} ")
                    }
                }
            }
            return Info(target, b)
        }
    }
}