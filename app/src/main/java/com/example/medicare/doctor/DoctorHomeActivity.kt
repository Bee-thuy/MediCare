package com.example.medicare.doctor

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.databinding.ActivityDoctorShellBinding
import com.example.medicare.doctor.chat.DoctorChatsFragment
import com.example.medicare.doctor.dashboard.DoctorDashboardFragment
import com.example.medicare.doctor.medical.DoctorPrescriptionsFragment
import com.example.medicare.doctor.patient.DoctorPatientsFragment
import com.example.medicare.doctor.profile.DoctorProfileFragment
import com.example.medicare.doctor.schedule.DoctorScheduleFragment
import com.example.medicare.notification.NotificationRegistration
import com.example.medicare.notification.NotificationPermission
import com.example.medicare.notification.NotificationsActivity
import com.example.medicare.ui.base.RoleCatalogDialog
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.ui.auth.LoginActivity

/** Persistent shell for the doctor role. Each day-to-day workflow owns a fragment. */
class DoctorHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorShellBinding
    private var selectedTab = TAB_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorShellBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.doctorNavHome, binding.doctorNavAppointments, binding.doctorNavPatients,
            binding.doctorNavExam, binding.doctorNavCatalog).forEach(::styleNavIcon)
        NotificationRegistration.registerCurrentDevice()
        NotificationPermission.requestIfNeeded(this)
        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB, TAB_HOME) ?: TAB_HOME
        binding.doctorMenuButton.setOnClickListener { showCatalog() }
        binding.doctorHeaderNotifications.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        binding.doctorNavHome.setOnClickListener { showTab(TAB_HOME) }
        binding.doctorNavAppointments.setOnClickListener { showTab(TAB_APPOINTMENTS) }
        binding.doctorNavPatients.setOnClickListener { showTab(TAB_PATIENTS) }
        binding.doctorNavExam.setOnClickListener { showTab(TAB_EXAM) }
        binding.doctorNavCatalog.setOnClickListener { showCatalog() }
        if (savedInstanceState == null) showTab(selectedTab) else updateNavigation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    fun showTab(tab: Int) {
        selectedTab = tab
        supportFragmentManager.beginTransaction().replace(binding.doctorFragmentContainer.id, fragmentFor(tab)).commit()
        updateNavigation()
    }

    private fun fragmentFor(tab: Int): Fragment = when (tab) {
        TAB_PATIENTS -> DoctorPatientsFragment()
        TAB_CHATS -> DoctorChatsFragment()
        TAB_SCHEDULE -> DoctorScheduleFragment()
        TAB_PRESCRIPTIONS -> DoctorPrescriptionsFragment()
        TAB_PROFILE -> DoctorProfileFragment()
        else -> DoctorDashboardFragment().apply {
            arguments = Bundle().apply {
                putBoolean(DoctorDashboardFragment.ARG_COMPLETED_ONLY, tab == TAB_EXAM)
                putBoolean(DoctorDashboardFragment.ARG_SHOW_OVERVIEW, tab == TAB_HOME)
            }
        }
    }

    private fun updateNavigation() {
        listOf(
            binding.doctorNavHome to TAB_HOME,
            binding.doctorNavAppointments to TAB_APPOINTMENTS,
            binding.doctorNavPatients to TAB_PATIENTS,
            binding.doctorNavExam to TAB_EXAM,
        ).forEach { (view, tab) ->
            view.isSelected = selectedTab == tab
            view.setTextColor(ContextCompat.getColor(this, if (view.isSelected) R.color.doctor_teal else R.color.manager_muted))
        }
        binding.doctorNavCatalog.isSelected = selectedTab in setOf(TAB_CHATS, TAB_SCHEDULE, TAB_PRESCRIPTIONS, TAB_PROFILE)
        binding.doctorNavCatalog.setTextColor(ContextCompat.getColor(this,
            if (binding.doctorNavCatalog.isSelected) R.color.doctor_teal else R.color.manager_muted))
        binding.doctorHeaderTitle.text = when (selectedTab) {
            TAB_HOME -> "Tổng quan bác sĩ"
            TAB_PATIENTS -> "Bệnh nhân được phân công"
            TAB_EXAM -> "Kết quả khám"
            TAB_CHATS -> "Chat tư vấn"
            TAB_SCHEDULE -> "Thời gian làm việc"
            TAB_PRESCRIPTIONS -> "Đơn thuốc điện tử"
            TAB_PROFILE -> "Hồ sơ cá nhân"
            else -> "Lịch khám cá nhân"
        }
    }

    private fun showCatalog() {
        val labels = arrayOf("🏠  Tổng quan bác sĩ", "📋  Lịch khám cá nhân", "👥  Bệnh nhân được phân công",
            "🩺  Kết quả khám", "💊  Đơn thuốc điện tử", "💬  Chat tư vấn",
            "📅  Thời gian làm việc", "👤  Hồ sơ cá nhân")
        RoleCatalogDialog.show(this, "Bác sĩ · Các chức năng", getColor(R.color.doctor_teal), labels,
            selectedIndex = when (selectedTab) {
                TAB_HOME -> 0; TAB_APPOINTMENTS -> 1; TAB_PATIENTS -> 2; TAB_EXAM -> 3
                TAB_CHATS -> 5; TAB_SCHEDULE -> 6; TAB_PRESCRIPTIONS -> 4; TAB_PROFILE -> 7; else -> -1
            }, footerLabel = "🚪  Đăng xuất", onFooter = {
                AuthRepository().signOut()
                startActivity(Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }) { index ->
                when (index) {
                    0 -> showTab(TAB_HOME)
                    1 -> showTab(TAB_APPOINTMENTS)
                    2 -> showTab(TAB_PATIENTS)
                    3 -> showTab(TAB_EXAM)
                    4 -> showTab(TAB_PRESCRIPTIONS)
                    5 -> showTab(TAB_CHATS)
                    6 -> showTab(TAB_SCHEDULE)
                    7 -> showTab(TAB_PROFILE)
                }
            }
    }

    private fun styleNavIcon(view: TextView) {
        val label = SpannableString(view.text)
        val iconEnd = label.indexOf('\n')
        if (iconEnd > 0) label.setSpan(RelativeSizeSpan(1.8f), 0, iconEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text = label
    }

    companion object {
        const val TAB_APPOINTMENTS = 0
        const val TAB_PATIENTS = 1
        const val TAB_CHATS = 2
        const val TAB_SCHEDULE = 3
        const val TAB_HOME = 4
        const val TAB_EXAM = 5
        const val TAB_PRESCRIPTIONS = 6
        const val TAB_PROFILE = 7
        private const val STATE_SELECTED_TAB = "doctor_selected_tab"
    }
}
