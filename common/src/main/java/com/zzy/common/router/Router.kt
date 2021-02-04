package com.zzy.common.router

import android.app.Activity
import android.content.Intent

object Router {

    fun startSimple(activity: Activity, module: String, page: String) {
        activity.startActivity(getIntent(activity, module, page))
    }

    fun getIntent(activity: Activity, module: String, page: String): Intent {
        val url = assembleUrl(module, page)
        val target = PageContainer.getPageClazzNoNull(url)
        return Intent(activity, target)
    }

    fun assembleUrl(module: String, page: String) =
            "${RouterConstant.SCHEME}${RouterConstant.SCHEME_SEPARATOR}${RouterConstant.HOST}" +
                    "${RouterConstant.URI_PATH_SEPARATOR}$module${RouterConstant.URI_PATH_SEPARATOR}$page"
}