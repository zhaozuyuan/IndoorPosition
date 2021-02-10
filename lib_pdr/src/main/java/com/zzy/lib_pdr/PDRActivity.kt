package com.zzy.lib_pdr

import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.sensor.IStepHandler
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.sensor.StepComputeHandler
import com.zzy.common.sensor.SysStepSensorHandler
import kotlinx.android.synthetic.main.activity_pdr.*


class PDRActivity : AppCompatActivity() {

    private var rotationSensorHandler: RotationSensorHandler? = null
    private var stepComputeHandler: IStepHandler? = null

    private var curAngle = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)
        supportActionBar?.hide()

        btnStart.setOnClickListener {
            val sensorManager: SensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            rotationSensorHandler = rotationSensorHandler ?: RotationSensorHandler(sensorManager)
            stepComputeHandler = stepComputeHandler ?: StepComputeHandler(sensorManager)
            rotationSensorHandler?.also {
                it.setCallback { array->
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
        }

        btnStop.setOnClickListener {
            rotationSensorHandler?.stopListen()
            stepComputeHandler?.stopListen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationSensorHandler?.stopListen()
        stepComputeHandler?.stopListen()
    }
}