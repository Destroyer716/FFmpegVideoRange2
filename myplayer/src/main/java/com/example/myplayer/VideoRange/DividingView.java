package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.myplayer.Utils;


/**
 * Created By Ele
 * on 2020/1/15
 **/
public class DividingView extends View {

    private Paint pointPaint;
    private Paint textPaint;
    //设置最大宽度，与recyclerView 一样
    private int maxWidth;
    //recyclerView item 数量
    private int videoPic;
    private int rvLeftPaddin;

    public DividingView(Context context) {
        super(context);
        init();
    }

    public DividingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DividingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        pointPaint = new Paint();
        pointPaint.setColor(Color.BLACK);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(5);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeWidth(5);
        textPaint.setTextSize(20f);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (videoPic != 0 && maxWidth != 0 ){
            int t = (maxWidth-0*rvLeftPaddin)/videoPic;
            for (int i=0;i<videoPic + 1;i++){
                canvas.drawPoint(t*i + rvLeftPaddin,50,pointPaint);
                String parseTime = Utils.MilliToMinuteTime(i*1000);
                float textWidth = textPaint.measureText(parseTime);
                canvas.drawText(parseTime,t*i - (textWidth / 2) + rvLeftPaddin,20,textPaint);
            }
        }
    }


    public void setMaxWidth(int width){
        maxWidth = width;
    }

    public void setVideoPicNum(int num){
        videoPic = num;
    }

    public void setLeftPaddin(int leftPaddin){
        rvLeftPaddin = leftPaddin;
    }
}
