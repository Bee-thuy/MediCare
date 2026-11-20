package com.example.medicare.manager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.databinding.ActivityManagerBinding
import com.example.medicare.manager.appointment.ManagerAppointmentsFragment
import com.example.medicare.manager.dashboard.ManagerDashboardFragment
import com.example.medicare.manager.doctor.ManagerDoctorsFragment
import com.example.medicare.manager.notification.ManagerNotificationsFragment
import com.example.medicare.manager.patient.ManagerPatientsFragment
import com.example.medicare.manager.schedule.ManagerWorkScheduleFragment
import com.example.medicare.manager.settings.ManagerMoreFragment
import com.example.medicare.manager.doctor.SpecialtyActivity
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.appointment.BookingSlotRepository
import com.example.medicare.ui.chat.StaffChatActivity
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.example.medicare.manager.dashboard.ManagerReportActivity
import com.example.medicare.notification.NotificationRegistration
import com.example.medicare.notification.NotificationPermission
import com.example.medicare.ui.base.RoleCatalogDialog

/** The manager shell: one activity that owns the persistent bottom navigation. */
open class ManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerBinding
    private var selectedTab = TAB_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        binding = ActivityManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BookingSlotRepository().backfillExistingAppointments()
        NotificationRegistration.registerCurrentDevice()
        NotificationPermission.requestIfNeeded(this)

        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB)
            ?: intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_HOME)
        binding.managerNavHomeInclude.root.setOnClickListener { showTab(TAB_HOME) }
        binding.managerNavDoctorsInclude.root.setOnClickListener { showTab(TAB_APPOINTMENTS) }
        binding.managerNavPatientsInclude.root.setOnClickListener { showTab(TAB_DOCTORS) }
        binding.managerNavNotificationsInclude.root.setOnClickListener { showTab(TAB_PATIENTS) }
        binding.managerNavMoreInclude.root.setOnClickListener { showCatalog() }
        binding.managerMenuButton.setOnClickListener { showCatalog() }
        binding.managerNotificationButton.setOnClickListener { showTab(TAB_NOTIFICATIONS) }

        if (savedInstanceState == null) showTab(selectedTab) else updateNavigation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showTab(intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_HOME))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showTab(tab: Int) {
        selectedTab = tab
        supportFragmentManager.beginTransaction()
            .replace(binding.managerFragmentContainer.id, fragmentFor(tab))
            .commit()
        updateNavigation()
    }

    private fun fragmentFor(tab: Int): Fragment = when (tab) {
        TAB_HOME -> ManagerDashboardFragment()
        TAB_APPOINTMENTS -> ManagerAppointmentsFragment()
        TAB_DOCTORS -> ManagerDoctorsFragment()
        TAB_PATIENTS -> ManagerPatientsFragment()
        TAB_NOTIFICATIONS -> ManagerNotificationsFragment()
        TAB_WORK_SCHEDULE -> ManagerWorkScheduleFragment()
        else -> ManagerMoreFragment()
    }

    private fun updateNavigation() {
        binding.managerHeaderTitle.text = when (selectedTab) {
            TAB_HOME -> "Tổng quan"
            TAB_APPOINTMENTS -> "Yêu cầu đặt lịch"
            TAB_DOCTORS -> "Quản lý bác sĩ"
            TAB_PATIENTS -> "Quản lý bệnh nhân"
            TAB_NOTIFICATIONS -> "Thông báo"
            TAB_WORK_SCHEDULE -> "Lịch làm việc"
            else -> "Danh mục quản trị"
        }
        val items = listOf(
            NavigationItem(binding.managerNavHomeInclude.root, binding.managerNavHomeInclude.managerNavHomeIcon, binding.managerNavHomeInclude.managerNavHomeLabel, TAB_HOME),
            NavigationItem(binding.managerNavDoctorsInclude.root, binding.managerNavDoctorsInclude.managerNavDoctorsIcon, binding.managerNavDoctorsInclude.managerNavDoctorsLabel, TAB_APPOINTMENTS),
            NavigationItem(binding.managerNavPatientsInclude.root, binding.managerNavPatientsInclude.managerNavPatientsIcon, binding.managerNavPatientsInclude.managerNavPatientsLabel, TAB_DOCTORS),
            NavigationItem(binding.managerNavNotificationsInclude.root, binding.managerNavNotificationsInclude.managerNavNotificationsIcon, binding.managerNavNotificationsInclude.managerNavNotificationsLabel, TAB_PATIENTS),
            NavigationItem(binding.managerNavMoreInclude.root, binding.managerNavMoreInclude.managerNavMoreIcon, binding.managerNavMoreInclude.managerNavMoreLabel, TAB_MORE),
        )
        items.forEach { item ->
            val active = item.tab == selectedTab
            val color = ContextCompat.getColor(this, if (active) R.color.manager_blue else R.color.manager_muted)
            item.container.isSelected = active
            item.icon.isSelected = active
            item.label.setTextColor(color)
        }
    }

    private fun showCatalog() {
        val labels = arrayOf("📊  Tổng quan", "🗓️  Yêu cầu đặt lịch", "👨‍⚕️  Quản lý bác sĩ",
            "👥  Quản lý bệnh nhân", "📅  Lịch làm việc", "🧬  Danh mục chuyên khoa",
            "🔔  Thông báo", "📈  Thống kê báo cáo", "💬  Chat với bác sĩ")
        RoleCatalogDialog.show(this, "Quản trị viên · Các chức năng", getColor(R.color.manager_blue), labels,
            selectedIndex = when (selectedTab) {
                TAB_HOME -> 0; TAB_APPOINTMENTS -> 1; TAB_DOCTORS -> 2; TAB_PATIENTS -> 3
                TAB_WORK_SCHEDULE -> 4; TAB_NOTIFICATIONS -> 6; else -> -1
            }, footerLabel = "☷  Công cụ bổ sung", onFooter = { showTab(TAB_MORE) }) { index ->
            when (index) {
                0 -> showTab(TAB_HOME)
                1 -> showTab(TAB_APPOINTMENTS)
                2 -> showTab(TAB_DOCTORS)
                3 -> showTab(TAB_PATIENTS)
                4 -> showTab(TAB_WORK_SCHEDULE)
                5 -> startActivity(Intent(this, SpecialtyActivity::class.java))
                6 -> showTab(TAB_NOTIFICATIONS)
                7 -> startActivity(Intent(this, ManagerReportActivity::class.java))
                8 -> chooseDoctorForChat()
            }
        }
    }

    fun chooseDoctorForChat() {
        DoctorManagementRepository(this).getDoctors { result ->
            if (isFinishing || isDestroyed) return@getDoctors
            result.onSuccess { doctors ->
                if (doctors.isEmpty()) {
                    Toast.makeText(this, "Chưa có bác sĩ để trò chuyện.", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                AlertDialog.Builder(this).setTitle("Chat với bác sĩ")
                    .setItems(doctors.map { it.fullName }.toTypedArray()) { _, index ->
                        val doctor = doctors[index]
                        startActivity(Intent(this, StaffChatActivity::class.java)
                            .putExtra(StaffChatActivity.EXTRA_DOCTOR_ID, doctor.id)
                            .putExtra(StaffChatActivity.EXTRA_TITLE, "BS. ${doctor.fullName}"))
                    }.show()
            }.onFailure { Toast.makeText(this, it.message ?: "Không tải được danh sách bác sĩ.", Toast.LENGTH_LONG).show() }
        }
    }

    private data class NavigationItem(
        val container: View,
        val icon: TextView,
        val label: TextView,
        val tab: Int,
    )

    companion object {
        const val STATE_SELECTED_TAB = "selected_manager_tab"
        const val EXTRA_INITIAL_TAB = "manager_initial_tab"
        const val TAB_HOME = 0
        const val TAB_APPOINTMENTS = 1
        const val TAB_DOCTORS = 2
        const val TAB_PATIENTS = 3
        const val TAB_MORE = 4
        const val TAB_NOTIFICATIONS = 5
        const val TAB_WORK_SCHEDULE = 6
    }
}
