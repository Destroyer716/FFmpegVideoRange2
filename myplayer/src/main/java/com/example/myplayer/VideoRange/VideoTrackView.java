package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myplayer.R;
import com.example.myplayer.bean.VideoBitmapBean;

import java.util.List;

/**
 * Created By Ele
 * on 2020/8/23
 **/
public class VideoTrackView extends FrameLayout {
    private Context mContext;
    private LinearLayout llVideoPicList;
    private List<VideoBitmapBean>  data;

    //每一张预览图的宽度
    private int itemPicWidth = 100;
    //最多显示多少张预览图
    public static int maxPicNum = 300;

    /**
     * 重新渲染数据
     * @param data
     */
    public void setData(List<VideoBitmapBean> data) {
        this.data = data;
        maxPicNum = 100;
        showVideoPic();
    }

    /**
     * 更新指定下标的数据
     * @param videoBitmapBean
     * @param index
     */
    public void updateData(VideoBitmapBean videoBitmapBean,int index){
        if (videoBitmapBean == null || data == null || llVideoPicList == null){
            return;
        }
        if (index >= data.size() || index >= llVideoPicList.getChildCount()){
            return;
        }

        ImageView imageView = (ImageView) llVideoPicList.getChildAt(index);
        imageView.setImageBitmap(videoBitmapBean.getBitmap());

    }

    /**
     * 更新数据，如果原来的数据长度比新数据长，就删除多余的旧数据
     * @param data
     */
    public void updateData(List<VideoBitmapBean> data){
        if ( data == null || llVideoPicList == null){
            return;
        }
        int childCount = llVideoPicList.getChildCount();
        //倒序是为了防止删除view的时候产生bug
        for (int i=childCount-1;i>=0;i--){
            if (i < data.size()){
                ImageView imageView = (ImageView) llVideoPicList.getChildAt(i);
                imageView.setImageBitmap(data.get(i).getBitmap());
            }else {
                llVideoPicList.removeViewAt(i);
            }
        }

    }

    /**
     * 末尾添加数据
     * @param videoBitmapBean
     */
    public void addData(VideoBitmapBean videoBitmapBean){
        if (llVideoPicList == null || videoBitmapBean == null){
            return;
        }
        ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemPicWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(videoBitmapBean.getBitmap());
        llVideoPicList.addView(imageView);
    }


    /**
     * 指定下标处添加数据
     * @param videoBitmapBean
     * @param index
     */
    public void addData(VideoBitmapBean videoBitmapBean,int index){
        if (llVideoPicList == null || videoBitmapBean == null){
            return;
        }
        ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemPicWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(videoBitmapBean.getBitmap());
        llVideoPicList.addView(imageView,index);
        if (llVideoPicList.getChildCount() > 300){
            llVideoPicList.removeViewAt(llVideoPicList.getChildCount() - 1);
        }
    }

    /**
     * 移除指定下标的数据
     * @param index
     */
    public void remove(int index){
        if (llVideoPicList == null || index >= llVideoPicList.getChildCount()){
            return;
        }

    }


    public VideoTrackView(@NonNull Context context) {
        this(context,null);
    }

    public VideoTrackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public VideoTrackView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        this.mContext = context;
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.video_track_layout, this, true);
        llVideoPicList = inflate.findViewById(R.id.ll_video_pic_list);
    }


    private void showVideoPic(){
        if (llVideoPicList == null){
            return;
        }

        llVideoPicList.removeAllViews();
        if (data == null){
            return;
        }
        int i=0;
        for (VideoBitmapBean bean:data){

            ImageView imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemPicWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(layoutParams);
            if (i < maxPicNum){
                imageView.setImageBitmap(bean.getBitmap());
            }
            llVideoPicList.addView(imageView);

            i ++;
        }

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return super.onInterceptTouchEvent(e);
    }


    public int getItemPicWidth() {
        return itemPicWidth;
    }

    public void setItemPicWidth(int itemPicWidth) {
        this.itemPicWidth = itemPicWidth;
    }

    public int getPreItemCount(){
        if (llVideoPicList == null){
            return 0;
        }
        return llVideoPicList.getChildCount();
    }

}
