package com.sam.video.timeline.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.sam.video.timeline.cache.DiskLruCache
import com.sam.video.util.fillOutputStream
import com.sam.video.util.hashKeyForDisk
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 *@version:
 *@FileDescription: 磁盘缓存协助类
 *@Author:Jing
 *@Since:2019/3/1
 *@ChangeList:
 */

/**
 *
 * maxSize:max MB
 * */
class DiskCacheAssist(dir: String, appVersion: Int, valueCount: Int = 1, maxSize: Int) {

    private val TAG = "DiskCacheAssist"

    private val diskLruCache by lazy {
        DiskLruCache.open(File(dir), appVersion, valueCount, (maxSize * 1024 * 1024).toLong())
    }

    private val writerExecutors by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val queueWriteTask by lazy {
        CopyOnWriteArrayList<Int>()
    }

    /*
    *  key :缓存的文件名
    *  
    * 存Disk缓存在异步线程执行
    * */
    fun writeBitmap(key: String, b: Bitmap, success: () -> Unit = {},
                    failed: (Exception) -> Unit = {}) {
        //正在写入就不重复执行
        val code = key.hashCode()
        if (queueWriteTask.contains(code)) {
            return
        }
        //对key进行hash化
        val cacheKey = key.hashKeyForDisk()
        //查看LruDiskCache是否有这个缓存，这一步基本不消耗时间
        val snapshot = diskLruCache.get(cacheKey)
        if (snapshot != null) {
            success.invoke()
            return
        }

        queueWriteTask.add(code)
        //在一个子线程执行写入磁盘缓存的任务队列
        writerExecutors.execute(WriteR(cacheKey, b, {
            queueWriteTask.remove(code)
            success.invoke()
        }, {
            queueWriteTask.remove(code)
            failed.invoke(it)
        }, {
            //写入完成时，检查线程池是否关闭，然后关闭DiskLruCache
            if (writerExecutors.isShutdown) {
                diskLruCache.close()
            } else {
                if (queueWriteTask.size == 0) {
                    //如果队列任务做完了，就调用flush方法同步信息
                    val c = measureTimeMillis {
                        diskLruCache.flush()
                    }
                    Log.d(TAG, "flush cost $c ")
                }
            }
        }))
    }

    //----read---------
    private val readerExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }
    private val compositeDisposable by lazy {
        CompositeDisposable()
    }

    //异步读取磁盘缓存
    fun asyncReadBitmap(
        key: String, timeMs: Long, success: (Bitmap, Long) -> Unit = { bt, us ->},
        failed: (Exception) -> Unit = {}) {
        var disposable: Disposable? = null
        disposable = Single.fromCallable {
            Log.d(TAG, "read key $key thread ${Thread.currentThread().name}: ")
            blockingReadBitmap(key) ?: throw NullPointerException("null bitmap")
        }.subscribeOn(Schedulers.from(readerExecutor))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    success.invoke(it,timeMs)
                    disposable?.dispose()
                }, {
                    Log.d(TAG, "async load disk failed $it: ")
                    failed.invoke(Exception(it))
                    disposable?.dispose()
                })
        compositeDisposable.add(disposable)
    }

    //同步得到缓存
    fun blockingReadBitmap(key: String): Bitmap? {
        val cacheKey = key.hashKeyForDisk()
        diskLruCache.get(cacheKey)?.apply {
            val start = System.currentTimeMillis()
            val inputStream = this.getInputStream(0)
            val b = BitmapFactory.decodeStream(inputStream)
            Log.d(TAG, "read bitmap cost ${System.currentTimeMillis() - start}")
            return b
        }
        return null
    }

    fun release() {
        writerExecutors.shutdownNow()
        compositeDisposable.clear()
    }

    private inner class WriteR(private val key: String, private val b: Bitmap,
                               private val success: () -> Unit,
                               private val failed: (Exception) -> Unit,
                               private val complete: () -> Unit = {}) : Runnable {

        override fun run() {
            try {
                val editor = diskLruCache.edit(key)
                editor?.apply {

                    Log.d(TAG, "writer disk cache ${Thread.currentThread().name}")
                    // 将bitmap写入缓存
                    val fillSucceed = b.fillOutputStream(newOutputStream(0))
                    if (fillSucceed) {
                        editor.commit()
                    } else {
                        editor.abort()
                        failed.invoke(Exception("bitmap fill outputStream failed"))
                        complete.invoke()
                        return
                    }
                    success.invoke()
                    complete.invoke()
                    return
                }
                failed.invoke(Exception("edit key is null"))
                complete.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                failed.invoke(e)
                complete.invoke()
            }
        }
    }
}