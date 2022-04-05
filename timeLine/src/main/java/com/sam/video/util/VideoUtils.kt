package com.sam.video.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

object VideoUtils {
    /**
     * 获取视频时长
     *
     * @param videoPath 视频地址
     * @return 视频时长 单位毫秒
     */
    fun getVideoDuration(context: Context, videoPath: String?): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(File(videoPath)))
        val duration =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                .toLong()
        retriever.release()
        return duration
    }
}