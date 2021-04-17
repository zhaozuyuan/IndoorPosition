package com.zzy.common.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.zzy.common.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * create by zuyuan on 2021/2/20
 * 虽然不是传感器, 可以当成传感器来处理
 */
class WifiHandler(private val activity: FragmentActivity,
                  private val maxScanTimes: Int = 6,
                  private val sleepDurationOnSuccess: Long = 50L) : ISensorHandler {

    companion object {
        private const val TAG = "WifiHandler"

        private const val DEFAULT_TAG = -1L

        private val HANDLER_WHAT = hashCode()
    }

    private val isRegistered = AtomicBoolean(false)

    private var curScanSuccessCount = 0
    private var curScanResultsList: MutableList<List<ScanResult>> = mutableListOf()

    @Volatile
    private var curTag = DEFAULT_TAG
    //结果callback
    @Volatile
    private var curCallback: ((List<List<ScanResult>>) -> Unit)? = null
    //进度callback
    @Volatile
    private var curProgressCallback: ((List<List<ScanResult>>) -> Unit)? = null

    private val wifiManager: WifiManager by lazy {
        activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val lock: Object = Object()

    private val myHandler = mainHandler

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(p0: Context?, intent: Intent) {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            } else {
                true
            }
            if (!success) {
                Log.e(TAG, "EXTRA_RESULTS_UPDATED = false. (数据未刷新)")
                scanFailure()
            } else {
                scanSuccess()
            }
        }
    }

    fun isRunning() = isRegistered.get()

    fun scanOnce(callback: (List<List<ScanResult>>) -> Unit,
                 progress: (List<List<ScanResult>>) -> Unit = { }) {
        if (!isRegistered.get()) return

        curCallback = callback
        curProgressCallback = progress
        curScanSuccessCount = 0
        curScanResultsList = mutableListOf()
        val myTag: Long = System.currentTimeMillis()
        curTag = myTag
        ioSync {
            var preSuccessCount = 0
            while (myTag == curTag) {
                if (curScanSuccessCount >= maxScanTimes) {
                    val copyList = curScanResultsList
                    postToUIThread(lifecycle = activity.lifecycle) {
                        curCallback!!.invoke(copyList)
                    }
                    curTag = DEFAULT_TAG
                    break
                } else if (preSuccessCount != curScanSuccessCount) {
                    preSuccessCount = curScanSuccessCount
                    postToUIThread(lifecycle = activity.lifecycle) {
                        curProgressCallback!!.invoke(curScanResultsList)
                    }
                }

                //8.0以上过时,2min4次的限制.
                if (wifiManager.startScan()) {
                    Log.d(TAG, "startScan=true")
                    //防止出现意外一直休眠
                    val msg = Message.obtain(myHandler) {
                        synchronized(lock) {
                            if (isRegistered.get()) {
                                lock.notifyAll()
                            }
                        }
                    }
                    msg.what = HANDLER_WHAT
                    myHandler.sendMessageDelayed(msg, 10000L)

                    //等待结果
                    synchronized(lock) {
                        lock.wait()
                    }
                    if (myTag != curTag) {
                        break
                    }

                    //清除队列里面的消息
                    myHandler.removeMessages(HANDLER_WHAT)

                    //成功一次，休眠指定时间
//                    if (curScanSuccessCount in preSuccessCount until maxScanTimes) {
//                        SystemClock.sleep(sleepDurationOnSuccess)
//                    }
                } else {
                    Log.d(TAG, "startScan=false")
                    SystemClock.sleep(50L)
                }
            }
        }
    }

    override fun startListen() {
        val canRegister: Boolean = isRegistered.compareAndSet(false, true)
        if (!canRegister) {
            throw RuntimeException("SysStepSensorHandler has be registered !!!")
        }

        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE
        )
        PermissionHelper.simplePermissions(activity, permissions, object : PermissionHelper.Callback {
            override fun onGranted() {
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                activity.registerReceiver(receiver, intentFilter)
                if (curCallback != null && curTag == 0L) {
                    scanOnce(curCallback!!, curProgressCallback!!)
                }
            }

            override fun onDenied(deniedPermissions: List<String>) {
                curCallback?.invoke(emptyList())
            }
        })
    }

    override fun stopListen() {
        myHandler.removeMessages(HANDLER_WHAT)

        curTag = DEFAULT_TAG
        synchronized(lock) {
            lock.notifyAll()
        }
        if (isRegistered.compareAndSet(true, false)) {
            activity.unregisterReceiver(receiver)
        }
    }

    private fun scanSuccess() {
        val results: List<ScanResult> = wifiManager.scanResults
        if (!results.isNullOrEmpty()) {
            curScanResultsList.add(results)
            curScanSuccessCount++
            Log.d(TAG, results.joinToString { "${it.SSID} ${it.BSSID} ${it.level}" })
        } else {
            Log.e(TAG, "scan result is null or empty.")
        }

        //防止线程堵死
        synchronized(lock) {
            lock.notifyAll()
        }
    }

    private fun scanFailure() { }
}