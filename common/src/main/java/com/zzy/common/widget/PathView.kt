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
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.util.DisplayAttrUtil
import com.zzy.common.util.toastShort
import kotlin.math.*

/**
 * 绘制步行轨迹的View
 * 1. addStep 支持前进一步
 * 2. toRSSIXY 支持一个自定义xy坐标的增加
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
        //备注的margin
        private val DESC_MARGIN = DisplayAttrUtil.getDensity() * 7f
        private val DESC_SIZE = DisplayAttrUtil.getDensity() * 7f
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        notifyOption(options)
    }

    fun getCurXY(): Pair<Float, Float> {
        if (xyPoints.isNotEmpty()) {
            return xyPoints.last()
        } else {
            return Pair(0f, 0f)
        }
    }

    /**
     * 预坐标
     */
    fun nextStepXY(angle: Float): Pair<Float, Float> {
        val realAngle = 360f - (angle - options.pdrInitDirection)
        val offsetX = cos(realAngle / 360f * (2f * Math.PI.toFloat())) * options.stepDisplayLength
        //屏幕的y轴是反的
        val offsetY = -sin(realAngle / 360f * (2f * Math.PI.toFloat())) * options.stepDisplayLength
        val xy = xyPoints.last()
        val x = xy.first + offsetX
        val y = xy.second + offsetY
        return Pair(x, y)
    }

    /**
     * 预坐标
     */
    fun getRSSIXY(toX: Float, toY: Float): Pair<Float, Float>  {
        //转换成屏幕距离
        val scale = options.unitLengthRSSI / options.stepLength * options.stepDisplayLength
        val displayX = (toX - options.initRSSIX) * scale + measuredWidth * options.lineInitX
        //y轴是反的
        val displayY = measuredHeight * options.lineInitY - (toY - options.initRSSIY) * scale
        return Pair(displayX, displayY)
    }

    /**
     * @param angle 角度 0 -> 1 -> -1 -> 0
     */
    fun addStep(angle: Float) {
        val xy = nextStepXY(angle)
        Log.d(TAG, "angle=$angle, ox=${xy.first}, oy=${xy.second}")
        addNewPoint(xy.first, xy.second)

        if (!autoScale(xy.first, xy.second)) {
            invalidate()
        }
    }

    /**
     * 到一个rssi坐标
     */
    fun toRSSIXY(toX: Float, toY: Float) {
        val xy = getRSSIXY(toX, toY)
        addNewPoint(xy.first, xy.second)

        if (!autoScale(xy.first, xy.second)) {
            invalidate()
        }
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
        pointPaint.color = options.pointColor
        canvas.drawPath(pointPath, pointPaint)
        pointPaint.color = Color.RED
        canvas.drawCircle(xyPoints.last().first, xyPoints.last().second, options.pointRadius, pointPaint)
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
        canvas.drawLine(measuredWidth * options.lineInitX, measuredHeight.toFloat(), measuredWidth * options.lineInitX, 0f, coordinateAxisPaint)
        canvas.drawLine(0f, measuredHeight * options.lineInitY, measuredWidth.toFloat(), measuredHeight * options.lineInitY, coordinateAxisPaint)
        canvas.drawText(options.stepString, DESC_MARGIN, measuredHeight - DESC_MARGIN * 2, textPaint)
        canvas.drawLine(DESC_MARGIN, measuredHeight - DESC_MARGIN, DESC_MARGIN + options.stepDisplayLength * scale, measuredHeight - DESC_MARGIN, linePaint)
    }

    private fun autoScale(x: Float, y: Float): Boolean {
        //暂时按照0.5 0.5 的标准来
        val middleWidth = measuredWidth.toFloat() * options.lineInitX
        val middleHeight = measuredHeight.toFloat() * options.lineInitY
        val edgeDistanceX = (x - middleWidth) * scale
        val edgeDistanceY = (y - middleHeight) * scale

        //最小比例
        return if (scale >= 0.2f) {
            if ((edgeDistanceX < 0 && edgeDistanceX < -middleWidth)
                || (edgeDistanceX > 0 && edgeDistanceX > measuredWidth - middleWidth)
                || (edgeDistanceY < 0 && edgeDistanceY < -middleHeight)
                || (edgeDistanceY > 0 && edgeDistanceY > measuredHeight - middleHeight)) {
                Log.d(TAG, "x=$x, y=$y, ex=$edgeDistanceX, ey=$edgeDistanceY")
                changeScale(((scale - 0.2f) * 10).roundToInt() / 10f)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    private fun changeScale(scale: Float) {
        this.scale = scale
        invalidate()
        toastShort("比例变为 $scale")
    }

    private fun addNewPoint(x: Float, y: Float) {
        pointPath.addCircle(x, y, options.pointRadius, Path.Direction.CW)
        //线太多了
        //linePath.lineTo(x, y)
        xyPoints.add(Pair(x, y))
    }

    class Options {

        //真实步长 cm
        var stepLength = 50f
        var stepString = "50cm"

        //路径参数
        var lineColor = Color.BLUE
        var lineWidth = DisplayAttrUtil.getDensity() * 2f
        //初始位置的百分比
        var lineInitX = 0.5f
        var lineInitY = 0.5f
        //点参数
        var pointRadius = DisplayAttrUtil.getDensity() * 3f
        var pointColor = Color.BLACK
        //pdr 初始角度 -3.14~3.14
        var pdrInitDirection = 0.0f

        //每一步屏幕长度
        var stepDisplayLength = (DisplayAttrUtil.getDensity() * 20f).toInt()
        var backgroundColor = Color.WHITE

        //坐标轴参数
        var coordinateAxisColor = Color.parseColor("#bbbbbb")
        var coordinateAxisWidth = 1.5f

        //---------后面才引入的RSSI坐标------------//

        //lineInitX,lineInitY 初始点对应的RSSI坐标
        var initRSSIX = 0f
        var initRSSIY = 0f
        //RSSI坐标单位长度 cm
        var unitLengthRSSI = 0f
    }
}