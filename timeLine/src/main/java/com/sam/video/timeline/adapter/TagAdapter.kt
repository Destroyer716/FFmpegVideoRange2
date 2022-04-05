package com.sam.video.timeline.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseViewHolder
import com.sam.video.R
import com.sam.video.timeline.bean.TagLineViewData
import com.sam.video.timeline.bean.TagType
import kotlinx.android.synthetic.main.item_tag_img.view.*
import kotlinx.android.synthetic.main.item_tag_img.view.selectView
import kotlinx.android.synthetic.main.item_tag_text.view.*


/**
 * 文字贴纸 弹窗列表 adapter
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-07
 */
class TagAdapter(val context: Context, val data: MutableList<TagLineViewData>) :
        RecyclerView.Adapter<BaseViewHolder>(), View.OnClickListener {
    var selectedItem: TagLineViewData? = null


    override fun getItemViewType(position: Int): Int {
        return data[position].itemType
    }

    //fixme 不知为何，没有复用viewHolder，滑动一直调create，导致滑动很卡
    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): BaseViewHolder {
        val layout: Int = if (itemType == TagType.ITEM_TYPE_IMG) {
            R.layout.item_tag_img
        } else { //TEXT
            R.layout.item_tag_text
        }
        val itemView: View =  LayoutInflater.from(context).inflate(layout, parent, false)

        itemView.setOnClickListener(this)
        return BaseViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = data[position]
        holder.itemView.selectView.visibility =  if(selectedItem === item) View.VISIBLE else View.GONE
        if (getItemViewType(position) == TagType.ITEM_TYPE_IMG) {
            holder.itemView.iv?.let {
                it.setBackgroundColor(item.color)
                Glide.with(it).load(item.content).into(it)
            }
        } else {
            holder.itemView.tv?.apply {
                setBackgroundColor(item.color)
                text = item.content
            }
        }
    }

    var onClickListener: View.OnClickListener? = null

    override fun onClick(v: View?) {
        v ?: return
        onClickListener?.onClick(v)
    }
}
