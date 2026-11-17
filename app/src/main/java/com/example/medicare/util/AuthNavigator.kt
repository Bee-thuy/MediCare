package com.example.medicare.util

import android.app.Activity
import android.content.Intent
import com.example.medicare.model.UserRole
import com.example.medicare.doctor.DoctorHomeActivity
import com.example.medicare.manager.ManagerActivity
import com.example.medicare.user.PatientHomeActivity

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
