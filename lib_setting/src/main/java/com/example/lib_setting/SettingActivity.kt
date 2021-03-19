package com.example.lib_setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zzy.common.router.Router
import com.zzy.common.router_api.PageConstant
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        btnWiFiList.setOnClickListener {
            Router.startSimple(this, PageConstant.Setting.NAME, PageConstant.Setting.WIFI_APPOINT_PAGE)
        }

        btnPDRDirection.setOnClickListener {
            Router.startSimple(this, PageConstant.Setting.NAME, PageConstant.Setting.PDR_DIRECTION_PAGE)
        }
    }
}