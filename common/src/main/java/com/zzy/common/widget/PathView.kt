package com.zzy.common.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.zzy.common.util.DisplayAttrUtil
import com.zzy.common.util.toastShort
import kotlin.math.*

/**
 * 绘制步行轨迹的View
 */
class PathView : View {

    var options = Options()
        private set

    private var isFirstDraw = true

    private var linePath = Path()
    private var pointPath = Path()
    private val linePaint = Paint()
    private val pointPaint = Paint()
    private val textPaint = Paint()
    private val coordinateAxisPaint = Paint()

    private var scale = 1.0f
    private var xyPoints = mutableListOf<Pair<Float, Float>>()

    //常量
    companion object {
        private const val TAG = "PathView"

        private const val DEFAULT_SCALE = 1.0f
        private val DESC_MARGIN = DisplayAttrUtil.getDensity() * 7f
        private val DESC_SIZE = DisplayAttrUtil.getDensity() * 7f
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        notifyOption(options)
    }

    /**
     * @param angle 角度 0 -> 3.14 -> -3.14 -> 0
     */
    fun addStep(angle: Float) {
        val offsetX = cos(angle.toDouble()).toFloat() * options.stepLength
        val offsetY = sin(angle.toDouble()).toFloat() * options.stepLength
        val xy = xyPoints.last()
        val x = xy.first + offsetX
        val y = xy.second + offsetY
        Log.d(TAG, "angle=$angle, ox=$offsetX, oy=$offsetY")
        pointPath.addCircle(x, y, options.pointRadius, Path.Direction.CW)
        linePath.lineTo(x, y)
        xyPoints.add(Pair(x, y))

        //暂时按照0.5 0.5 的标准来
        val middleWidth = measuredWidth.toFloat() * options.lineInitX
        val middleHeight = measuredHeight.toFloat() * options.lineInitY
        val edgeDistanceX = (x - middleWidth).absoluteValue * scale
        val edgeDistanceY = (y - middleHeight).absoluteValue * scale
        if (scale > 0.3f && (edgeDistanceX >= middleWidth || edgeDistanceY >= middleHeight)) {
            Log.d(TAG, "x=$x, y=$y, ex=$edgeDistanceX, ey=$edgeDistanceY")
            changeScale(((scale - 0.1f) * 10).roundToInt() / 10f)
        } else {
            invalidate()
        }
    }

    /**
     * 矫正轨迹
     */
    fun correctLocus() {

    }

    fun notifyOption(options: Options) {
        this.options = options
        coordinateAxisPaint.isAntiAlias = true
        coordinateAxisPaint.color = options.coordinateAxisColor
        coordinateAxisPaint.strokeWidth = options.coordinateAxisWidth
        linePaint.isAntiAlias = true
        linePaint.color = options.lineColor
        linePaint.strokeWidth = options.lineWidth
        linePaint.style = Paint.Style.STROKE
        pointPaint.isAntiAlias = true
        pointPaint.color = options.pointColor
        pointPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.textSize = DESC_SIZE
        textPaint.color = options.lineColor
        textPaint.isAntiAlias = true

        invalidate()
    }

    fun clear() {
        linePath.reset()
        xyPoints.clear()
        pointPath.reset()
        scale = DEFAULT_SCALE
        isFirstDraw = true
        notifyOption(options)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
            MotionEvent.ACTION_CANCEL -> {

            }
            MotionEvent.ACTION_UP -> {

            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        checkFirst()
        drawBackground(canvas)

        if (scale != DEFAULT_SCALE) {
            canvas.save()
            canvas.scale(scale, scale, options.lineInitX * measuredWidth, options.lineInitY * measuredHeight)
        }

        canvas.drawPath(linePath, linePaint)
        canvas.drawPath(pointPath, pointPaint)
    }

    private fun checkFirst() {
        if (isFirstDraw) {
            isFirstDraw = false
            val x = measuredWidth * options.lineInitX
            val y = measuredHeight * options.lineInitY
            xyPoints.add(Pair(x, y))
            linePath.moveTo(x, y)
            pointPath.addCircle(x, y, options.pointRadius, Path.Direction.CW)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(options.backgroundColor)
        canvas.drawLine(measuredWidth / 2f, measuredHeight.toFloat(), measuredWidth / 2f, 0f, coordinateAxisPaint)
        canvas.drawLine(0f, measuredHeight / 2f, measuredWidth.toFloat(), measuredHeight / 2f, coordinateAxisPaint)
        canvas.drawText(options.stepString, DESC_MARGIN, measuredHeight - DESC_MARGIN * 2, textPaint)
        canvas.drawLine(20f, measuredHeight - DESC_MARGIN, DESC_MARGIN + options.onceLength * scale, measuredHeight - DESC_MARGIN, linePaint)
    }

    private fun changeScale(scale: Float) {
        this.scale = scale
        invalidate()
        toastShort("比例变为 $scale")
    }

    class Options {

        //步长
        var stepLength = DisplayAttrUtil.getDensity() * 18f
        var stepString = "65cm"

        //路径参数
        var lineColor = Color.BLUE
        var lineWidth = DisplayAttrUtil.getDensity() * 2f
        var lineInitX = 0.5f
        var lineInitY = 0.5f
        var pointRadius = DisplayAttrUtil.getDensity() * 3f
        var pointColor = Color.RED

        //平面参数
        var onceLength = (DisplayAttrUtil.getDensity() * 20f).toInt()
        var backgroundColor = Color.WHITE

        //坐标轴参数
        var coordinateAxisColor = Color.parseColor("#aaaaaa")
        var coordinateAxisWidth = 2f
    }
}