package com.example.medicare.util

import android.app.Activity
import android.content.Intent
import com.example.medicare.model.UserRole
import com.example.medicare.ui.doctor.DoctorHomeActivity
import com.example.medicare.manager.ui.ManagerActivity
import com.example.medicare.ui.patient.PatientHomeActivity

object AuthNavigator {
    fun openHome(activity: Activity, role: UserRole) {
        val destination = when (role) {
            UserRole.ADMIN -> ManagerActivity::class.java
            UserRole.USER -> PatientHomeActivity::class.java
            UserRole.DOCTOR -> DoctorHomeActivity::class.java
        }
        activity.startActivity(
            Intent(activity, destination).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
        activity.finish()
    }
}
