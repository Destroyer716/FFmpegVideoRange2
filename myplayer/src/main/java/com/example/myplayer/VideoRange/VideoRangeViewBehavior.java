package com.example.myplayer.VideoRange;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Created By Ele
 * on 2020/1/15
 **/
public class VideoRangeViewBehavior extends CoordinatorLayout.Behavior<DividingView> {
    public VideoRangeViewBehavior() {
    }

    public VideoRangeViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DividingView child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return target instanceof RecyclerView;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull DividingView child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        if (target instanceof RecyclerView){
            //Log.e("kzg","***********************dx:"+dx);
            //child.scrollTo(dx,0);
        }

    }
}
