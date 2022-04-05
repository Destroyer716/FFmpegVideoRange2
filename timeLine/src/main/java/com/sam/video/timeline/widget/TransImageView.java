package com.sam.video.timeline.widget;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class TransImageView extends ImageView {
    public TransImageView(Context context) {
        this(context,null);
    }

    public TransImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TransImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if(drawable!=null){
            super.setImageDrawable(drawable);
        }
    }
}
