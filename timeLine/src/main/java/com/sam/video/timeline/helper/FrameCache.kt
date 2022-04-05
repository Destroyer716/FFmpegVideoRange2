package com.sam.video.timeline.helper

import android.graphics.Bitmap
import android.os.Environment
import androidx.collection.LruCache
import android.text.TextUtils
import android.util.Log
import com.sam.video.util.md5

/**
 *@version:
 *@FileDescription: 帧的缓存管理类；内存缓存和磁盘缓存
 *@Author:jing
 *@Since:2019/2/20
 *@ChangeList:
 */
class FrameCache(dataSource: String) {
    private val TAG = "FrameCache"
    private val mainKey = md5(dataSource)
    private val lruCache by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory()).toInt()

        // Use 1/8th of the available memory for this memory cache.
        val cacheSize = maxMemory / 12
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                value?.apply {
                    return byteCount
                }
                return super.sizeOf(key, value)
            }
        }
    }

    private val diskCache by lazy {
        DiskCacheAssist(TextUtils.concat(
                Environment.getExternalStorageDirectory().path,
                "/frameCache/", mainKey).toString(), 1, 1, 121)
    }

    //获取目标帧缓存
    fun cacheFrame(target: Long, b: Bitmap) {
        val b1 = Bitmap.createBitmap(b)
        cacheLru(target, b1)
        cacheDisk(target, b1)
    }

    private fun cacheLru(target: Long, b: Bitmap) {
        lruCache.put(getLruKey(target), b)
    }

    private fun cacheDisk(target: Long, b: Bitmap) {
//        diskCache.writeBitmap(getDiskKey(target), b)
    }

    fun asyncGetTarget(target: Long, isNeedCache: Boolean = true, success: (time: Long, Bitmap) -> Unit, failed: (Throwable) -> Unit) {
        if (!isNeedCache) {
            failed.invoke(Throwable("function set not need cache"))
            return
        }
        val lruBitmap = getLruBitmap(target)
        if (lruBitmap != null) {
            Log.d(TAG, "get cache from lru $target ")
            success.invoke(target, lruBitmap)
        } else {
            failed.invoke(Throwable("has not lru"))
        }
        //异步读取Disk缓存，Disk读取Bitmap的success是在子线程执行的，所以回调主线程
//        diskCache.asyncReadBitmap(getDiskKey(target), {
//            Log.d(TAG, "get disk cache ${getDiskKey(target)} ")
//            success.invoke(target, it)
//        }, failed)
    }

    fun getLruBitmap(target: Long): Bitmap? {
        return lruCache.get(getLruKey(target))
    }

    private fun getLruKey(target: Long) = TextUtils.concat(mainKey, target.toString()).toString()

    private fun getDiskKey(target: Long) = TextUtils.concat(
            Environment.getExternalStorageDirectory().path,
            "/frameCache/", mainKey, "/$target.jpg").toString()

    fun release() {
        lruCache.trimToSize(0)
        diskCache.release()
    }
}