package com.sam.video.timeline.listener

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.RecyclerView

/**
 * 帧列表点击事件
 * 调用 {Recycler.addOnItemTouchListener}
 */
abstract class OnFrameClickListener(
    private val recyclerView: RecyclerView
) : RecyclerView.SimpleOnItemTouchListener() {
    private val context: Context = recyclerView.context

    val gestureDetector: GestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        val detector = GestureDetector(context, gestureListener)
        detector.setIsLongpressEnabled(false)
        detector
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        //这个方法一直不会被调用。。。。所以需要在外面重置longClickEnable
//        Log.d("Sam", " $e ")
        if (e.action == MotionEvent.ACTION_DOWN && e.pointerCount == 1) {
            gestureDetector.setIsLongpressEnabled(true)
        }
        gestureDetector.onTouchEvent(e)
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//        Log.d("Sam", " $e ")
        if (e.action == MotionEvent.ACTION_DOWN && e.pointerCount == 1) {
            gestureDetector.setIsLongpressEnabled(true)
        }
        gestureDetector.onTouchEvent(e)
        return false
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                return false
            }
            onClick(e)
            return false
        }

        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
//            Log.d("Sam", " onLongPress ${gestureDetector.isLongpressEnabled} ")
            if (!gestureDetector.isLongpressEnabled || recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                return
            }
            e?.let {
                onLongClick(e)
            }
        }

    }


    open fun onLongClick(e: MotionEvent): Boolean {
        return false
    }

    /**
     * item 点击
     * @param v 点击的view
     * @param position 点击的位置
     */
    abstract fun onClick(e: MotionEvent): Boolean

    abstract fun onScale(detector: ScaleGestureDetector): Boolean

}