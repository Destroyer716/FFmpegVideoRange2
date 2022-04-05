package com.sam.video.timeline.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.sam.video.R;

/**
 * 通过 PorterDuffXfermode 得到圆角ImageView
 * @author SamWang(33691286@qq.com)
 * @date 2019/5/7
 */
public class RoundImageView extends AppCompatImageView {

    private int radius = 0;
    private RectF rect;
    private Paint paint;
    private Bitmap mRectMask;
    private PorterDuffXfermode xfermode;

    public RoundImageView(Context context) {
        super(context);
        init();
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        if (attrs != null) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView);
            radius = typedArray.getDimensionPixelOffset(R.styleable.RoundImageView_iv_radius, 0);
            typedArray.recycle();
        }
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
    }

    private void createMask() {
        int maskWidth = getMeasuredWidth();
        int maskHeight = getMeasuredHeight();
        if (maskWidth == 0 || maskHeight == 0) {
            return;
        }
        mRectMask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(mRectMask);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }


    public void setRadius(int radius) {
        this.radius = radius;
        createMask();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect = new RectF(0, 0, w, h);
        createMask();
    }

    @Override
    public void draw(Canvas canvas) {
        int id = canvas.saveLayer(0, 0, getWidth(), getHeight(), null, Canvas.ALL_SAVE_FLAG);
        super.draw(canvas);

        if (mRectMask != null && !mRectMask.isRecycled()) {
            paint.setXfermode(xfermode);
            canvas.drawBitmap(mRectMask, 0, 0, paint);
            paint.setXfermode(null);
        }
        canvas.restoreToCount(id);
    }
}
