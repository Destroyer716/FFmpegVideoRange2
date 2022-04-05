package com.sam.video.timeline.listener;

import android.view.View;

/**
 * Created by wyh3 on 2018/7/21.
 * 一些通用的、与交互相关的、与业务无关的点击事件监听器
 * 抽离出来公用，避免每次用的时候都要重新写一个
 */
public class Click {

    /**
     * 天真无暇的click
     */
    public interface OnClickListener {
        void onClick();
    }

    /**
     * 监听控件点击时还想多传一个参数
     */
    public interface OnViewClickListener<T> {
        void onClick(View view, T t);
    }

    public interface OnViewLongClickListener<T> {
        void onLongClick(View view, T t);
    }

    /**
     * 监听对象可能是任意一个抽象的实体
     */
    public interface OnObjectClickListener<T> {
        void onObjectClick(T t);
    }

    /**
     * 需要回调位置的监听
     */
    public interface OnPositionClickListener {
        void onPositionClick(int position);
    }

    /**
     * 需要回调位置的监听
     */
    public interface OnItemClickListener<T> {
        void onItemClick(T t, int position);
    }

    /**
     * 需要回调位置的监听
     */
    public interface OnItemViewClickListener<T> {
        void onItemClick(View view, T t, int position);
    }


}
