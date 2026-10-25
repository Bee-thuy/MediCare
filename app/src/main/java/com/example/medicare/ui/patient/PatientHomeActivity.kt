package com.example.medicare.ui.patient

import android.os.Bundle
import com.example.medicare.R
import com.example.medicare.ui.base.RoleHomeActivity

class PatientHomeActivity : RoleHomeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome(R.string.role_user_home_title, R.string.role_user_home_description)
    }
}
