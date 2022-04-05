package com.sam.video.timeline.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.sam.video.R
import com.sam.video.util.dp2px
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong


/**
 * 时间刻度尺
 * 最小高度20dp
 *
 * @author SamWang(33691286@qq.com)
 * @date 2019-08-16
 */
class RulerView @JvmOverloads constructor(
    paramContext: Context, paramAttributeSet: AttributeSet? = null,
    paramInt: Int = 0
) : View(paramContext, paramAttributeSet, paramInt),
    TimeLineBaseValue.TimeLineBaseView {
    override fun updateTime() {
        invalidate()
    }

    private val dp1: Float by lazy(LazyThreadSafetyMode.NONE) {
        TypedValue.applyDimension(
            1,
            1.0f,
            resources.displayMetrics
        )
    }
    private var linearGradient: LinearGradient? = null
    private val textPaint: Paint = Paint()

    /** 每小刻度的 单位像素长度 */
    private var rulerPxUnit = 1.0f
    /** 尺子的每个小刻度 单位时间长度 */
    private var rulerTimeUnit: Long = 1
    /** 大刻度的默认间隔 */
    private var standPxPs: Float = context.dp2px(64f)
//    /** 刻度辅助线的高度 */
//    private var marginHeight: Float = 0.toFloat()

    /** 基础数据 */
    override var timeLineValue: TimeLineBaseValue? = null
        set(value) {
            field = value
            invalidate()
        }

    private var numberStr = StringBuilder()
    private val white30Color: Int = ContextCompat.getColor(context,
        R.color.white30
    )
    private val white50Color: Int = ContextCompat.getColor(context,
        R.color.white50
    )

    init {
        textPaint.color = white30Color
        textPaint.strokeWidth = this.dp1
        textPaint.textSize = context.dp2px(8f)
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDraw(paramCanvas: Canvas) {
        super.onDraw(paramCanvas)
        val timeLineValue = this.timeLineValue ?: return

        if (timeLineValue.pxInSecond <= 0.0f) {
            return
        }

        var startX = measuredWidth / 2.0f //默认从中间开始

        //现在要画的刻度 单位毫秒
        var currentRuleFlag = (startX / timeLineValue.pxInSecond * 1000f).toLong() //从0开始中间位置对应的刻度时间

        currentRuleFlag = if (timeLineValue.time <= currentRuleFlag) {
            0L //时间在默认位置的左边，从0开始画
        } else {
            //时间在默认位置的右边，则起始的位置为右边的偏移量，起始位置调整为整数
            (ceil(((timeLineValue.time - currentRuleFlag) / this.rulerTimeUnit).toDouble()) * this.rulerTimeUnit).toLong()
        }

        startX -= (timeLineValue.time - currentRuleFlag).toFloat() * timeLineValue.pxInSecond / 1000f

        var minute: Int
        while (startX < measuredWidth //画完一屏
//            && (timeLineValue.duration <= 0L || currentRuleFlag < timeLineValue.duration) //画完和时间轴一样的长度
        ) {

            if (currentRuleFlag / this.rulerTimeUnit % 5L == 0L) {
                textPaint.color = white50Color
                //5格一个大刻度，带数字的线
                paramCanvas.drawLine(
                    startX,
                      0f,
                    startX,
                      this.dp1 *5.0f,
                    textPaint
                )

                val second = currentRuleFlag.toFloat() / 1000.0f % 60.0f
                val ms = (1000.0f * second).roundToInt() //
                if (ms % 1000 == 0) {
                    //整秒
                    numberStr.clear()
                    numberStr.append(ms / 1000)
                    numberStr.append("s")
                } else {
                    //x.x s
                    numberStr.clear()
                    numberStr.append((second * 100f).roundToInt() / 100.0f)
                    if (numberStr.indexOf(".") > 0) {
                        while ((numberStr.indexOf(".") > 0)
                            && (numberStr.endsWith('0') || numberStr.endsWith('.'))
                        ) {
                            numberStr.deleteCharAt(numberStr.lastIndex)
                        }
                    }
                    numberStr.append("s")
                }

                minute = (currentRuleFlag / 60_000L).toInt()
                if (minute > 0) { //满分钟
                    numberStr.insert(0, "${minute}m ")
                }
                paramCanvas.drawText(
                    numberStr.toString(),
                    startX,
                      this.dp1 * 16.0f,
                    textPaint
                )

//                paramCanvas.drawLine(startX, dp1 * 20, startX, dp1 * 20 + this.marginHeight, textPaint)

            } else {
                textPaint.color = white30Color
                paramCanvas.drawLine(
                    startX,
                     0f,
                    startX,
                     this.dp1 * 3,
                    textPaint
                )
            }

            currentRuleFlag += this.rulerTimeUnit
            startX += this.rulerPxUnit
        }

    }

    fun setMarginHeight(paramInt: Int) {
//        marginHeight = paramInt.toFloat()
//        linearGradient = LinearGradient(
//            0.0f,
//            marginHeight / 2.0f,
//            0.0f,
//            0.0f,
//            colorGradient,
//            0,
//            Shader.TileMode.MIRROR
//        )
//        paintE.shader = linearGradient
    }

    /** 更新刻度单位 自动限制在标准 */
    override fun scaleChange() {
        val timeLineBaseValue = timeLineValue ?: return

        var pxInSecondScaled = timeLineBaseValue.pxInSecond

        //限制尺子每格的范围在标准的1-2倍之间
        if (pxInSecondScaled < standPxPs) {
            while (true) {
                if (pxInSecondScaled >= standPxPs) {
                    break
                }
                pxInSecondScaled *= 2.0f
            }
        }
        if (pxInSecondScaled >= standPxPs * 2.0f) {
            while (true) {
                if (pxInSecondScaled < standPxPs * 2.0f) {
                    break
                }
                pxInSecondScaled /= 2.0f
            }
        }

        //经过处理后的每大格一定是1s * 2^n （n可能为负），这样刻度就可以用比较整的数
        this.rulerPxUnit = pxInSecondScaled / 5.0f
        this.rulerTimeUnit = (this.rulerPxUnit * 1000f / timeLineBaseValue.pxInSecond).roundToLong()
        invalidate()
    }

}