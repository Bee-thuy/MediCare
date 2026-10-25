package com.example.medicare.ui.admin

import android.os.Bundle
import com.example.medicare.R
import com.example.medicare.ui.base.RoleHomeActivity

class AdminHomeActivity : RoleHomeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome(R.string.role_admin_home_title, R.string.role_admin_home_description)
    }
}
