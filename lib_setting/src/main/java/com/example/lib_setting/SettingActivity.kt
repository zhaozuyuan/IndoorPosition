package com.example.lib_setting

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.zzy.common.router.Router
import com.zzy.common.router_api.PageConstant
import com.zzy.common.util.LogTxtUtil
import com.zzy.common.util.SPKeys
import com.zzy.common.util.SPUtil
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        btnWiFiList.setOnClickListener {
            Router.startSimple(this, PageConstant.Setting.NAME, PageConstant.Setting.WIFI_APPOINT_PAGE)
        }

        btnPDRDirection.setOnClickListener {
            Router.startSimple(this, PageConstant.Setting.NAME, PageConstant.Setting.PDR_DIRECTION_PAGE)
        }

        refreshKey()
        btnWifiAlgorithm.setOnClickListener { view ->
            SPUtil.commitValues {
                var isKNN = true
                SPUtil.getValues {
                    isKNN = getBoolean(SPKeys.ALGORITHM_IS_KNN, true)
                }
                SPUtil.commitValues {
                    putBoolean(SPKeys.ALGORITHM_IS_KNN, !isKNN)
                }
                refreshKey()
            }
        }

        if (LogTxtUtil.openLog) {
            btnOpenLog.text = "已打开日志txt输出"
        } else {
            btnOpenLog.text = "已关闭日志txt输出"
        }
        btnOpenLog.setOnClickListener {
            if (LogTxtUtil.openLog) {
                btnOpenLog.text = "已关闭日志txt输出"
                LogTxtUtil.openLog = false
            } else {
                btnOpenLog.text = "已打开日志txt输出"
                LogTxtUtil.openLog = true
            }

        }
    }

    private fun refreshKey() {
        var isKNN = true
        SPUtil.getValues {
            isKNN = getBoolean(SPKeys.ALGORITHM_IS_KNN, true)
        }
        if (isKNN) {
            btnWifiAlgorithm.text = "WiFi定位：KNN算法"
        } else {
            btnWifiAlgorithm.text = "WiFi定位：邻近矩形法"
        }
    }
}