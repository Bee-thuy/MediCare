package com.example.medicare.manager

import android.app.Activity
import android.content.Intent
import android.view.View
import com.example.medicare.R
import com.example.medicare.manager.dashboard.ManagerReportActivity
import com.example.medicare.manager.doctor.SpecialtyActivity
import com.example.medicare.ui.base.RoleCatalogDialog

/** Keeps the same five Admin navigation items visible on report and specialty screens. */
object ManagerAuxNavigation {
    fun bind(activity: Activity) {
        mapOf(
            R.id.managerNavHome to ManagerActivity.TAB_HOME,
            R.id.managerNavDoctors to ManagerActivity.TAB_APPOINTMENTS,
            R.id.managerNavPatients to ManagerActivity.TAB_DOCTORS,
            R.id.managerNavNotifications to ManagerActivity.TAB_PATIENTS,
        ).forEach { (viewId, tab) ->
            activity.findViewById<View>(viewId)?.setOnClickListener { go(activity, tab) }
        }
        activity.findViewById<View>(R.id.managerNavMore)?.setOnClickListener { showCatalog(activity) }
        activity.findViewById<View>(R.id.managerAuxNotificationButton)?.setOnClickListener {
            go(activity, ManagerActivity.TAB_NOTIFICATIONS)
        }
    }

    private fun go(activity: Activity, tab: Int) {
        activity.startActivity(Intent(activity, ManagerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ManagerActivity.EXTRA_INITIAL_TAB, tab))
        activity.finish()
    }

    fun showCatalog(activity: Activity) {
        val labels = arrayOf("📊  Tổng quan", "🗓️  Yêu cầu đặt lịch", "👨‍⚕️  Quản lý bác sĩ",
            "👥  Quản lý bệnh nhân", "📅  Lịch làm việc", "🧬  Danh mục chuyên khoa",
            "🔔  Thông báo", "📈  Thống kê báo cáo")
        RoleCatalogDialog.show(activity, "Quản trị viên · Các chức năng",
            activity.getColor(R.color.manager_blue), labels,
            selectedIndex = if (activity is SpecialtyActivity) 5 else 7,
            footerLabel = "☷  Công cụ bổ sung", onFooter = { go(activity, ManagerActivity.TAB_MORE) }) { index ->
            when (index) {
                in 0..3 -> go(activity, index)
                4 -> go(activity, ManagerActivity.TAB_WORK_SCHEDULE)
                5 -> if (activity !is SpecialtyActivity) {
                    activity.startActivity(Intent(activity, SpecialtyActivity::class.java)); activity.finish()
                }
                6 -> go(activity, ManagerActivity.TAB_NOTIFICATIONS)
                7 -> if (activity !is ManagerReportActivity) {
                    activity.startActivity(Intent(activity, ManagerReportActivity::class.java)); activity.finish()
                }
            }
        }
    }
}
