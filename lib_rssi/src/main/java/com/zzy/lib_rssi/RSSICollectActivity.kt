package com.zzy.lib_rssi

import android.graphics.Color
import android.net.wifi.ScanResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zzy.common.bean.WiFiAppointBean
import com.zzy.common.bean.WifiTag
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.DisplayAttrUtil
import com.zzy.common.util.PermissionHelper
import com.zzy.common.util.SPUtil
import com.zzy.common.util.toastShort
import kotlinx.android.synthetic.main.activity_rssi_collect.*

class RSSICollectActivity : AppCompatActivity() {

    private val handler: WifiHandler by lazy {
        WifiHandler(this, 6)
    }

    private val targetWifiList: MutableList<WifiTag> = mutableListOf()

    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rssi_collect)
        supportActionBar?.hide()

        btnCollect.setOnClickListener {
            if (!it.isSelected) {
                selected(it, "收集中...")
                selected(btnLookCur)
                selected(btnPushCur)

                makeSureHandlerIsRunning()
                handler.scanOnce { data ->
                    unselected(it, "收集一次RSSI")
                    unselected(btnLookCur)
                    unselected(btnPushCur)

                    if (checkCanContinue(data)) {
                        adapter.data = getTargetResult(data)
                    } else {
                        adapter.data = emptyList()
                    }
                }
            }
        }

        rvCur.layoutManager = LinearLayoutManager(this)
        rvCur.adapter = adapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.stopListen()
    }

    private fun makeSureHandlerIsRunning() {
        if (!handler.isRunning()) {
            handler.startListen()
        }
    }

    private fun unselected(v: View, str: String = "") {
        v.isClickable = true
        v.isSelected = false
        if (str.isNotEmpty() && v is TextView) {
            v.text = str
        }
        v.setBackgroundColor(Color.parseColor("#FF3700B3"))
    }

    private fun selected(v: View, str: String = "") {
        v.isClickable = false
        v.isSelected = true
        v.setBackgroundColor(Color.GRAY)
        if (str.isNotEmpty() && v is TextView) {
            v.text = str
        }
    }

    private fun checkCanContinue(data: List<List<ScanResult>>): Boolean {
        if (data.isEmpty() || data[0].isEmpty()) {
            toastShort("收集失败")
            return false
        }
        if (targetWifiList.isEmpty()) {
            val bean = SPUtil.readJsonObj(WiFiAppointBean.SP_KEY, WiFiAppointBean::class.java)
            if (bean?.wifi_list?.size == 3) {
                targetWifiList.addAll(bean.wifi_list)
            } else {
                if (data.isNotEmpty() && data[0].size < 3) {
                    toastShort("WiFi数量小于3，不能工作!!!")
                    return false
                } else {
                    //默认是前三个WiFi
                    data[0][0].also {
                        targetWifiList.add(WifiTag(it.SSID, it.BSSID))
                    }
                    data[0][1].also {
                        targetWifiList.add(WifiTag(it.SSID, it.BSSID))
                    }
                    data[0][2].also {
                        targetWifiList.add(WifiTag(it.SSID, it.BSSID))
                    }
                }
            }
        }
        return true
    }

    private fun getTargetResult(data: List<List<ScanResult>>): List<List<ScanResult>>{
        val targetResult: MutableList<List<ScanResult>> = mutableListOf()
        data.forEach { list ->
            targetResult.add(list.filter { result ->
                targetWifiList.contains(WifiTag(result.SSID, result.BSSID))
            }.sortedBy { result ->
                //根据名字简单排个序
                result.SSID
            })
        }
        return targetResult
    }

    class Adapter : RecyclerView.Adapter<Holder>() {

        var data: List<List<ScanResult>>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val tv = Button(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                textSize = 14f
                gravity = Gravity.CENTER or Gravity.START
                setTextColor(Color.GRAY)
                isAllCaps = false
            }
            return Holder(tv)
        }

        override fun getItemCount(): Int = if (data == null) 0 else data!!.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.view.text = data!![position].let { list ->
                var str = ""
                val maxIndex = list.size - 1
                list.forEachIndexed { index, result ->
                    val ssid = if (result.SSID.length > 24) {
                        result.SSID.substring(0, 24) + "..."
                    } else result.SSID
                    str = "$str$ssid   level=${result.level} ${if (maxIndex != index) "\n" else ""}"
                }
                return@let str
            }
        }
    }

    class Holder(val view: Button) : RecyclerView.ViewHolder(view)
}