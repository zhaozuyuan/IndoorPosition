package com.zzy.common.util

import android.app.Activity
import android.view.View

object DisplayAttrUtil {

    fun getDensity() = Global.app.resources.displayMetrics.density

    fun getContentWH(activity: Activity): Pair<Int, Int> {
        val view: View? = activity.findViewById(android.R.id.content)
        return if (view == null || !view.isLaidOut) {
            val metrics = activity.resources.displayMetrics
            Pair(metrics.widthPixels, metrics.heightPixels)
        } else {
            Pair(view.measuredWidth, view.measuredHeight)
        }
    }
}