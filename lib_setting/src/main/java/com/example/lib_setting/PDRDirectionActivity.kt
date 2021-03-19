package com.example.lib_setting

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zzy.common.sensor.RotationSensorHandler
import com.zzy.common.util.SPKeys
import com.zzy.common.util.SPUtil
import com.zzy.common.util.toastShort
import kotlinx.android.synthetic.main.activity_pdr_direction.*
import kotlin.math.asin
import kotlin.math.roundToInt

class PDRDirectionActivity : AppCompatActivity() {

    private val rotationHandler by lazy {
        RotationSensorHandler(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
    }

    private var curAngle = 0f

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr_direction)
        supportActionBar?.title = "初始PDR方向"

        rotationHandler.setCallback {
            curAngle = it[0]
            val rotationView = 360 - curAngle.roundToInt()
            ivArrow.rotation = rotationView.toFloat()
            tvDirection.text = "${rotationView}°"
        }

        rotationHandler.startListen()

        btnOK.setOnClickListener {
            SPUtil.putValues {
                putFloat(SPKeys.PDR_INIT_DIRECTION_KEY, curAngle)
                toastShort("保存成功")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationHandler.stopListen()
    }
}