package com.example.ffmpegvideorange2;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created By Ele
 * on 2020/6/25
 **/
public class PreviewImageAdapter extends RecyclerView.Adapter<PreviewImageAdapter.ViewHolder>{

    private List<Bitmap> dataList = new ArrayList<>();
    private Context context;
    private OnItemClickListener onItemClickListener;

    public PreviewImageAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Bitmap> data){
        if (data == null){
            data = new ArrayList<>();
        }
        dataList = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(context).inflate(R.layout.item_preview_img, parent, false);
        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Bitmap bitmap = dataList.get(position);
        holder.img.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        ImageView img;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.iv_img);
        }
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener{
        void onItemClick(int position);
    }
}
