package com.sam.video.timeline.helper

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.sam.video.util.*

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
class VideoAnalyze(val dataSource: String) {

    val mediaExtractor by lazy {
        MediaExtractor()
    }

    private val checkExtractor by lazy {
        MediaExtractor().apply {
            setDataSource(dataSource)
            val trackIndex = selectVideoTrack()
            selectTrack(trackIndex)
        }
    }

    val mediaFormat: MediaFormat

    val firstFrameTime by lazy {
        //得到第一个关键帧的时间点
        checkExtractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        checkExtractor.sampleTime
    }

    val lastFrameTime by lazy {
        //测试过，MediaExtractor video末端的sampleTime会大于duration，用这个方法来得到最后一帧的时间点
        checkExtractor.seekTo(checkExtractor.getTrackFormat(checkExtractor.selectVideoTrack()).duration,
                MediaExtractor.SEEK_TO_NEXT_SYNC)
        var l = mediaExtractor.sampleTime
        var end = false
        while (!end) {
            mediaExtractor.advance()
            val sampleTime = mediaExtractor.sampleTime
            if (sampleTime != -1L) {
                l = sampleTime
            } else {
                end = true
            }
        }
        l
    }

    init {
        mediaExtractor.setDataSource(dataSource)
        // 查看是否含有视频轨
        val trackIndex = mediaExtractor.selectVideoTrack()
        if (trackIndex < 0) {
            throw RuntimeException("this data source not video")
        }
        mediaExtractor.selectTrack(trackIndex)
        mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
    }

    /*
    * 计算总帧数
    * duration 是微秒，转换成秒再计算
    * */
    val totalFramesCount by lazy {
        (mediaFormat.duration.toFloat() / 1000000f * mediaFormat.fps).toInt()
    }

    
    /**
     * @param time: 毫秒
     * */
    fun getSafeTimeUs(time: Long): Long {
        return mediaFormat.getSafeTimeUS(time)
    }

    /*
    * 
    * 查找这个时间点对应的最接近的一帧。
    * 这一帧的时间点如果和目标时间相差不到 一帧间隔 就算相近
    * 
    * maxRange:查找范围
    * */
    fun getValidSampleTime(time: Long): Long {
        Log.d("getValidSampleTime", "time $time")
        checkExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        var sampleTime = checkExtractor.sampleTime
        val topTime = time + 2000000
        var isFind = false
        while (!isFind) {
            checkExtractor.advance()
            val s = checkExtractor.sampleTime
            Log.d("getValidSampleTime", "advance $s")
            if (s != -1L) {
                // 选取和目标时间差值最小的那个
                sampleTime = time.minDifferenceValue(sampleTime, s)
                isFind = s >= topTime
            } else {
                isFind = true
            }
        }
        Log.d("getValidSampleTime", "final time is  $sampleTime")
        return sampleTime
    }

    /*
    * time是一个精确的帧，所以使用closest_sync
    * */
    fun getLaterTime(time: Long, range: Int): ArrayList<Long> {
        checkExtractor.seekTo(time, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        checkExtractor.advance()
        val list = arrayListOf<Long>()
        if (checkExtractor.sampleTime == -1L) {
            return list
        }
        for (i in 0 until range) {
            val element = checkExtractor.sampleTime
            if (element == -1L) {
                return list
            }
            list.add(element)
            checkExtractor.advance()
        }
        return list
    }

    fun release() {
        mediaExtractor.release()
        checkExtractor.release()
    }

}