package com.sam.video

import android.app.Application
import android.content.Context
import android.util.Log


class TimeLineApp : Application() {

    override fun onCreate() {
        Log.e("kzg","******************TimeLineApp")
        instance = this
        super.onCreate()
    }

    override fun attachBaseContext(base: Context?) {
        Log.e("kzg","******************attachBaseContext")
        super.attachBaseContext(base)
        instance = this
    }

    companion object {
        lateinit var instance: TimeLineApp
            private set

    }
}