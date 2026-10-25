package com.example.medicare.ui.base

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton

abstract class RoleHomeActivity : FullscreenActivity() {
    protected fun showHome(@StringRes title: Int, @StringRes description: Int) {
        setContentView(R.layout.activity_role_home)
        findViewById<TextView>(R.id.roleHomeTitle).setText(title)
        findViewById<TextView>(R.id.roleHomeDescription).setText(description)
        findViewById<MaterialButton>(R.id.roleHomeLogout).setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
