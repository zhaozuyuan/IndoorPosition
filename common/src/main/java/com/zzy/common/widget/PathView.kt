package com.zzy.common.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.zzy.common.util.DisplayAttrUtil
import com.zzy.common.util.toastShort

/**
 * 绘制步行轨迹的View
 */
class PathView : View {

    var options = Options()
        private set

    private var isFirstDraw = true

    private var path = Path()
    private var pointPath = Path()
    private val pathPaint = Paint()
    private val pointPaint = Paint()
    private val coordinateAxisPaint = Paint()

    private var scale = 1.0f
    private var xyPoints = mutableListOf<Pair<Float, Float>>()

    //常量
    companion object {
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
     * @param angle 角度 最大360度
     */
    fun addStep(angle: Int) {

    }

    fun notifyOption(options: Options) {
        this.options = options
        coordinateAxisPaint.isAntiAlias = true
        coordinateAxisPaint.color = options.coordinateAxisColor
        coordinateAxisPaint.strokeWidth = options.coordinateAxisWidth
        pathPaint.isAntiAlias = true
        pathPaint.color = options.pathColor
        pathPaint.strokeWidth = options.pathWidth
        pathPaint.textSize = DESC_SIZE
        pointPaint.isAntiAlias = true
        pointPaint.color = options.pointColor
        pointPaint.style = Paint.Style.FILL_AND_STROKE

        invalidate()
    }

    fun clear() {
        path.reset()
        xyPoints.clear()
        pointPath.reset()
        scale = DEFAULT_SCALE
        isFirstDraw = true
    }

    override fun onDraw(canvas: Canvas) {
        checkFirst()
        drawBackground(canvas)

        if (scale != DEFAULT_SCALE) {
            canvas.save()
            canvas.scale(scale, scale, options.pathInitX * measuredWidth, options.pathInitY * measuredHeight)
        }

        canvas.drawPath(path, pathPaint)
        canvas.drawPath(pointPath, pointPaint)

        if (scale != DEFAULT_SCALE) {
            canvas.restore()
        }
    }

    private fun checkFirst() {
        if (isFirstDraw) {
            isFirstDraw = false
            val x = measuredWidth * options.pathInitX
            val y = measuredHeight * options.pathInitY
            xyPoints.add(Pair(x, y))
            path.moveTo(x, y, )
            pointPath.addCircle(measuredWidth * options.pathInitX, measuredHeight * options.pathInitY, options.pointRadius, Path.Direction.CW)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(options.backgroundColor)
        canvas.drawLine(measuredWidth / 2f, measuredHeight.toFloat(), measuredWidth / 2f, 0f, coordinateAxisPaint)
        canvas.drawLine(0f, measuredHeight / 2f, measuredWidth.toFloat(), measuredHeight / 2f, coordinateAxisPaint)
        canvas.drawText(options.stepLength, DESC_MARGIN, measuredHeight - DESC_MARGIN * 2, pathPaint)
        canvas.drawLine(20f, measuredHeight - DESC_MARGIN, DESC_MARGIN + options.onceLength * scale, measuredHeight - DESC_MARGIN, pathPaint)
    }

    private fun changeScale(scale: Float) {
        this.scale = scale
        invalidate()
        toastShort("比例变为 $scale")
    }

    class Options {

        //步长
        var stepLength = "30cm"

        //路径参数
        var pathColor = Color.BLUE
        var pathWidth = DisplayAttrUtil.getDensity() * 2f
        var pathInitX = 0.5f
        var pathInitY = 0.5f
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