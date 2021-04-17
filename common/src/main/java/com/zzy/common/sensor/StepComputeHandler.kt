package com.zzy.common.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * create by zuyuan on 2021/2/5
 * 利用加速度传感器制作的计步器
 */
class StepComputeHandler(private val manager: SensorManager)
    : SensorEventListener, ISensorHandler, IStepHandler {

    private val isRegistered = AtomicBoolean(false)

    //上一次最优的加速度
    private var preOptimalGravity = 0.0
    //上一次实际读的的加速度
    private var preRealGravity = 0.0
    //上一次计步的时间
    private var preNewStepTime = 0L

    //加速度趋势是否在上升
    private var trendIsUp = false
    private var trendUpCount = 0

    //这一次的波谷
    private var curTrough = 0.0

    //对gravity进行卡尔曼滤波，得到最优值 preOptimalGravity
    private val kalmanComputer = KalmanComputer()

    private var onNewStep: (() -> Unit)? = null

    companion object {
        private const val DATA_TAG = "stepHandler_data"
        private const val TAG = "stepHandler_tag"

        //最小触发的波峰
        private const val MIN_CREST = 9.75
        //最小触发的波峰和波谷差
        private const val MIN_CREST_TROUGH_DIFF = 1.8
        //最小触发趋势增加次数
        private const val MIN_TREND_UP_COUNT = 10
        //两步间隔最短的时间
        private const val MIN_STEP_TIME = 250L

        //期望出现的Gravity增量
        private const val EXPECT_INCREMENT = 0.5
    }

    override fun setCallback(onNewStep: () -> Unit) {
        this.onNewStep = onNewStep
    }

    override fun startListen() {
        val canRegister: Boolean = isRegistered.compareAndSet(false, true)
        if (!canRegister) {
            throw RuntimeException("SensorEventListener has be registered !!!")
        }
        val sensor: Sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun stopListen() {
        if (isRegistered.compareAndSet(true, false)) {
            kalmanComputer.clear()
            manager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.i(DATA_TAG, "精度改变 value=$p1")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x: Float = event.values[0]
        val y: Float = event.values[1]
        val z: Float = event.values[2]
        //加速度数据
        val gravity: Double = sqrt((x * x + y * y + z * z).toDouble())

        if (preOptimalGravity <= 0) {
            preOptimalGravity = gravity
        } else {
            if (checkStepValid(gravity)) {
                Log.d(TAG, "new step.")
                onNewStep?.invoke()
            }
        }
    }

    private fun checkStepValid(gravity: Double): Boolean {
        //静止时的误差直接return
        if ((gravity - preRealGravity).absoluteValue < 0.2) {
            //Log.d(DATA_TAG, "invalid, gravity=$gravity")
            return false
        }

        val preStatus: Boolean = trendIsUp
        var result = false
        var curGravity = gravity
        if (curGravity >= preRealGravity) {
            curGravity = kalmanComputer.getOptimalSolution(preOptimalGravity + EXPECT_INCREMENT, curGravity)
            //记录波谷
            if (!preStatus) {
                curTrough = preOptimalGravity
            }
            trendIsUp = true
            trendUpCount++
        } else {
            curGravity = kalmanComputer.getOptimalSolution(preOptimalGravity - EXPECT_INCREMENT, curGravity)
            val duration: Long = System.currentTimeMillis() - preNewStepTime
            val diff: Double = preOptimalGravity - curTrough
            //过滤下日志
            if (preStatus && duration > MIN_STEP_TIME) {
                Log.d(TAG, "trough=${curTrough.toFloat()}, crest=${preOptimalGravity.toFloat()}, " +
                        "diff=${diff.toFloat()}, trendUpCount=$trendUpCount, duration=$duration")
            }
            //上一次在加速
            if (preStatus
                    //间隔时间必须大于
                    && duration > MIN_STEP_TIME
                    //波峰必须大于
                    && preOptimalGravity > MIN_CREST
                    //加速次数大于
                    && trendUpCount > MIN_TREND_UP_COUNT
                    //波峰必须大于波谷
                    && diff > MIN_CREST_TROUGH_DIFF) {
                preNewStepTime = System.currentTimeMillis()
                result = true
            }
            trendIsUp= false
            trendUpCount = 0
        }
        Log.d(DATA_TAG, "isUp=$trendIsUp, preGravity=$preRealGravity, gravity=$gravity, optimalGravity=$curGravity")
        preOptimalGravity = curGravity
        preRealGravity = gravity
        return result
    }

    /**
     * 卡尔曼滤波算法
     * 总结: 根据本次的估计值X和实际测量值Z，结合卡尔曼增益K，得到最优的值。
     * 描述公式: Target = X + K(Z - X)
     * 描述: 估计值和实测值的方差情况，来得到K，两个值中哪一个值的方差越小，它所占的权重也就更大。
     */
    class KalmanComputer {

        //上次的最优解偏差
        private var preOptimalSolutionDeviation = 0.0

        companion object {
            //预测的不确定度
            private const val UNCERTAIN_VALUE = 2.0
        }

        /**
         * 得到本次的最优解
         */
        fun getOptimalSolution(expectValue: Double, realValue: Double): Double {
            //高斯噪声偏差
            val gaussianNoise = sqrt(preOptimalSolutionDeviation * preOptimalSolutionDeviation +
                    UNCERTAIN_VALUE * UNCERTAIN_VALUE)
            val kg = sqrt(gaussianNoise * gaussianNoise / (gaussianNoise * gaussianNoise +
                    UNCERTAIN_VALUE * UNCERTAIN_VALUE))
            val optimalValue = expectValue + kg * (realValue - expectValue)
            preOptimalSolutionDeviation = sqrt((1 - kg) * gaussianNoise * gaussianNoise)
            return optimalValue

        }

        fun clear() {
            preOptimalSolutionDeviation = 0.0
        }
    }
}