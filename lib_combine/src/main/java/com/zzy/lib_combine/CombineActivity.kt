package com.zzy.lib_combine

import android.annotation.SuppressLint
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.zzy.common.bean.RSSIPointBean
import com.zzy.common.bean.RSSITaskBean
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.sensor.StepComputeHandler
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.*
import com.zzy.common.widget.PullTaskDialog
import kotlinx.android.synthetic.main.activity_combine.*
import java.lang.RuntimeException
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class CombineActivity : AppCompatActivity() {

    private var taskData: RSSITaskBean? = null

    private var rssiPointBeans: List<RSSIPointBean> = emptyList()

    @Volatile
    private var curTag = 0L

    private val logTag = "combine"

    private var curAngle = 0f

    private var curRSSIDisplayXY: Pair<Float, Float> = Pair(0f, 0f)
    private var curRSSIXY: Pair<Float, Float> = Pair(0f, 0f)

    private val wifiHandler by lazy {
        WifiHandler(this, Int.MAX_VALUE)
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
                        pathView.options.unitLengthRSSI = rssiTaskBean.unit_length.toFloat()
                        taskData = rssiTaskBean
                        dialogFragment.dismiss()
                        dialog.dismiss()
                    }
                }
            }
        }

        SPUtil.getValues {
            pathView.options.pdrInitDirection = getFloat(SPKeys.PDR_INIT_DIRECTION_KEY, 0f)
        }

        btnStart.setOnClickListener {
            pathView.clear()
            btnStart.isClickable = false
            btnStart.isSelected = true
            btnStart.text = "定位中..."

            rotationHandler.setCallback { array->
                // z x y
                curAngle = array[0]
            }
            rotationHandler.startListen()

            stepHandler.setCallback {
                //pathView.addStep(curAngle)
                val finalCurAngle = curAngle
                val nextStep = pathView.nextStepXY(finalCurAngle)
                //判断策略
                val unit = pathView.options.stepLength / pathView.options.stepDisplayLength
                //换算成cm
                val nextOX = (nextStep.first - curRSSIDisplayXY.first).absoluteValue * unit
                val nextOY = (nextStep.second - curRSSIDisplayXY.second).absoluteValue * unit
                val maxLength = pathView.options.unitLengthRSSI
                if (nextOX <= maxLength && nextOY <= maxLength) {
                    //step在rssi的允许范围内，允许向前一步
                    pathView.addStep(finalCurAngle)
                    tvDesc.text = "已向前一步\nRSSI坐标(${curRSSIXY.first.roundToInt()},${curRSSIXY.second.roundToInt()})"
                } else {
                    //不在允许范围内，则检查当前坐标是否在允许范围内
                    val curXY = pathView.getCurXY()
                    //换算成cm
                    val curOX = (curXY.first - curRSSIDisplayXY.first).absoluteValue * unit
                    val curOY = (curXY.second - curRSSIDisplayXY.second).absoluteValue * unit
                    if (curOX <= maxLength && curOY <= maxLength) {
                        //在允许范围内，则保持当前位置不动
                        tvDesc.text = "保持不动\nRSSI坐标(${curRSSIXY.first.roundToInt()},${curRSSIXY.second.roundToInt()})"
                    } else {
                        //跳转到相应的RSSI坐标
                        pathView.toRSSIXY(curRSSIDisplayXY.first, curRSSIDisplayXY.second)
                        tvDesc.text = "刷新RSSI坐标(${curRSSIDisplayXY.first.roundToInt()},${curRSSIDisplayXY.second.roundToInt()})"
                    }
                }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiHandler.stopListen()
        stepHandler.stopListen()
        rotationHandler.stopListen()
    }

    @SuppressLint("SetTextI18n")
    private fun wifiPositionStart() {
        wifiHandler.startListen()
        wifiHandler.scanOnce({ }, { data ->
            if (data.isNotEmpty()) {
                val tag = System.currentTimeMillis()
                curTag = tag
                cpuSync {
                    //只用到最新的数据
                    val lastBean = listOf(data.last())
                    val result = WifiRSSIUtil.parseScanResult(taskData!!.wifi_tags, lastBean)[0]
                    result.forEach {
                        //无效值不作数
                        if (it.level == WifiRSSIUtil.INVALID_LEVEL) {
                            runOnUiThread {
                                tvDesc.text = "未扫描${it.ssid}\n当前RSSI坐标(${curRSSIXY.first.roundToInt()},${curRSSIXY.second.roundToInt()})"
                            }
                            return@cpuSync
                        }
                    }
                    Log.d(logTag, "scan=$result")
                    val xy = WifiRSSIUtil.getCurrentXY(rssiPointBeans, result)
                    Log.d(logTag, "refresh: (${xy.first}, ${xy.second})")
                    runOnUiThread {
                        if (curTag == tag) {
                            if (curRSSIXY.first != xy.first || curRSSIXY.second != xy.second) {
                                pathView.toRSSIXY(xy.first, xy.second)
                                tvDesc.text = "刷新RSSI坐标(${xy.first.roundToInt()},${xy.second.roundToInt()})"
                            }
                            curRSSIDisplayXY = pathView.getRSSIXY(xy.first, xy.second)
                            curRSSIXY = xy
                        } else {
                            Log.i(logTag, "tag changed.")
                        }
                    }
                }
            }
        })
    }
}