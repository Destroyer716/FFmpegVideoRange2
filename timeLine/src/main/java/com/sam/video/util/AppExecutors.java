package com.sam.video.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wyh3 on 2018/11/20.
 * 全局执行池，可简化&集中线程操作
 */
@SuppressLint("ThreadNameRequired ")
public class AppExecutors {

    private static class AppExecutorsHolder {
        private static final AppExecutors instance = new AppExecutors();
    }

    public static AppExecutors get() {
        return AppExecutorsHolder.instance;
    }

    private final MainThreadExecutor mMainExecutor;//主线程
    private final ExecutorService mDiskExecutor;//文件读写
    private final ExecutorService mNetworkExecutor;//网络请求
    private final ExecutorService mDbExecutor;//数据库读写
    private final ExecutorService mWorkExecutor;//其他耗时操作

    private AppExecutors() {
        this.mMainExecutor = new MainThreadExecutor();
        this.mDiskExecutor = Executors.newSingleThreadExecutor(new AppExecutorsThreadFactory("mtxx-disk-io"));
        this.mNetworkExecutor = Executors.newFixedThreadPool(5, new AppExecutorsThreadFactory("mtxx-network"));
        this.mDbExecutor = Executors.newFixedThreadPool(3, new AppExecutorsThreadFactory("mtxx-db"));
        this.mWorkExecutor = Executors.newCachedThreadPool(new AppExecutorsThreadFactory("mtxx-bg-work"));
    }

    /* ******** 直接执行 *********/

    /**
     * 主线程操作
     */
    public static void executeMain(Runnable runnable) {
        getMainExecutor().execute(runnable);
    }

    /**
     * 文件读写操作
     */
    public static void executeDisk(Runnable runnable) {
        getDiskExecutor().execute(runnable);
    }

    /**
     * 网络操作
     */
    public static void executeNetwork(Runnable runnable) {
        getNetworkExecutor().execute(runnable);
    }

    /**
     * 数据库操作
     */
    public static void executeDb(Runnable runnable) {
        getDbExecutor().execute(runnable);
    }

    /**
     * 其他耗时操作
     */
    public static void executeWork(Runnable runnable) {
        getWorkExecutor().execute(runnable);
    }


    /* ******** 获取线程池 *********/

    /**
     * 文件读写线程池
     */
    public static ExecutorService getDiskExecutor() {
        return get().mDiskExecutor;
    }

    /**
     * 网络操作线程池
     */
    public static ExecutorService getNetworkExecutor() {
        return get().mNetworkExecutor;
    }

    /**
     * 数据库操作线程池
     */
    public static ExecutorService getDbExecutor() {
        return get().mDbExecutor;
    }

    /**
     * 其他耗时操作线程池
     */
    public static ExecutorService getWorkExecutor() {
        return get().mWorkExecutor;
    }

    /**
     * 主线程
     */
    public static MainThreadExecutor getMainExecutor() {
        return get().mMainExecutor;
    }


    //主线程
    public static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }

        public Handler getMainThreadHandler() {
            return mainThreadHandler;
        }
    }

    //其他线程工厂
    private static class AppExecutorsThreadFactory implements ThreadFactory {

        private AtomicInteger count = new AtomicInteger(1);
        private String name;

        AppExecutorsThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "AppExecutors-" + name + "-Thread-" + count.getAndIncrement());
        }
    }
}
