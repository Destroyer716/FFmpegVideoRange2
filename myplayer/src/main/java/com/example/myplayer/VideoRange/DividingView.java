package com.example.myplayer.VideoRange;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
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
    //是否改变了长度
    private boolean isChange = true;
    //限定绘制的区域
    private int lefX = 0;
    private int rightX = 0;
    //已经滑动过的距离
    private int scrollCount = 0;
    //初始化时的预览条宽度
    private int defaultWidth;

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
        long start = System.currentTimeMillis();
        super.onMeasure(MeasureSpec.makeMeasureSpec(defaultWidth + totalPadding + 2*rvLeftPaddin,MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);

        if (onRequestLayoutListener != null){
            //onRequestLayoutListener.onLayout();
        }
        Log.e("kzg","*************************onMeasure:"+(System.currentTimeMillis() - start) + "  ,  "+(defaultWidth + totalPadding));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        long start = System.currentTimeMillis();
        super.onLayout(changed, left, top, right, bottom);
        //Log.e("kzg","*************************onLayout:"+(System.currentTimeMillis() - start));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        long start = System.currentTimeMillis();
        super.onDraw(canvas);
        if (videoPic != 0 && defaultWidth != 0 ){
            int t = (defaultWidth-0*rvLeftPaddin)/videoPic;
            totalPadding = 0;
            for (int i=0;i<videoPic + 1;i++){
                if (t*i + rvLeftPaddin + i *pointPadding < lefX || t*i + rvLeftPaddin + i *pointPadding > rightX){
                    continue;
                }
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
            if (onRequestLayoutListener != null){
                onRequestLayoutListener.onChangeWidth(totalPadding);
            }

            Log.e("kzg","*************************onDraw11111:"+(System.currentTimeMillis() - start));
            if (isChange){

            }

        }
        Log.e("kzg","*************************onDraw:"+(System.currentTimeMillis() - start));
    }

    public void setScaleFactor(float factor){
        maxWidth = (int) (maxWidth * factor);
        pointPadding = (maxWidth - defaultWidth)/videoPic;
        if (pointPadding < 0){
            pointPadding = 0;
        }
        if (pointPadding > maxPadding * 4){
            pointPadding = maxPadding * 4;
        }
        secondPointNum = pointPadding / maxPadding;
        isChange = true;
        postInvalidate();

        /*ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = defaultWidth + totalPadding + 2*rvLeftPaddin;
        setLayoutParams(layoutParams);*/
    }



    public void setMaxWidth(int width){
        maxWidth = width;
    }

    public void setVideoPicNum(int num){
        videoPic = num;
        isChange = true;
    }

    public void setLeftPaddin(int leftPaddin){
        rvLeftPaddin = leftPaddin;
        isChange = true;
    }

    public int getPointPadding() {
        return pointPadding;
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

    public void setChange(boolean change) {
        isChange = change;
    }

    public int getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    /**
     * 设置已经滚动的水平距离，并以此计算需要绘制的范围,减小绘制范围，降低draw方法时间
     *
     * @param scrollX
     */
    public void setDrawAround(int scrollX){
        scrollCount = scrollX;
        if (scrollX <= 0){
            lefX = 0;
            rightX = defaultWidth + totalPadding + 2*rvLeftPaddin;
        }else {
            if (scrollX <=3 * rvLeftPaddin){
                lefX = 0;
            }else {
                lefX =  scrollX - 2 * rvLeftPaddin;
            }
            rightX = scrollX + 6 * rvLeftPaddin;
        }
    }





    public void setOnRequestLayoutListener(OnRequestLayoutListener onRequestLayoutListener) {
        this.onRequestLayoutListener = onRequestLayoutListener;
    }

    public void setTotalPadding(int totalPadding) {
        this.totalPadding = totalPadding;
    }


    public interface OnRequestLayoutListener{
        void onLayout();
        void onChangeWidth(int changeWidth);
    }
}
