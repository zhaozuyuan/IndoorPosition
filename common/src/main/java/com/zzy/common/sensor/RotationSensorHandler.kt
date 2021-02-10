package com.zzy.common.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * create by zuyuan on 2021/2/4
 * desc: 监听和处理方向的逻辑类
 */
class RotationSensorHandler(private val manager: SensorManager) : SensorEventListener,ISensorHandler {

    private val isRegistered = AtomicBoolean(false)

    @Volatile
    private var callback: ((FloatArray) -> Unit) ?= null

    companion object {
        private const val TAG = "rotationHandler"
    }

    fun setCallback(callback: (FloatArray) -> Unit) {
        this.callback = callback
    }

    override fun startListen() {
        val canRegister: Boolean = isRegistered.compareAndSet(false, true)
        if (!canRegister) {
            throw RuntimeException("SensorEventListener has be registered !!!")
        }
        //旋转矢量传感器
        val sensor: Sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        //定位用normal的刷新率足够了
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }


    override fun stopListen() {
        if (isRegistered.compareAndSet(true, false)) {
            manager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(p0: Sensor, p1: Int) {
        Log.i(TAG, "精度改变 value=$p1")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rotationMatrix = FloatArray(16)
        //将旋转矢量转换成旋转矩阵，支持长度8和16的矩阵
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val coordinateSystemMatrix = FloatArray(16)
        //将旋转矩阵重映射为坐标系矩阵，输入和输出矩阵长度保持一致
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X,
                SensorManager.AXIS_Y, coordinateSystemMatrix)
        val orientationArray = FloatArray(3)
        //将坐标系矩阵转换成zxy轴旋转角度数组
        SensorManager.getOrientation(coordinateSystemMatrix, orientationArray)
        //   0 -> 3.14 -> -3.14 -> 0
        Log.i(TAG, "围绕z旋转=${orientationArray[0]}, " +
                        "围绕x旋转=${orientationArray[1]} " +
                        "围绕y旋转=${orientationArray[2]} ")
        callback?.invoke(orientationArray)
    }
}