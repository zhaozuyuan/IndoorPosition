package com.zzy.lib_pdr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class PDRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)
        supportActionBar?.hide()
    }
}