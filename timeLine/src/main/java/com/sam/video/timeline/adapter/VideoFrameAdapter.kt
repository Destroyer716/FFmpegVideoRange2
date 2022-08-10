package com.sam.video.timeline.adapter

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.sam.video.R
import com.sam.video.timeline.bean.VideoFrameData
import com.sam.video.timeline.helper.IAvFrameHelper
import com.sam.video.timeline.helper.VideoDecoder2
import com.sam.video.timeline.widget.RoundRectMask
import com.sam.video.timeline.widget.VideoFrameRecyclerView

/**
 * 帧列表 adapter
 * @author SamWang(33691286@qq.com)
 * @date 2019-07-19
 */
class VideoFrameAdapter(data: MutableList<VideoFrameData>, private val frameWidth: Int) : BaseQuickAdapter<VideoFrameData, BaseViewHolder>(
    R.layout.item_video_frame, data) {

    private var index = 0

    override fun convert(helper: BaseViewHolder, item: VideoFrameData) {
        helper.adapterPosition
        val imageView = helper.getView<ImageView>(R.id.iv)
        val layoutParams = helper.itemView.layoutParams
        layoutParams.width = item.frameWidth

        val maskView = helper.getView<RoundRectMask>(R.id.mask)
        maskView.setCornerRadiusDp(4f)
        maskView.setCorners(item.isFirstItem, item.isLastItem, item.isFirstItem, item.isLastItem)
        val maskLayoutParams = maskView.layoutParams as FrameLayout.LayoutParams
        val ivLayoutParams = imageView.layoutParams as FrameLayout.LayoutParams

        if (item.isFirstItem) {
            maskLayoutParams.gravity = Gravity.LEFT
            ivLayoutParams.gravity = Gravity.RIGHT
        } else {
            maskLayoutParams.gravity = Gravity.RIGHT
            ivLayoutParams.gravity = Gravity.LEFT
        }

        maskLayoutParams.width = if (item.isFirstItem && item.isLastItem) {
            ivLayoutParams.gravity = Gravity.LEFT
            ivLayoutParams.marginStart = -item.offsetX //只有一帧考虑位移

            item.frameWidth             //如果一段视频在列表中只有一帧，则要显示全部圆角，遮罩同步缩小
        } else {
            ivLayoutParams.marginStart = 0
            frameWidth
        }

        imageView.tag = helper.adapterPosition

        if (recyclerView is VideoFrameRecyclerView) {
            (recyclerView as VideoFrameRecyclerView).getAvFrameHelperByIndex(item.videoData.index)?.loadAvFrame(imageView,item.frameClipTime * 1000)
        }

        if (recyclerView !is VideoFrameRecyclerView ||  (recyclerView as VideoFrameRecyclerView).getAvFrameHelper() == null){
            Glide.with(imageView)
                .asBitmap()
                .load(item.videoData.originalFilePath)
                .frame(item.frameClipTime * 1000)
                .thumbnail(
                    //todo 更好的方案是往前找一个已经有的缓存帧
                    Glide.with(imageView).asBitmap().load(item.videoData.originalFilePath)
                )
                .into(imageView)
        }
        /**/
    }






    override fun onViewDetachedFromWindow(holder: BaseViewHolder) {
        if (recyclerView is VideoFrameRecyclerView && holder.adapterPosition >=0) {
            //Log.e("kzg","**************onViewDetachedFromWindow:${holder.adapterPosition}")
            (recyclerView as VideoFrameRecyclerView).getAvFrameHelperByIndex(data[holder.adapterPosition].videoData.index)?.removeAvFrameTag(holder.itemView.findViewById<ImageView>(R.id.iv))
        }
        //Log.e("kzg","*******************onViewDetachedFromWindow:${holder.itemView.findViewById<View>(R.id.iv)}")
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        if (recyclerView is VideoFrameRecyclerView) {
                //Log.e("kzg","**************onViewAttachedToWindow:${holder.adapterPosition}")
            (recyclerView as VideoFrameRecyclerView).getAvFrameHelperByIndex(data[holder.adapterPosition].videoData.index)?.addAvFrame(holder.itemView.findViewById<ImageView>(R.id.iv))
        }
        super.onViewAttachedToWindow(holder)

    }

}




