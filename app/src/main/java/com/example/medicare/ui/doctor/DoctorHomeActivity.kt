package com.example.medicare.ui.doctor

import android.os.Bundle
import com.example.medicare.R
import com.example.medicare.ui.base.RoleHomeActivity

class DoctorHomeActivity : RoleHomeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome(R.string.role_doctor_home_title, R.string.role_doctor_home_description)
    }
}
