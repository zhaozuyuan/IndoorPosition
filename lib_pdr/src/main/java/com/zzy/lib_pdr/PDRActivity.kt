package com.zzy.lib_pdr

import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.sensor.IStepHandler
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.sensor.StepComputeHandler
import com.zzy.common.sensor.SysStepSensorHandler
import com.zzy.common.util.SPKeys
import com.zzy.common.util.SPUtil
import com.zzy.common.util.isAllNumberAndNoNull
import com.zzy.common.util.toastShort
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
            pathView.options.lineInitX = 0.1f
            pathView.options.lineInitY = 0.9f
            pathView.clear()
            pathView.notifyOption(pathView.options)
        }

        btnStart.setOnClickListener {
            if (!etLength.text.toString().isAllNumberAndNoNull()) {
                toastShort("请输入整数步长")
                return@setOnClickListener
            }
            pathView.options.stepLength = etLength.text.toString().toFloat()
            pathView.options.stepString = "${pathView.options.stepLength}cm"
            pathView.notifyOption(pathView.options)

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
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationSensorHandler?.stopListen()
        stepComputeHandler?.stopListen()
    }
}