package com.example.ffmpegvideorange2

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.widget.ImageView
import com.sam.video.timeline.bean.TargetBean

class ImageViewBitmapBean(var mapEntry: Map.Entry<ImageView,TargetBean>? = null
                          , var image: YuvImage? = null,var rect: Rect? = null,val time:Long) {

}