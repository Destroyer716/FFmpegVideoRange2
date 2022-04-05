package com.sam.video.timeline.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * 圆角遮罩
 *
 * - 可设置四个圆角
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019/3/12
 */
public class RoundRectMask extends View {

    private float density;

    public RoundRectMask(Context context) {
        super(context);
        init();
    }

    public RoundRectMask(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public RoundRectMask(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @ColorInt
    private int maskColor = Color.BLACK;

    /**
     * 最终绘制的路径
     */
    private Path path;
    /**
     * 上下左右 是否有圆角
     */
    private boolean topLeftCorner;
    private boolean topRightCorner;
    private boolean bottomRightCorner;
    private boolean bottomLeftCorner;
    private float r; //圆角半径
    private float d; //圆角直径

    protected RectF rect = new RectF();
    private Paint paint;


    public void setCorners(boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        topLeftCorner = topLeft;
        topRightCorner = topRight;
        bottomLeftCorner = bottomLeft;
        bottomRightCorner = bottomRight;
        resetRoundRect();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetRoundRect();
    }

    public void setCornerRadiusDp(float radiusDp) {
        this.r = radiusDp * density;
        this.d = this.r * 2;
    }

    protected void init() {
        density = getResources().getDisplayMetrics().density;
        path = new Path();

        r = 2 * density;
        d = 4 * density;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(maskColor);
        paint.setStyle(Paint.Style.FILL);
    }

    protected void resetRoundRect() {
        if (!hasMask()) {
            return;
        }

        rect.left = 0;
        rect.right = getWidth();
        rect.top = 0;
        rect.bottom = getHeight();

        path.reset();

        if (topLeftCorner) {
            path.moveTo(rect.left, rect.top + r);
            path.arcTo(new RectF(rect.left ,rect.top,rect.left + d,rect.top + d) ,180,90);
        } else {
            path.moveTo(rect.left, rect.top);
        }

        if (topRightCorner) {
            path.lineTo(rect.right - r, rect.top);
            path.arcTo(new RectF(rect.right - d ,rect.top,rect.right,rect.top + d) ,270,90);
        } else {
            path.lineTo(rect.right, rect.top);
        }

        if (bottomRightCorner) {
            path.lineTo(rect.right, rect.bottom-r);
            path.arcTo(new RectF(rect.right - d ,rect.bottom-d,rect.right,rect.bottom ) ,0,90);

        } else {
            path.lineTo(rect.right, rect.bottom);
        }

        if (bottomLeftCorner) {
            path.lineTo(rect.left + r, rect.bottom);
            path.arcTo(new RectF(rect.left, rect.bottom - d, rect.left + d, rect.bottom), 90, 90);

        } else {
            path.lineTo(rect.left, rect.bottom);
        }

        path.close();

        Path rectPath = new Path();
        rectPath.lineTo(getWidth(), 0);
        rectPath.lineTo(getWidth(), getHeight());
        rectPath.lineTo(0, getHeight());
        rectPath.close();
        path.op(rectPath, Path.Op.REVERSE_DIFFERENCE);
    }

    private boolean hasMask() {
        return r > 0 && (topLeftCorner || topRightCorner || bottomLeftCorner || bottomRightCorner);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!hasMask()) {
            return;
        }
        canvas.drawPath(path, paint);


    }
}

