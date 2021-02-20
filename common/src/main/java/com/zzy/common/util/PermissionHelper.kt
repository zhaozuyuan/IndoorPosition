package com.zzy.common.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 权限请求Helper
 * 注意: onRequestPermissionsResult()需要在BaseActivity或者业务Activity中主动调用
 */
object PermissionHelper {

    private const val INIT_ATOMIC_VALUE = false

    //0 以上才是有效值
    private var code = AtomicInteger(0)

    private val callbackList = mutableListOf<CallbackWithCode>()

    private val atomicBoolean = AtomicBoolean(INIT_ATOMIC_VALUE)

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 需主动在BaseActivity的onRequestPermissionsResult()下调用
     */
    @MainThread
    fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<out String>,
                                   grantResults: IntArray) {
        var index = -1
        val length = callbackList.size
        while (++index < length) {
            val target = callbackList[index]
            if (target.code == requestCode) {
                if (!target.isUseful) {
                    return
                } else {
                    target.isUseful = false
                }
                val deniedPermissions = mutableListOf<String>()
                permissions.forEachIndexed { index, per ->
                    if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                        deniedPermissions.add(per)
                    }
                }
                if (deniedPermissions.isEmpty()) {
                    target.callback?.onGranted()
                } else {
                    target.callback?.onDenied(deniedPermissions)
                }
            }
        }
    }

    /**
     * 请求权限，采用此方法就够了（支持多线程环境）
     *
     * @param forceRequest 不管用户有没有拒绝，都要请求权限
     * @param callback 真正向用户请求权限时的回掉
     * @param showToastListener 检查是否是用户已经拒绝多次的权限，是的话则产生回掉
     */
    fun simplePermissions(activity: FragmentActivity,
                          permissions: Array<String>,
                          callback: Callback,
                          forceRequest: Boolean = true,
                          showToastListener: OnShouldShowToastListener? = null) {
        val deniedPermissions = getDeniedPermissions(activity, permissions)
        if (deniedPermissions.isNotEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !forceRequest) {
                for (permission in permissions) {
                    if (!activity.shouldShowRequestPermissionRationale(permission)) {
                        requestPermissions(activity, permissions, callback)
                        return
                    }
                }
                showToastListener?.onShowToast()
            } else {
                requestPermissions(activity, permissions, callback)
            }
        } else {
            runUIThread {
                callback.onGranted()
            }
        }
    }

    /**
     * 得到没有用户批准的权限数组
     */
    fun getDeniedPermissions(context: Context, permissions: Array<String>): Array<String> {
        val targetList = mutableListOf<String>()
        permissions.forEach {
            val result = ActivityCompat.checkSelfPermission(context, it)
            if (result == PackageManager.PERMISSION_DENIED) {
                targetList.add(it)
            }
        }
        return targetList.toTypedArray()
    }

    /**
     * 直接请求权限（支持多线程环境）
     */
    fun requestPermissions(activity: FragmentActivity,
                           permissions: Array<String>,
                           callback: Callback
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val callbackCode = CallbackWithCode(callback, code.incrementAndGet())
            putCallback(callbackCode)
            ActivityCompat.requestPermissions(activity, permissions, callbackCode.code)
            activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    callbackCode.callback = null
                }
            })
        } else {
            ActivityCompat.requestPermissions(activity, permissions, Int.MAX_VALUE)
            runUIThread {
                if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    callback.onGranted()
                }
            }
        }
    }

    private fun putCallback(callback: CallbackWithCode) {
        while (!atomicBoolean.compareAndSet(INIT_ATOMIC_VALUE, !INIT_ATOMIC_VALUE)) {
            //CAS
        }
        callbackList.add(callback)
        atomicBoolean.set(INIT_ATOMIC_VALUE)
    }

    private fun runUIThread(task: () -> Unit) {
        mainHandler.post(task)
    }

    interface Callback {

        /**
         * 当申请的全部权限通过时
         */
        @MainThread
        fun onGranted()

        /**
         * 当某些权限被拒绝时
         */
        @MainThread
        fun onDenied(deniedPermissions: List<String>)
    }

    interface OnShouldShowToastListener {

        /**
         * 当前第二次请求被拒绝的权限时，应该回掉此方法
         */
        fun onShowToast()
    }

    private class CallbackWithCode(var callback: Callback?, val code: Int, var isUseful: Boolean = true)
}