package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.myplayer.utils.Utils;


/**
 * Created By Ele
 * on 2020/1/15
 **/
public class DividingView extends View {

    public static final int maxPadding = 50;

    private OnRequestLayoutListener onRequestLayoutListener;

    private Paint pointPaint;
    private Paint textPaint;
    //设置最大宽度，与recyclerView 一样
    private int maxWidth;
    //recyclerView item 数量
    private int videoPic;
    private int rvLeftPaddin;
    //两点之间的拉伸间隔
    private int pointPadding = 0;
    //上一次拉伸的间隔
    private int lastPointPadding = 0;
    //一秒之间有多个个点，最多3个
    private int secondPointNum = 0;
    //总共缩放的长度
    private int totalPadding = 0;

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
        super.onMeasure(MeasureSpec.makeMeasureSpec(maxWidth + totalPadding + 2*rvLeftPaddin,MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);
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
            totalPadding = 0;
            for (int i=0;i<videoPic + 1;i++){
                if (i > 0 && secondPointNum > 0){
                    int lastX = t*(i-1) + rvLeftPaddin +(i-1) *pointPadding;
                    int currentX = t*i + rvLeftPaddin + i *pointPadding;
                    int itemPointX = (currentX - lastX)/(secondPointNum + 1);
                    for (int j=1;j<=secondPointNum;j++){
                        canvas.drawPoint(lastX + j *itemPointX,50,pointPaint);
                    }
                }

                canvas.drawPoint(t*i + rvLeftPaddin + i *pointPadding,50,pointPaint);
                String parseTime = Utils.MilliToMinuteTime(i*1000);
                float textWidth = textPaint.measureText(parseTime);
                canvas.drawText(parseTime,t*i - (textWidth / 2) + rvLeftPaddin + i *pointPadding,20,textPaint);
            }
            totalPadding = videoPic *pointPadding;
            requestLayout();
            if (onRequestLayoutListener != null){
                onRequestLayoutListener.onLayout();
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

    public int getPointPadding() {
        return pointPadding;
    }

    public void setPointPadding(int pointPadding) {
        if (pointPadding < 0){
            pointPadding = 0;
        }
        if (pointPadding > maxPadding * 4){
            pointPadding = maxPadding * 4;
        }
        this.pointPadding = pointPadding ;
        secondPointNum = pointPadding / maxPadding;
    }

    public int getLastPointPadding() {
        return lastPointPadding;
    }

    public void setLastPointPadding(int lastPointPadding) {
        this.lastPointPadding = lastPointPadding;
    }

    public int getVideoPic() {
        return videoPic;
    }

    public void setVideoPic(int videoPic) {
        this.videoPic = videoPic;
    }

    public int getTotalPadding() {
        return totalPadding;
    }

    public void setOnRequestLayoutListener(OnRequestLayoutListener onRequestLayoutListener) {
        this.onRequestLayoutListener = onRequestLayoutListener;
    }

    public void setTotalPadding(int totalPadding) {
        this.totalPadding = totalPadding;
    }


    public interface OnRequestLayoutListener{
        void onLayout();
    }
}
