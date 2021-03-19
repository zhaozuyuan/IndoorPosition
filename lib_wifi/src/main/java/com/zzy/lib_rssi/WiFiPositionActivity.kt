package com.zzy.lib_rssi

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.zzy.common.bean.RSSIPointBean
import com.zzy.common.bean.RSSITaskBean
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.*
import com.zzy.common.widget.PullTaskDialog
import kotlinx.android.synthetic.main.activity_wifi_position.*
import kotlin.math.roundToInt

class WiFiPositionActivity : AppCompatActivity() {

    private lateinit var taskData: RSSITaskBean

    private var rssiPointBeans: List<RSSIPointBean> = emptyList()

    @Volatile
    private var curTag = 0L

    private val logTag = "wifi_pt"

    private val wifiHandler by lazy {
        WifiHandler(this, Int.MAX_VALUE)
    }

    private var curXY = Pair(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_position)
        supportActionBar?.title = "WiFi定位"

        val dialog = PullTaskDialog(this)
        dialog.show(supportFragmentManager, "tag")
        dialog.setOnItemDataReady { bean, childDialog ->
            cpuSync {
                rssiPointBeans = WifiRSSIUtil.parseDataOfRSSI(bean)
                runOnUiThread {
                    if (rssiPointBeans.isEmpty()) {
                        toastShort("数据有问题")
                        finish()
                    } else {
                        taskData = bean
                        childDialog.dismiss()
                        dialog.dismiss()
                        pathView.options.unitLengthRSSI = taskData.unit_length.toFloat()
                        start()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiHandler.stopListen()
    }

    @SuppressLint("SetTextI18n")
    private fun start() {
        wifiHandler.startListen()
        wifiHandler.scanOnce({ }, { data ->
            if (data.isNotEmpty()) {
                val tag = System.currentTimeMillis()
                curTag = tag
                cpuSync {
                    //只用到最新的数据
                    val lastBean = listOf(data.last())
                    val result = WifiRSSIUtil.parseScanResult(taskData.wifi_tags, lastBean)[0]
                    result.forEach {
                        //无效值不作数
                        if (it.level == WifiRSSIUtil.INVALID_LEVEL) {
                            runOnUiThread {
                                tvDesc.text = "当前(${curXY.first}, ${curXY.second})\n未扫描到: ${it.ssid}"
                            }
                            return@cpuSync
                        }
                    }
                    Log.d(logTag, "scan=$result")
                    val xy = WifiRSSIUtil.getCurrentXY(rssiPointBeans, result)
                    Log.d(logTag, "refresh: (${xy.first}, ${xy.second})")
                    runUIThread {
                        if (curTag == tag) {
                            pathView.toRSSIXY(xy.first, xy.second)
                            curXY = Pair(xy.first.roundToInt(), xy.second.roundToInt())
                            tvDesc.text = "坐标: (${curXY.first}, ${curXY.second})"
                        } else {
                            Log.i(logTag, "tag changed.")
                        }
                    }
                }
            }
        })
    }
}