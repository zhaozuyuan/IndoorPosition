package com.zzy.indoorposition

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.router.Router
import com.zzy.common.router_api.PageConstant
import com.zzy.common.util.PermissionHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toPDR.setOnClickListener {
            Router.startSimple(this, PageConstant.Positing.NAME, PageConstant.Positing.PDR_PAGE)
        }
        toRSSI.setOnClickListener {
            Router.startSimple(this, PageConstant.Wifi.NAME, PageConstant.Wifi.WIFI_RSSI_PAGE)
        }
        btnSetting.setOnClickListener {
            Router.startSimple(this, PageConstant.Setting.NAME, PageConstant.Setting.SETTING_HOME_PAGE)
        }
        toCombine.setOnClickListener {
            Router.startSimple(this, PageConstant.Positing.NAME, PageConstant.Positing.COMBINE_PAGE)
        }
        toWifiPosition.setOnClickListener {
            Router.startSimple(this, PageConstant.Positing.NAME, PageConstant.Positing.WIFI_PAGE)
        }

        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)


        PermissionHelper.simplePermissions(this, permissions, object : PermissionHelper.Callback {
            override fun onGranted() {
                Log.d("Main", "permissions success!!!")
            }

            override fun onDenied(deniedPermissions: List<String>) {
                finish()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}