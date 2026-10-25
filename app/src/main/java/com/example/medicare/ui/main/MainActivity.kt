package com.example.medicare.ui.main

import android.content.Intent
import android.os.Bundle
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.splash.SplashActivity

class MainActivity : FullscreenActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }
}
