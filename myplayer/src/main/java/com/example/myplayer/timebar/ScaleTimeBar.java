package com.example.myplayer.timebar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScaleTimeBar extends View {

    private static final String NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
    /*一个刻度对应的屏幕像素大小*/
    private float pixelsPerScaler;
    private ScaleModel mScaleModel;
    private Scroller mScroller;
    private ScaleGestureDetector mScaleGestureDetector;
    //游标(中轴线)
    private Course mCourse;
    private long zoomModelCourseTime;
    private int screenWidthPixels;
    private int screenHeightPixels;
    private int layout_width;
    private int layout_height;
    private boolean drawDiving = false;
    private VelocityTracker mVelocityTracker;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;
    private int screenWidth;

    public ScaleTimeBar(Context context) {
        this(context, null);
    }

    public ScaleTimeBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleTimeBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        try {
            String width = attrs.getAttributeValue(NAMESPACE_ANDROID, "layout_width");
            String height = attrs.getAttributeValue(NAMESPACE_ANDROID, "layout_height");
            String width_dip = width.replace("dip", "").trim();
            String height_dip = height.replace("dip", "").trim();
            layout_width = dp2px(Float.valueOf(width_dip));
            layout_height = dp2px(Float.valueOf(height_dip));
            screenWidth = getScreenWidth(context);
            Log.e("kzg","************************screenWidth:"+screenWidth);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        init();
    }

    private void init() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        screenWidthPixels = displayMetrics.widthPixels;
        screenHeightPixels = displayMetrics.heightPixels;

        mScroller = new Scroller(getContext());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), onScaleGestureListener);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        //获得允许执行一个fling手势动作的最小速度值
        mMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        //获得允许执行一个fling手势动作的最大速度值
        mMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        long startTime = instance.getTimeInMillis();
        instance.set(Calendar.HOUR_OF_DAY, 23);
        instance.set(Calendar.MINUTE, 59);
        instance.set(Calendar.SECOND, 59);
        instance.set(Calendar.MILLISECOND, 999);
        long endTime = instance.getTimeInMillis();
        initScaleInfo(startTime, /*开始时间:分*/endTime/*结束时间:分*/);

    }

    public void setModel(ScaleModel.UnitModel unitModle) {
        mScaleModel.setSizeParam(unitModle);
        calcPixelsPerSecond();
        updateCoursePosition();
        invalidate();
    }

    private void scaleView(float scaleFactor) {
        int scaleWidth = (int) (getWidth() * scaleFactor);
        int scaleScrollX = (int) (getScrollX() * scaleFactor);
        Log.e("kzg","**********************scaleView:"+mScaleModel.changeSize(scaleWidth) + ",  scaleScrollX:"+scaleScrollX);
        if (mScaleModel.changeSize(scaleWidth)) {

            calcPixelsPerSecond();
            updateCoursePosition();
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.width = scaleWidth;
            setLayoutParams(layoutParams);
            scrollTo(scaleScrollX, 0);
        }
    }

    ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleView(detector.getScaleFactor());
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    /**
     * 初始化所有刻度信息
     *
     * @param startTime 刻度开始值
     * @param endTime   刻度结束值
     */
    private void initScaleInfo(long startTime, long endTime) {
        mScaleModel = new ScaleModel();
        //设置最小值
        mScaleModel.setSartValue(startTime);
        //设置最大值
        mScaleModel.setEndValue(endTime);
        //计算刻度集
        mScaleModel.setSizeParam(ScaleModel.UnitModel.UNITVALUE_5_MIN);
        mCourse = new Course();
        mCourse.setPosition(0);
        zoomModelCourseTime = mScaleModel.getSartValue();
    }

    public void setScaleStartValue(long startValue) {
        mScaleModel.setSartValue(startValue);
    }

    public void setScaleEndValue(long endValue) {
        mScaleModel.setEndValue(endValue);
    }

    public void setCurrTime(long time) {
        if (time < mScaleModel.getSartValue())
            return;
        if (time > mScaleModel.getEndValue())
            return;
        zoomModelCourseTime = time;
        scrollByTimeCalibration();
        updateCoursePosition();
        postInvalidate();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        Log.e("kzg","**********************computeScroll");
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else {
            if (motionModel == MotionModel.ComputeScroll
                    || motionModel == MotionModel.FlingScroll) {
                motionModel = MotionModel.None;
            }
        }
        updateCoursePosition();
        if (null != onBarMoveListener) {
            if (motionModel == MotionModel.None) {
                onBarMoveListener.onBarMoveFinish(calcCourseTimeMills());
            } else if (motionModel == MotionModel.Move
                    || motionModel == MotionModel.FlingScroll) {
                onBarMoveListener.onBarMove(calcCourseTimeMills());
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        long startTime = System.currentTimeMillis();

        setMeasuredDimension(getDefautSize(layout_width <= 0 ? getSuggestedMinimumWidth() : layout_width,
                widthMeasureSpec),
                getDefautSize(layout_height <= 0 ? getSuggestedMinimumHeight() : layout_height,
                        heightMeasureSpec));
        Log.e("kzg","**********************onMeasure:"+(mScaleModel.getScaleWith()));
        if (mScaleModel.getDisPlayWidth() == 0) {
            mScaleModel.setDisPlayWidth(getMeasuredWidth());
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.width = mScaleModel.getScaleWith();
            setLayoutParams(layoutParams);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.e("kzg","**********************onSizeChanged:"+(motionModel != MotionModel.Move));
        //　尺寸发生改变，重新计算单位刻度对应屏幕像素
        calcPixelsPerSecond();
        if (motionModel != MotionModel.Move) { //校准刻度尺
            scrollByTimeCalibration();
            updateCoursePosition();
        } else {
            updateCoursePosition();
        }
    }

    private int getDefautSize(int size, int measureSpec) {
        int measureMode = MeasureSpec.getMode(measureSpec);
        int measureSize = MeasureSpec.getSize(measureSpec);
        int result = size;
        switch (measureMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                result = measureSize;
                break;
            case MeasureSpec.UNSPECIFIED:
                result = size > 0 ? size : measureSize / 2;
                break;
        }
        return result;
    }

    /**
     * 计算单位刻度对应屏幕像素
     */
    private void calcPixelsPerSecond() {
        pixelsPerScaler = getWidth() * 1.0f / mScaleModel.getScaleCount();
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        long startTime = System.currentTimeMillis();
        super.onDraw(canvas);
        if (null == mScaleModel) return;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        //绘制刻度的开始位置
        int timebarStatX = getTimebarStatX();
        //重置画笔颜色
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        //绘制顶部横线
        canvas.drawLine(0 - 100,
                0,
                calcTimebarWidth() + getWidth() + 100,
                0, paint);
        //绘制底部横线
        canvas.drawLine(0 - 100,
                getHeight(),
                getWidth() + calcTimebarWidth() + 100,
                getHeight(), paint);
        //重置画笔宽度
        paint.setStrokeWidth(1);
        //高的1/3
        int height_1_3 = getHeight() / 3;
        if (drawDiving) {
            //绘制1/3处横线/*canvas.drawLine(0,
            //                    height_1_3,
            //                    getWidth() + calcTimebarWidth(),
            //                    height_1_3, paint);*/

        }
        int timeBarStopY = height_1_3 * 2;
        //绘制时间轴横线
        /*canvas.drawLine(timebarStatX,
                timeBarStopY,
                getWidth() + calcTimebarWidth(),
                timeBarStopY, paint);*/


        int a = 0,b=0;
        Scaler scaler = null;
        //高的1/10
        int height_1_10 = getHeight() / 10;
        List<Scaler> scaleList = mScaleModel.getScaleList();
        if (null == scaleList) return;
        float startX = 0, stopX = 0, startY = 0, stopY = 0;
        int scrollX = getScrollX();
        for (int i = 0; i < scaleList.size(); i++) {
            scaler = scaleList.get(i);
            if (null == scaler) continue;
            startX = timebarStatX + pixelsPerScaler * scaler.getPosition();
            stopY = timeBarStopY;
            // 只绘制控件width区域 (显示区域)
            if (startX < scrollX + timebarStatX - screenWidth) {
                a ++;
                continue;
            }
            if (startX > scrollX + screenWidth + timebarStatX/*scrollX + getWidth()*/) {
                a ++;
                continue;
            }

            b++;
            if (scaler.isKeyScaler()) {  //关键刻度
                stopX = startX;
                startY = stopY - height_1_10 * 2;
                //设置文字画笔
                paint.setTextSize(20);
                paint.setStrokeWidth(1);
                String subscrib = mScaleModel.getSubscrib(scaler);
                //测量一个字符的大小
                float size = paint.measureText("a");
                int count = TextUtils.isEmpty(subscrib) ? 0 : subscrib.length();
                //绘制文字
                canvas.drawText(TextUtils.isEmpty(subscrib) ? "" : subscrib,
                        0,
                        count,
                        startX - size * count / 2,
                        startY - size / 2,
                        paint);
                //设置刻度画笔
                paint.setStrokeWidth(5);
            } else {
                stopX = startX;
                startY = stopY - height_1_10;
                //设置刻度画笔
                paint.setStrokeWidth(4);
            }
            //绘制刻度
            canvas.drawPoint(startX,stopY,paint);
            /*canvas.drawLine(startX,
                    startY,
                    stopX,
                    stopY,
                    paint);*/


        }
        /*long startValue = 0;
        long endValue = 0;
        //绘制颜色刻度
        for (SmallTime smallTime : smallTimeList) {
            if (null == smallTime) continue;
            startValue = smallTime.getStartValue() < mScaleModel.getSartValue() ?
                    mScaleModel.getSartValue() : smallTime.getStartValue();
            endValue = smallTime.getEndValue() > mScaleModel.getEndValue() ?
                    mScaleModel.getEndValue() : smallTime.getEndValue();

            //计算开始绘制位置
            startX = timebarStatX + calcPixelsByTime(startValue - mScaleModel.getSartValue());
            startY = timeBarStopY;
            //计算结束绘制位置
            stopX = timebarStatX + calcPixelsByTime(endValue - mScaleModel.getSartValue());
            stopY = getHeight() - 5;

            paint.setColor(smallTime.getTimeColor());
            canvas.drawRect(startX, startY, stopX, stopY, paint);
        }*/

      /*  int courseStartX = 0;
        if (null != mCourse) {
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(2);
            //游标移动的距离
            float pixels = mCourse.getPosition() * pixelsPerScaler;
            courseStartX = (int) (timebarStatX + pixels);
            //绘制三角形
            Path path = new Path();
            path.moveTo(courseStartX - 18, 0);
            path.lineTo(courseStartX + 18, 0);
            path.lineTo(courseStartX, 18);
            path.close();
            canvas.drawPath(path, paint);
            //绘制游标刻度线
            canvas.drawLine(courseStartX,
                    18,
                    courseStartX,
                    getHeight(),
                    paint);
            String format = "";
            if (motionModel == MotionModel.Move) { //滑动模式
                format = simpleDateFormat.format(calcCourseTimeMills());
            } else if (motionModel == MotionModel.Zoom) { // 缩放模式
                format = simpleDateFormat.format(zoomModelCourseTime);
            } else {
                format = simpleDateFormat.format(calcCourseTimeMills());
            }
            //重置画笔
            paint.setColor(Color.BLACK);
            paint.setTextSize(25);
            int textLength = TextUtils.isEmpty(format) ? 0 : format.length();
            int size = textLength <= 0 ? 0 : (int) paint.measureText(format) / textLength;
            int y = height_1_3 / 2 + size / 2;
            canvas.drawText(format,
                    0,
                    textLength,
                    courseStartX - size * textLength / 2,
                    y,
                    paint);

        }*/
        Log.e("kzg","**********************draw:"+(System.currentTimeMillis() - startTime) + "  , a:"+a + "   ,b:"+b
                + "  ,timebarStatX:"+timebarStatX + "  ,scrollX:"+scrollX + "  ,getWidth():"+getWidth());




        /*if (videoPic != 0 && defaultWidth != 0 ){

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
                        canvas.drawPoint(lastX + j *itemPointX,height_1_3,pointPaint);
                    }
                }
                canvas.drawPoint(t*i + rvLeftPaddin + i *pointPadding,height_1_3,pointPaint);
                String parseTime = MilliToMinuteTime(i*1000);
                float textWidth = textPaint.measureText(parseTime);
                canvas.drawText(parseTime,t*i - (textWidth / 2) + rvLeftPaddin + i *pointPadding,height_1_3,textPaint);
            }

            totalPadding = videoPic *pointPadding;


        }*/
    }

    /**
     * 校准时间
     */
    private void scrollByTimeCalibration() {
        long dTime = zoomModelCourseTime - calcCourseTimeMills();
        if (dTime != 0) {  // 用于处理缩放时 时间轴变化bug
            scrollBy(calcPixelsByTime(dTime), 0);
        }
    }

    public long getTime() {
        return calcCourseTimeMills();
    }

    public long getStartValue() {
        return mScaleModel.getSartValue();
    }

    public long getEndValue() {
        return mScaleModel.getEndValue();
    }

    /**
     * 计算一段时间对应的像素大小
     *
     * @param time
     * @return
     */
    private int calcPixelsByTime(long time) {
        return (int) (time * 1.0f / mScaleModel.getDecimal() / mScaleModel.getUnitValue() * pixelsPerScaler);
    }

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private int getTimebarStatX() {
        return getMeasuredWidth() / 2;
    }

    int lastX = 0;
    MotionModel motionModel = MotionModel.None;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (mScaleGestureDetector.isInProgress()) return true;
        obtainVelocityTracker(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:   // 手指按下
                motionModel = MotionModel.Down;
                //记录按下x位置
                lastX = (int) event.getX();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // 多指监听
                motionModel = MotionModel.Zoom;
                zoomModelCourseTime = calcCourseTimeMills();
                break;
            case MotionEvent.ACTION_MOVE:
                if (motionModel == MotionModel.Zoom) {
                } else if (motionModel == MotionModel.Down
                        || motionModel == MotionModel.Move) {
                    motionModel = MotionModel.Move;
                    //获取当前x位置
                    int currX = (int) event.getX();
                    //开始移动
                    scrollBy(calcScrollX(lastX, currX), 0);
                    lastX = currX;
//                    //更新x记录
//                    updateCoursePosition();
//                    if (null != onBarMoveListener) {
//                        onBarMoveListener.onBarMove(calcCourseTimeMills());
//                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                motionModel = MotionModel.None;
                int scrollX = getScrollX();
                int scrollY = getScrollY();
                if (scrollX < 0) {
                    motionModel = MotionModel.ComputeScroll;
                    mScroller.startScroll(scrollX,
                            scrollY,
                            0 - scrollX,
                            0 - scrollY);
                    postInvalidate();
                } else if (scrollX > calcMaxScaleScrollX()) {
                    motionModel = MotionModel.ComputeScroll;
                    mScroller.startScroll(scrollX,
                            scrollY,
                            calcMaxScaleScrollX() - scrollX,
                            0 - scrollY);
                    postInvalidate();
                } else {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    int xVelocity = (int) mVelocityTracker.getXVelocity();
                    if (Math.abs(xVelocity) > mMinimumFlingVelocity && Math.abs(xVelocity) < mMaximumFlingVelocity) { //惯性滑动
                        motionModel = MotionModel.FlingScroll;
                        mScroller.fling(scrollX,
                                scrollY,
                                -xVelocity,
                                0,
                                0,
                                calcMaxScaleScrollX(),
                                0,
                                getHeight());
                        awakenScrollBars(mScroller.getDuration());
                        postInvalidate();
                    } else {
                        updateCoursePosition();
                        if (null != onBarMoveListener) {
                            onBarMoveListener.onBarMoveFinish(calcCourseTimeMills());
                        }
                    }
                }
                break;
        }
        return true;
    }

    /**
     * 设置MotionEvent给VelocityTracker
     *
     * @param event
     */
    private void obtainVelocityTracker(MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (null != mVelocityTracker) { //回收
            mVelocityTracker.recycle();
        }
    }

    private void updateCoursePosition() {
        mCourse.setPosition(pixelsPerScaler <= 0 ? 0 : getScrollX() / pixelsPerScaler);
    }

    /**
     * 计算游标在刻度尺中的真实值
     */
    public long calcCourseTimeMills() {
        return mScaleModel.getSartValue() + (long) (mCourse.getPosition() * mScaleModel.getDecimal() * mScaleModel.getUnitValue());
    }

    /**
     * 计算 x 方向需要滑动的距离
     *
     * @param lastX 上次手指坐标
     * @param currX 当前手指坐标
     * @return
     */
    private int calcScrollX(int lastX, int currX) {
        int dx = lastX - currX;  //dx大于0:(上次手指坐标大于当前手指坐标)手指往左边移动(读取更大刻度值)    dx小于0:(上次手指坐标小于当前手指坐标)手指往右边移动(读取更小刻度值)
        //时间轴最小值与游标重合（已读到最小值）
        if (getScrollX() < 0 && dx < 0) { //已读到最小值,手指还在继续往右边滑动时,为了不让时间轴继续往右滑,dx=0
            dx = 0;
        } else if (getScrollX() > calcMaxScaleScrollX() && dx > 0) { //已读到最大值,手指还在继续往左边滑动时,为了不让时间轴继续往左滑,dx=0
            dx = 0;
        }
        return dx;
    }

    /**
     * 计算读取刻度尺最大值滑动的距离
     *
     * @return
     */
    private int calcMaxScaleScrollX() {
        return (int) calcTimebarWidth();
    }

    /**
     * 计算刻度尺长度范围: ０~Ｎ
     *
     * @return
     */
    private float calcTimebarWidth() {
        return pixelsPerScaler * mScaleModel.getScaleCount();
    }

    private OnBarMoveListener onBarMoveListener;

    public void setOnBarMoveListener(OnBarMoveListener onBarMoveListener) {
        this.onBarMoveListener = onBarMoveListener;
    }

    public interface OnBarMoveListener {
        void onBarMove(long time);

        void onBarMoveFinish(long time);
    }

    /**
     * 事件
     */
    enum MotionModel {
        None,
        Down, //
        Zoom,
        Move,
        ComputeScroll,
        FlingScroll;
    }

    private int dp2px(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5);
    }

    public static String MilliToMinuteTime(long duration){
        String time = "" ;
        long minute = duration / 60000 ;
        long seconds = duration % 60000 ;
        long second = Math.round((float)seconds/1000) ;
        if( minute < 10 ){
            time += "0" ;
        }
        time += minute+":" ;
        if( second < 10 ){
            time += "0" ;
        }
        time += second ;
        return time ;
    }

    private int getScreenWidth(Context context){
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        return width;
    }
}
