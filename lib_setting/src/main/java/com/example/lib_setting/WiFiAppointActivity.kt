package com.example.lib_setting

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zzy.common.bean.WiFiAppointBean
import com.zzy.common.bean.WifiTag
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.*
import kotlinx.android.synthetic.main.activity_wifi_appoint.*
import java.lang.RuntimeException

class WiFiAppointActivity : AppCompatActivity() {

    private val wifiHandler = WifiHandler(this, 1)

    private val wifiAppointAdapter = Adapter { adapter, tag ->
        adapter.changeList {
            it.remove(tag)
        }
    }

    private val wifiScanAdapter = Adapter { adapter, tag ->
        wifiAppointAdapter.changeList {
            if (!it.contains(tag)) {
                it.add(tag)
            } else {
                toastShort("已添加")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_appoint)
        supportActionBar?.title = "指定WiFi"

        rvWifiAppoint.adapter = wifiAppointAdapter
        rvWifiAppoint.layoutManager = LinearLayoutManager(this)

        rvWifiScan.adapter = wifiScanAdapter
        rvWifiScan.layoutManager = LinearLayoutManager(this)

        wifiHandler.startListen()
        wifiHandler.scanOnce ({ list ->
            wifiScanAdapter.data = list[0]
                .filter { !it.SSID.isNullOrEmpty() }
                .map { WifiTag(it.SSID, it.BSSID) }
                .toMutableList()
        })

        cpuSync {
            val bean = SPUtil.readJsonObj(SPKeys.APPOINT_WIFI_KEY, WiFiAppointBean::class.java)
            bean?.wifi_list?.apply {
                runOnUiThread {
                    wifiAppointAdapter.data = this.toMutableList()
                }
            }
        }

        btnSave.setOnClickListener {
            val size = wifiAppointAdapter.data?.size
            if (size != null && size < 3) {
                toastShort("需指定大于2个WiFi")
            } else {
                SPUtil.putJsonString(SPKeys.APPOINT_WIFI_KEY, WiFiAppointBean(wifiAppointAdapter.data!!))
                toastShort("保存成功!")
            }
        }
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
        wifiHandler.stopListen()
    }

    class Adapter(private val clickListener: (adapter: Adapter, tag: WifiTag) -> Unit) : RecyclerView.Adapter<Holder>() {

        var data: MutableList<WifiTag>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun changeList(m: (MutableList<WifiTag>) -> Unit) {
            if (data == null) {
                data = mutableListOf()
            }
            m.invoke(data!!)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val height = (DisplayAttrUtil.getDensity() * 50f).toInt()
            val tv = Button(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                textSize = 14f
                gravity = Gravity.CENTER or Gravity.START
                setTextColor(Color.GRAY)
                isAllCaps = false
            }
            return Holder(tv)
        }

        override fun getItemCount(): Int = if (data == null) 0 else data!!.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            //不展示bssid
            holder.view.text = data!![position].ssid
            holder.view.setOnLongClickListener {
                clickListener.invoke(this, data!![position])
                return@setOnLongClickListener true
            }
        }
    }

    class Holder(val view: Button) : RecyclerView.ViewHolder(view)
}