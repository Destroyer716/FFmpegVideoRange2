package com.sam.video.timeline.helper

import android.graphics.Bitmap

interface OnGetFrameBitmapCallback {

    fun onGetBitmap(bitmap: Bitmap?, usTime: Long)
    fun onCodecStart()
}