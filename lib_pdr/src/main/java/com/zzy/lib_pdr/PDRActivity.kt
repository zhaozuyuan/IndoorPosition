package com.zzy.lib_pdr

import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.sensor.IStepHandler
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.sensor.StepComputeHandler
import com.zzy.common.sensor.SysStepSensorHandler
import com.zzy.common.util.*
import kotlinx.android.synthetic.main.activity_pdr.*


class PDRActivity : AppCompatActivity() {

    private var rotationSensorHandler: RotationSensorHandler? = null
    private var stepComputeHandler: IStepHandler? = null

    private var curAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)
        supportActionBar?.hide()

        SPUtil.getValues {
            pathView.options.pdrInitDirection = getFloat(SPKeys.PDR_INIT_DIRECTION_KEY, 0f)
            pathView.options.lineInitX = 0.05f
            pathView.options.lineInitY = 0.95f
            //-x, -y
            pathView.options.initRSSIY = -3.8f
            pathView.options.initRSSIX = -1.4f
            pathView.clear()
            pathView.notifyOption(pathView.options)
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

            val sensorManager: SensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            rotationSensorHandler = rotationSensorHandler ?: RotationSensorHandler(sensorManager)
            stepComputeHandler = stepComputeHandler ?: StepComputeHandler(sensorManager)
            rotationSensorHandler?.also {
                it.setCallback { array->
                    // z x y
                    curAngle = array[0]
                }
                it.startListen()
            }
            stepComputeHandler?.also {
                it.setCallback {
                    pathView.addStep(curAngle)
                }
                it.startListen()
            }
            pathView.clear()

            btnStart.isClickable = false
            btnStart.isSelected = true
            btnStart.text = "定位中..."

//            for (i in 0..6) {
//                for (j in 0..8) {
//                    pathView.toRSSIXY(i.toFloat(), j.toFloat())
//                }
//            }
        }

        btnStop.setOnClickListener {
            rotationSensorHandler?.stopListen()
            stepComputeHandler?.stopListen()

            if (btnStart.isSelected) {
                btnStart.isClickable = true
                btnStart.isSelected = false
                btnStart.text = "开始定位"
            }
        }

        etLength.hint = "默认${pathView.options.stepString}"

        pathView.setOnClickListenerSafely(View.OnClickListener {
            pathView.options.apply {
                val scale = stepLength / stepDisplayLength.toFloat()
                val curPoint = pathView.getCurDisplayPoint()
                val rx = ((curPoint.first - lineInitX * pathView.measuredWidth.toFloat())
                        * scale / unitLengthRSSI + pathView.options.initRSSIX).round(2)
                val ry = ((lineInitY * pathView.measuredHeight.toFloat() - curPoint.second)
                        * scale / unitLengthRSSI + pathView.options.initRSSIY).round(2)
                if (LogTxtUtil.saveLine("PDR定位坐标($rx, $ry)")) {
                    toastShort("标记一次")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationSensorHandler?.stopListen()
        stepComputeHandler?.stopListen()
        //刷日志
        if (LogTxtUtil.getTxtLineCount() != 0) {
            LogTxtUtil.saveLine("<--- 退出PDR定位页面 --->\n")
            LogTxtUtil.forceFlush()
        }
    }
}