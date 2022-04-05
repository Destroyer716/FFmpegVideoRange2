package com.sam.video.timeline.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.sam.video.R;


/**
 * 只需要设置背景色就能得到圆角的背景 TextView
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019/5/7
 */
public class RoundTextView extends AppCompatTextView {

    private int radius = 0;
    private RectF rect;
    private Path path;

    public RoundTextView(Context context) {
        this(context, null);
        init();
    }

    public RoundTextView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);

    }

    public RoundTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundTextView);
            radius = typedArray.getDimensionPixelOffset(R.styleable.RoundTextView_tv_radius, 0);
            typedArray.recycle();
        }
        init();
    }

    private void init() {
        rect = new RectF();
        path = new Path();
    }

    public void setRadius(int radius) {
        this.radius = radius;
        updatePath();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0, 0, w, h);
        updatePath();
    }

    private void updatePath() {
        if (path == null) {
            init();
        }
        path.reset();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
    }

    private Rect canvasRect = new Rect();
    @Override
    public void draw(Canvas canvas) {
        if (radius > 0) {
            canvas.getClipBounds(canvasRect);
            rect.set(canvasRect);
            path.reset();
            path.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.clipPath(path);
        }
        super.draw(canvas);
    }

}
