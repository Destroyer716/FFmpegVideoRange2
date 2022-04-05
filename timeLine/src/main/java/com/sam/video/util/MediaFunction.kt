package com.sam.video.util

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/12
 *@ChangeList:
 */
val MediaFormat.width
    get() = getInteger(MediaFormat.KEY_WIDTH)

val MediaFormat.height
    get() = getInteger(MediaFormat.KEY_HEIGHT)

val MediaFormat.size
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    get() = Size(width, height)

val MediaFormat.duration
    get() = getLong(MediaFormat.KEY_DURATION)

//时长：秒
val MediaFormat.durationSecond
    get() = (getLong(MediaFormat.KEY_DURATION) / 1000000L).toInt()

// 每秒传输帧数
val MediaFormat.fps: Int
    get() = try {
        getInteger(MediaFormat.KEY_FRAME_RATE)
    } catch (e: Exception) {
        0
    }

/**
 * 每一帧时间，微秒
 * */
val MediaFormat.perFrameTime: Long
    get() {
        return 1000000L / this.fps
    }

/**
 * AAC 每秒帧数计算
 * AAC的Frame_size为1024，每帧一个包的话，每个RTP时间差为1024
 * */
val MediaFormat.aacFPS: Int
    get() = try {
        sampleRate / 1024
    } catch (e: Exception) {
        0
    }

val MediaFormat.sampleRate: Int
    get() = try {
        getInteger(MediaFormat.KEY_SAMPLE_RATE)
    } catch (e: Exception) {
        0
    }

/**
 * AAC 每一帧时间，微秒
 * */
val MediaFormat.aacPerFrameTime: Long
    get() = 1000000L / aacFPS

val MediaFormat.mime
    get() = getString(MediaFormat.KEY_MIME)

/*
* 选择视频轨
* */
fun MediaExtractor.selectVideoTrack(): Int {
    val numTracks = trackCount
    for (i in 0 until numTracks) {
        val format = getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (mime.startsWith("video/")) {
            return i
        }
    }
    return -1
}

/*
* 防止时间越界
* */
fun MediaFormat.getSafeTimeUS(second: Float): Long {
    return when {
        second < 0L -> 0L
        second * 1000000 > duration -> duration
        else -> (second * 1000000).toLong()
    }
}

fun MediaFormat.getSafeTimeUS(ms: Long): Long {
    return when {
        ms < 0L -> 0L
        ms * 1000 > duration -> duration
        else -> ms * 1000
    }
}

/**
 * AAC 数据增加ADT头信息
 * */
fun ByteArray.addADTS(packetLen: Int) {
    val profile = 2 // AAC LC
    val freqIdx = 4 // 44.1KHz
    val chanCfg = 2// CPE
    this[0] = 0xFF.toByte()
    this[1] = 0xF9.toByte()
    this[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
    this[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
    this[4] = (packetLen and 0x7FF shr 3).toByte()
    this[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
    this[6] = 0xFC.toByte()
}
