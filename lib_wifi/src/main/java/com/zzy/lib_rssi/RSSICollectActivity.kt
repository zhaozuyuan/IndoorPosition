package com.zzy.lib_rssi

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.wifi.ScanResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zzy.common.bean.*
import com.zzy.common.net.HttpUtil
import com.zzy.common.sensor.WifiHandler
import com.zzy.common.util.*
import kotlinx.android.synthetic.main.activity_rssi_collect.*

class RSSICollectActivity : AppCompatActivity() {

    private val handler: WifiHandler by lazy {
        WifiHandler(this, 6)
    }

    private val targetWifiList: MutableList<WifiTag> = mutableListOf()

    private val adapter = Adapter()

    //<x-y,levels>
    private val rssiDataMap: LinkedHashMap<String, List<RSSIData>?> = LinkedHashMap()
    private val pushBean: RSSITaskBean = RSSITaskBean(6, -1)

    companion object {
        private const val PUSH_TAG = "push_tag"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rssi_collect)
        supportActionBar?.hide()

        val bean = SPUtil.readJsonObj(SPKeys.APPOINT_WIFI_KEY, WiFiAppointBean::class.java)
        if (bean != null && bean.wifi_list.isNotEmpty()) {
            targetWifiList.addAll(bean.wifi_list)
        }

        btnCollect.setOnClickListener {
            if (!it.isSelected) {
                if (targetWifiList.isEmpty()) {
                    toastShort("请在设置中指定WiFi")
                    return@setOnClickListener
                }
                if (etTaskName.text.isEmpty()) {
                    toastShort("任务名不能为空")
                    return@setOnClickListener
                }
                if (!etUnitLength.text.toString().isAllNumberAndNoNull()) {
                    toastShort("请输入正确的单位长度")
                    return@setOnClickListener
                }
                if (!etX.text.toString().isAllNumberAndNoNull() || !etY.text.toString().isAllNumberAndNoNull()) {
                    toastShort("请输入正确的坐标")
                   return@setOnClickListener
                }

                selectedBtn(it, "收集中...")
                selectedBtn(btnLookCur)
                selectedBtn(btnPushCur)
                //只能够输入一次
                if (!etTaskName.isSelected) {
                    selectedET(etTaskName)
                    selectedET(etUnitLength)
                    pushBean.task_name = etTaskName.text.toString()
                    pushBean.unit_length = etUnitLength.text.toString().toInt()
                }
                selectedET(etX)
                selectedET(etY)

                makeSureHandlerIsRunning()
                val x = etX.text.toString().toInt()
                val y = etY.text.toString().toInt()
                val key = "${x}*${y}"
                handler.scanOnce({ data ->
                    unselectedBtn(it, "收集一次RSSI")
                    unselectedBtn(btnLookCur)
                    unselectedBtn(btnPushCur)
                    unselectedET(etX)
                    unselectedET(etY)

                    if (checkCanContinue(data)) {
                        val lastData = WifiRSSIUtil.parseScanResult(targetWifiList, data)
                        adapter.data = lastData
                        val bssidMap = mutableMapOf<String, RSSIData>()
                        lastData.forEach { list ->
                            list.forEach { bean ->
                                if (bssidMap[bean.bssid] == null) {
                                    bssidMap[bean.bssid] = RSSIData(
                                            bean.bssid, bean.ssid, x, y, mutableListOf(bean.level))
                                } else {
                                    (bssidMap[bean.bssid]!!.levels as MutableList).add(bean.level)
                                }
                            }
                        }
                        rssiDataMap[key] = bssidMap.values.toList()
                    } else {
                        adapter.data = emptyList()
                    }
                }, { data ->
                    //进度展示
                    if (data.size != adapter.data?.size) {
                        if (checkCanContinue(data)) {
                            adapter.data = WifiRSSIUtil.parseScanResult(targetWifiList, data)
                        } else {
                            adapter.data = emptyList()
                        }
                    }
                })
            }
        }

        rvCur.layoutManager = LinearLayoutManager(this)
        rvCur.adapter = adapter

        btnPushCur.setOnClickListener {
            if (rssiDataMap.isEmpty()) {
                toastShort("扫描次数为0!")
                return@setOnClickListener
            }
            ioSync {
                pushBean.wifi_tags = targetWifiList
                pushBean.wifi_count = targetWifiList.size
                val dataList = mutableListOf<RSSIData>()
                rssiDataMap.forEach {
                    val list: List<RSSIData> = it.value ?: return@ioSync
                    dataList.addAll(list)
                    Log.d(PUSH_TAG, list.joinTo(StringBuilder()).toString())
                }
                pushBean.rssi_data = dataList
                val netResult  = HttpUtil.pushRSSITaskData(pushBean)
                if (netResult.code == NetResult.SUCCESS_CODE) {
                    toastShort("上传成功")
                } else {
                    toastShort(netResult.msg)
                }
            }
        }

        btnLookCur.setOnClickListener { view ->
            val dataList: MutableList<List<WifiBean>> = mutableListOf()
            rssiDataMap.forEach { entry ->
                val data = entry.value
                if (data.isNullOrEmpty()) {
                    return@forEach
                }
                val x = data[0].x
                val y = data[0].y
                val bean = XYBean(x, y)
                dataList.add(listOf(bean))
                data.forEach { rssiData ->
                    dataList.add(listOf(LevelsBean(rssiData.wifi_ssid,
                            rssiData.wifi_bssid, rssiData.levels.toString())))
                }
            }
            adapter.data = dataList
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
        handler.stopListen()
    }

    private fun makeSureHandlerIsRunning() {
        if (!handler.isRunning()) {
            handler.startListen()
        }
    }

    private fun unselectedBtn(v: View, str: String = "") {
        v.isClickable = true
        v.isSelected = false
        if (str.isNotEmpty() && v is TextView) {
            v.text = str
        }
        v.setBackgroundColor(Color.parseColor("#FF3700B3"))
    }

    private fun selectedBtn(v: View, str: String = "") {
        v.isClickable = false
        v.isSelected = true
        v.setBackgroundColor(Color.GRAY)
        if (str.isNotEmpty() && v is TextView) {
            v.text = str
        }
    }

    private fun selectedET(v: EditText) {
        v.isSelected = true
        v.isEnabled = false
        v.isFocusable = false
    }

    private fun unselectedET(v: EditText) {
        v.isSelected = false
        v.isEnabled = true
        v.isFocusable = true
        v.isFocusableInTouchMode = true
    }

    private fun checkCanContinue(data: List<List<ScanResult>>): Boolean {
        if (data.isEmpty() || data[0].isEmpty()) {
            toastShort("收集失败")
            return false
        }
        return true
    }

    class Adapter : RecyclerView.Adapter<Holder>() {

        var data: List<List<WifiBean>>? = null
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

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val beans = data!![position]
            if (beans.isNullOrEmpty()) {
                return
            }
            val b0 = beans[0]
            if (b0 is XYBean) {
                val xyBean = beans[0] as XYBean
                holder.view.text = "坐标: (${xyBean.x}, ${xyBean.y}) "
                holder.view.textSize = 17f
            } else {
                holder.view.textSize = 14f
                holder.view.text = beans.let { list ->
                    var str = ""
                    val maxSize = if (b0 is LevelsBean) 12 else 24
                    val maxIndex = list.size - 1
                    list.forEachIndexed { index, result ->
                        val levelStr = if (b0 is LevelsBean) {
                            (beans[index] as LevelsBean).levels
                        } else {
                            "${beans[index].level}"
                        }
                        val ssid = if (result.ssid.length > maxSize) {
                            result.ssid.substring(0, maxSize) + "..."
                        } else result.ssid
                        val line = if (maxIndex != index) "\n" else ""
                        str = "${str}level=$levelStr\t\t\t$ssid$line"
                    }
                    return@let str
                }
            }
        }
    }

    class Holder(val view: Button) : RecyclerView.ViewHolder(view)

    class XYBean(val x: Int, val y: Int) : WifiBean("", "", 0)

    class LevelsBean(ssid: String, bssid: String, val levels: String) : WifiBean(ssid, bssid, 0)
}