package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myplayer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By Ele
 * on 2020/1/14
 **/
public class VideoPreViewAdapter extends RecyclerView.Adapter<VideoPreViewAdapter.ViewHolder>{


    private Context mContext;
    private List<Bitmap> bitmapList = new ArrayList<>();

    public VideoPreViewAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setData(List<Bitmap> bitmapList){
        this.bitmapList = bitmapList;
        notifyDataSetChanged();
    }

    public void addData(Bitmap bitmap){
        if (bitmapList == null){
            bitmapList = new ArrayList<>();
        }
        bitmapList.add(bitmap);
        notifyItemChanged(bitmapList.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.layout_view_pre_item, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bitmap bitmap = bitmapList.get(position);
        holder.imageView.setImageBitmap(bitmap);
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
