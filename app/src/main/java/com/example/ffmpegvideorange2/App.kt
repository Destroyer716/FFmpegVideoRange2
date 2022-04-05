package com.example.ffmpegvideorange2

import android.app.Application
import android.content.Context
import android.util.Log
import com.sam.video.TimeLineApp
import java.lang.reflect.Method


class App : Application() {

    private var moduleApplication: TimeLineApp? = null

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        lateinit var instance: App
            private set

    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        moduleApplication = getModuleApplicationInstance(this)
        try {
            //通过反射调用moduleApplication的attach方法
            val method: Method? =
                Application::class.java.getDeclaredMethod("attach", Context::class.java)
            Log.e("kzg","******************attachBaseContext")
            if (method != null) {
                method.setAccessible(true)
                method.invoke(moduleApplication, baseContext)
                Log.e("kzg","******************通过反射调用moduleApplication的attach方法")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //映射获取ModuleApplication
    private fun getModuleApplicationInstance(paramContext: Context): TimeLineApp? {
        try {
            if (moduleApplication == null) {
                val classLoader: ClassLoader = paramContext.getClassLoader()
                if (classLoader != null) {
                    val mClass = classLoader.loadClass(TimeLineApp::class.java.getName())
                    if (mClass != null) moduleApplication =
                        mClass.newInstance() as TimeLineApp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return moduleApplication
    }
}