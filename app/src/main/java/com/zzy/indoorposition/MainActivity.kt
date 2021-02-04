package com.zzy.indoorposition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zzy.common.router.Router
import com.zzy.common.router_api.PageConstant
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toPDR.setOnClickListener {
            Router.startSimple(this, PageConstant.Positing.NAME, PageConstant.Positing.PDR_PAGE)
        }
    }
}