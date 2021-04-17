package com.zzy.common.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.R
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.util.DisplayAttrUtil
import com.zzy.common.util.toastShort
import java.util.*
import kotlin.math.*

/**
 * 绘制步行轨迹的View
 *
 * 主要API:
 * 1. addStep 支持前进一步
 * 2. toRSSIXY 支持一个自定义xy坐标的增加
 */
class PathView : View, Handler.Callback {

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
    private var xyPoints = LinkedList<Pair<Float, Float>>()

    private var bmp: Bitmap? = null

    private val arrowBmp by lazy {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_red_arrow)
        val scale = 14f * DisplayAttrUtil.getDensity() / bitmap.width
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    //箭头指向
    private var arrowAngle: Float = INVALID_ANGLE

    private val myLooper = Handler(Looper.getMainLooper(),this)

    private val rotationHandler by lazy {
        arrowAngle = 0f
        val handler = RotationSensorHandler(context.getSystemService(AppCompatActivity
                .SENSOR_SERVICE) as SensorManager)
        handler.setCallback {
            arrowAngle = it[0] - options.pdrInitDirection
        }
        handler
    }

    //常量
    companion object {
        private const val TAG = "PathView"

        private const val DEFAULT_SCALE = 1.0f
        //备注的margin
        private val DESC_MARGIN = DisplayAttrUtil.getDensity() * 3f
        private val DESC_SIZE = DisplayAttrUtil.getDensity() * 7f

        private const val ROTATION_MSG_WHAT = 0x1000
        private val INVALID_ANGLE = Float.MIN_VALUE
        private const val ANGLE_REFRESH_DURATION = 200L
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        notifyOption(options)
    }

    fun getCurDisplayPoint(): Pair<Float, Float> {
        return if (xyPoints.isNotEmpty()) {
            xyPoints.last()
        } else {
            Pair(0f, 0f)
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
     * 增加一步
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

        if (options.openAutoRotate && arrowAngle == INVALID_ANGLE) {
            rotationHandler.startListen()
            val refreshMsg = Message.obtain()
            refreshMsg.what = ROTATION_MSG_WHAT
            myLooper.sendMessageDelayed(refreshMsg, ANGLE_REFRESH_DURATION)
        }

        if (options.openDrawBmp) {
            //TODO 绘制背景图
            //54=17.2  433=8.6 433_2=10.8
            val oldBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_433_qinshi2)
            val height = 10.8f * options.stepDisplayLength / options.stepLength * options
                    .unitLengthRSSI
            val scale = height / oldBmp.height
            if (scale > 0f) {
                val matrix = Matrix()
                //宽了一点
                matrix.postScale(scale, scale)
                bmp = Bitmap.createBitmap(oldBmp, 0, 0, oldBmp.width, oldBmp.height, matrix, false)
            }
        }

        checkFirst()
        invalidate()
    }

    fun clear() {
        xyPoints.clear()
        scale = DEFAULT_SCALE
        isFirstDraw = true
        notifyOption(options)
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == ROTATION_MSG_WHAT) {
            val refreshMsg = Message.obtain()
            refreshMsg.what = ROTATION_MSG_WHAT
            myLooper.sendMessageDelayed(refreshMsg, ANGLE_REFRESH_DURATION)
            invalidate()
            return true
        }
        return false
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        //绘制背景
        drawBackground(canvas)

        canvas.save()

        //缩放比例
        val initX = options.lineInitX * measuredWidth
        val initY = options.lineInitY * measuredHeight
        if (scale != DEFAULT_SCALE) {
            canvas.scale(scale, scale, initX, initY)
            canvas.save()
        }

        //TODO 绘制背景图
        bmp?.also {
            //空白补偿
            val tx = initX// - 5 * DisplayAttrUtil.getDensity()
            val ty = initY// + 5 * DisplayAttrUtil.getDensity()
            val rect = RectF(tx, ty - it.height, tx + it.width, ty)
            canvas.drawBitmap(it, null, rect, null)
        }

        //绘制轨迹
        canvas.drawPath(linePath, linePaint)
        pointPaint.color = options.pointColor
        canvas.drawPath(pointPath, pointPaint)
        pointPaint.color = Color.RED

        //最后来绘制最后一个点
        if (options.openAutoRotate) {
            val ix = xyPoints.last().first
            val iy = xyPoints.last().second
            val width2 = arrowBmp.width / 2f
            val rect = RectF(ix - width2, iy - width2, ix + width2, iy + width2)
            canvas.save()
            canvas.rotate(arrowAngle + 90, ix, iy)
            canvas.drawBitmap(arrowBmp, null, rect, null)
        } else {
            canvas.drawCircle(xyPoints.last().first, xyPoints.last().second, options.pointRadius, pointPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (arrowAngle != INVALID_ANGLE) {
            rotationHandler.stopListen()
        }
        myLooper.removeMessages(ROTATION_MSG_WHAT)
    }

    private fun checkFirst() {
        if (isFirstDraw) {
            isFirstDraw = false
            options.apply {
                val scale = stepDisplayLength / stepLength
                val x = measuredWidth * lineInitX - initRSSIX * unitLengthRSSI * scale
                val y = measuredHeight * lineInitY + initRSSIY * unitLengthRSSI * scale
                xyPoints.add(Pair(x, y))
            }
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
        //线太多了
        //linePath.lineTo(x, y)

        while (xyPoints.size != 0 && xyPoints.size >= options.pointCount) {
            xyPoints.pop()
        }
        xyPoints.offer(Pair(x, y))

        linePath = Path()
        pointPath = Path()
        xyPoints.forEachIndexed { index, pair ->
            if (index == 0) {
                linePath.moveTo(pair.first, pair.second)
                pointPath.addCircle(pair.first, pair.second, options.pointRadius, Path.Direction.CW)
            } else {
                linePath.lineTo(pair.first, pair.second)
                if (index != xyPoints.lastIndex){
                    //最后一个点需要单独绘制
                    pointPath.addCircle(pair.first, pair.second, options.pointRadius, Path.Direction.CW)
                }
            }
        }
    }

    class Options {

        //真实步长 cm
        var stepLength = 50f
        var stepString = "50cm"

        //路径参数
        var lineColor = Color.BLUE
        var lineWidth = DisplayAttrUtil.getDensity() * 1.2f
        //初始位置的百分比
        var lineInitX = 0.5f
        var lineInitY = 0.5f
        //点参数
        var pointRadius = DisplayAttrUtil.getDensity() * 1.5f
        var pointColor = Color.BLACK
        //pdr 初始角度
        var pdrInitDirection = 0.0f

        //每一步屏幕长度 5教/4 寝室/2 控制屏幕的比例
        val stepDisplayLength
            get() = (DisplayAttrUtil.getDensity() * stepLength / 2.45f).roundToInt()
        var backgroundColor = Color.WHITE

        //坐标轴参数
        var coordinateAxisColor = Color.parseColor("#bbbbbb")
        var coordinateAxisWidth = 1.5f

        //---------后面才引入的RSSI坐标------------//

        //lineInitX,lineInitY 初始点对应的RSSI坐标，实际上这就是原点坐标
        var initRSSIX = 0f
        var initRSSIY = 0f
        //RSSI坐标单位长度 cm
        var unitLengthRSSI = 100f

        //最后一个点自动旋转
        var openAutoRotate = true

        //绘制背景图
        var openDrawBmp = true

        //点的个数
        var pointCount = 100
    }
}