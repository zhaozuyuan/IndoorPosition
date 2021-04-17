package com.zzy.lib_combine

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.net.wifi.ScanResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.zzy.common.bean.RSSIPointBean
import com.zzy.common.bean.RSSITaskBean
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.sensor.StepComputeHandler
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.*
import com.zzy.common.widget.PullTaskDialog
import kotlinx.android.synthetic.main.activity_combine.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 融合定位页面
 */
class CombineActivity : AppCompatActivity() {

    private var taskData: RSSITaskBean? = null

    private var rssiPointBeans: List<RSSIPointBean> = emptyList()

    @Volatile
    private var curTag = 0L

    private val logTag = "combine"

    private var curAngle = 0f

    private var curRSSIDisplayXY: Pair<Float, Float> = Pair(0f, 0f)
    private var curRSSIXY: Pair<Float, Float> = Pair(0f, 0f)
    private var isFirstRSSIXY = true
    private var preRSSITime = System.currentTimeMillis()

    private var wifiInvalidCount = 0

    //目前扫描一次大概2S，再休眠1S
    private val wifiHandler by lazy {
        WifiHandler(this, Int.MAX_VALUE, 1000L)
    }

    private val rotationHandler by lazy {
        RotationSensorHandler(getSystemService(SENSOR_SERVICE) as SensorManager)
    }

    private val stepHandler by lazy {
        StepComputeHandler(getSystemService(SENSOR_SERVICE) as SensorManager)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combine)
        supportActionBar?.hide()

        val dialog = PullTaskDialog(this)
        dialog.show(supportFragmentManager, "tag")
        dialog.setOnItemDataReady { rssiTaskBean, dialogFragment ->
            cpuSync {
                rssiPointBeans = WifiRSSIUtil.parseDataOfRSSI(rssiTaskBean)
                runOnUiThread {
                    if (rssiPointBeans.isEmpty()) {
                        toastShort("数据有问题")
                        finish()
                    } else {
                        dialogFragment.dismiss()
                        dialog.dismiss()

                        pathView.options.unitLengthRSSI = rssiTaskBean.unit_length.toFloat()
//                        5教
//                        pathView.options.lineInitX = 0.05f
//                        pathView.options.lineInitY = 0.95f
//                        pathView.options.initRSSIY = -0.7f
//                        pathView.options.initRSSIX = -0.7f

                        //433
                        pathView.options.lineInitX = 0.05f
                        pathView.options.lineInitY = 0.95f
                        pathView.options.initRSSIY = -3.8f
                        pathView.options.initRSSIX = -1.4f
                        pathView.clear()
                        pathView.notifyOption(pathView.options)
                        taskData = rssiTaskBean
                    }
                }
            }
        }

        SPUtil.getValues {
            pathView.options.pdrInitDirection = getFloat(SPKeys.PDR_INIT_DIRECTION_KEY, 0f)
        }

        btnStart.setOnClickListener {
            if (etLength.text.toString().isNotEmpty()) {
                if (!etLength.text.toString().isAllNumberAndNoNull()) {
                    toastShort("请输入整数步长")
                    return@setOnClickListener
                } else {
                    pathView.options.stepLength = etLength.text.toString().toFloat()
                    pathView.options.stepString = "${pathView.options.stepLength}cm"
                    pathView.notifyOption(pathView.options)
                    pathView.clear()
                }
            }
            btnStart.isClickable = false
            btnStart.isSelected = true
            btnStart.text = "定位中..."
            isFirstRSSIXY = true

            curRSSIDisplayXY = pathView.getCurDisplayPoint()

            rotationHandler.setCallback { array->
                // z x y
                curAngle = array[0]
            }
            rotationHandler.startListen()

            stepHandler.setCallback {
                handleNewStep()
            }
            stepHandler.startListen()

            wifiPositionStart()
        }

        btnStop.setOnClickListener {
            if (btnStart.isSelected) {
                btnStart.isClickable = true
                btnStart.isSelected = false
                btnStart.text = "开始定位"

                wifiHandler.stopListen()
                stepHandler.stopListen()
                rotationHandler.stopListen()
            }
        }

        etLength.hint = "默认${pathView.options.stepLength.roundToInt()}cm"

        pathView.setOnClickListenerSafely(View.OnClickListener {
            pathView.options.apply {
                val scale = stepLength / stepDisplayLength.toFloat()
                val curPoint = pathView.getCurDisplayPoint()
                val rx = ((curPoint.first - lineInitX * pathView.measuredWidth.toFloat())
                        * scale / unitLengthRSSI  + pathView.options.initRSSIX).round(2)
                val ry = ((lineInitY * pathView.measuredHeight.toFloat() - curPoint.second)
                        * scale / unitLengthRSSI + pathView.options.initRSSIY).round(2)
                if (LogTxtUtil.saveLine("WiFi坐标(${curRSSIXY.first}, ${curRSSIXY.second}) 实际坐标($rx,$ry)")) {
                    toastShort("标记一次")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiHandler.stopListen()
        stepHandler.stopListen()
        rotationHandler.stopListen()

        //刷日志
        if (LogTxtUtil.getTxtLineCount() != 0) {
            LogTxtUtil.saveLine("<--- 退出融合定位页面 --->\n")
            LogTxtUtil.forceFlush()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun wifiPositionStart() {
        wifiHandler.startListen()
        wifiHandler.scanOnce({ }, { data ->
            if (data.isEmpty()) {
                return@scanOnce
            } else {
                handleRSSIData(data)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun handleRSSIData(data: List<List<ScanResult>>) {
        val tag = System.currentTimeMillis()
        curTag = tag
        cpuSync {
            //只用到最新的数据
            val lastBean = listOf(data.last())
            val result = WifiRSSIUtil.parseScanResult(taskData!!.wifi_tags, lastBean)[0]
            var invalidCount = 0
            result.forEach {
                if (it.level == WifiRSSIUtil.INVALID_LEVEL) {
                    runOnUiThread {
                        tvDesc.text = "当前(${curRSSIXY.first}, ${curRSSIXY.second})\n未扫描到: ${it.ssid}"
                    }
                    invalidCount++
                    if (invalidCount >= 2) {
                        return@cpuSync
                    }
                }
            }
            Log.d(logTag, "scan=$result")
            //根据结果得到位置
            var xy = WifiRSSIUtil.getCurrentXY(rssiPointBeans, result)
            if (!isFirstRSSIXY) {
                //检查速度
                xy = WifiRSSIUtil.checkNormalSpeed(
                    xy.first, xy.second,
                    curRSSIXY.first, curRSSIXY.second,
                    taskData!!.unit_length, preRSSITime
                )
            } else {
                isFirstRSSIXY = false
            }
            val rssiPoint = pathView.getRSSIXY(xy.first, xy.second)
            val curPoint = pathView.getCurDisplayPoint()
            val od = sqrt((curPoint.first - rssiPoint.first) * (curPoint.first - rssiPoint.first) +
                    (curPoint.second - rssiPoint.second) * (curPoint.second - rssiPoint.second))
            val maxOd = 2 * pathView.options.stepDisplayLength
            //必须小于两倍步长
            if (od > maxOd) {
                ++wifiInvalidCount
                //强行纠正轨迹
                if (wifiInvalidCount < 5) {
                    return@cpuSync
                } else {
                    println("pdr error")
                    //认为当前PDR不正确
                }
            }
            Log.d(logTag, "refresh: (${xy.first}, ${xy.second})")
            runOnUIThread {
                if (curTag == tag) {
                        pathView.toRSSIXY(xy.first, xy.second)
                        tvDesc.text = "刷新RSSI坐标(${xy.first},${xy.second})"
                        invalidCount = 0

                    curRSSIDisplayXY = pathView.getRSSIXY(xy.first, xy.second)
                    curRSSIXY = xy
                } else {
                    Log.i(logTag, "tag changed.")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleNewStep() {
        //pathView.addStep(curAngle)
        val finalCurAngle = curAngle
        val nextStep = pathView.nextStepXY(finalCurAngle)
        //判断策略
        val unit = pathView.options.stepLength / pathView.options.stepDisplayLength
        //换算成cm
        val nextOX = (nextStep.first - curRSSIDisplayXY.first).absoluteValue * unit
        val nextOY = (nextStep.second - curRSSIDisplayXY.second).absoluteValue * unit
        //可能wifi定位迟迟未生效
        val wifiDuration = (System.currentTimeMillis() - preRSSITime) / 1000
        val maxLength = pathView.options.unitLengthRSSI * (wifiDuration + 1)
        //if (nextOX <= maxLength && nextOY <= maxLength) {
            //step在rssi的允许范围内，允许向前一步
            pathView.addStep(finalCurAngle)
            tvDesc.text = "已向前一步\nRSSI坐标(${curRSSIXY.first},${curRSSIXY.second})"
//        } else {
//            //不在允许范围内，则检查当前坐标是否在允许范围内
//            val curXY = pathView.getCurDisplayPoint()
//            //换算成cm
//            val curOX = (curXY.first - curRSSIDisplayXY.first).absoluteValue * unit
//            val curOY = (curXY.second - curRSSIDisplayXY.second).absoluteValue * unit
//            if (curOX <= maxLength && curOY <= maxLength) {
//                //在允许范围内，则保持当前位置不动
//                tvDesc.text = "保持不动\nRSSI坐标(${curRSSIXY.first},${curRSSIXY.second})"
//            } else {
//                //跳转到相应的RSSI坐标
//                pathView.toRSSIXY(curRSSIDisplayXY.first, curRSSIDisplayXY.second)
//                tvDesc.text = "刷新RSSI坐标(${curRSSIDisplayXY.first},${curRSSIDisplayXY.second})"
//            }
//        }
    }
}