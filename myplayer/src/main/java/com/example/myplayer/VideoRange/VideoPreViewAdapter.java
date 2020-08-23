package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myplayer.R;
import com.example.myplayer.bean.VideoBitmapBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By Ele
 * on 2020/1/14
 **/
public class VideoPreViewAdapter extends RecyclerView.Adapter<VideoPreViewAdapter.ViewHolder>{


    private Context mContext;
    private List<VideoBitmapBean> bitmapList = new ArrayList<>();

    public VideoPreViewAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setDataList(List<VideoBitmapBean> bitmapList){
        this.bitmapList = bitmapList;
        notifyDataSetChanged();
    }

    public void addData(VideoBitmapBean bitmapBean){
        if (bitmapList == null){
            bitmapList = new ArrayList<>();
        }
        bitmapList.add(bitmapBean);
        notifyItemChanged(bitmapList.size() - 1);
    }

    public void setData(int index,VideoBitmapBean bitmapBean){
        if (bitmapList == null){
            bitmapList = new ArrayList<>();
        }
        bitmapList.set(index,bitmapBean);
        notifyItemChanged(index);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.layout_view_pre_item, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoBitmapBean bitmapBean = bitmapList.get(position);
        holder.imageView.setImageBitmap(bitmapBean.getBitmap());
        if (position ==  bitmapList.size() - 1){
            holder.imageView.setRight(300);
        }
    }

    @Override
    public int getItemCount() {
        return bitmapList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_video_pre);
        }
    }
}
