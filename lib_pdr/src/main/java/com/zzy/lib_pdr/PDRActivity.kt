package com.zzy.lib_pdr

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_pdr.*


class PDRActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)
        supportActionBar?.hide()

        btnStart.setOnClickListener {
            val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            //获取线性加速度传感器
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        }
    }
}