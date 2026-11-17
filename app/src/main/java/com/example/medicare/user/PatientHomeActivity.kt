package com.example.medicare.user

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.medicare.databinding.ActivityPatientHomeBinding
import com.example.medicare.user.appointment.MyAppointmentsActivity
import com.example.medicare.user.doctor.DoctorDirectoryFragment
import com.example.medicare.user.appointment.PatientBookingFragment
import com.example.medicare.model.Doctor
import com.example.medicare.user.reminder.MedicationReminderActivity
import com.example.medicare.ui.profile.ProfileActivity
import com.example.medicare.notification.NotificationRegistration
import com.example.medicare.notification.NotificationPermission
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.user.medical.MedicalHistoryActivity
import com.example.medicare.user.review.MyReviewsActivity
import com.example.medicare.user.chat.PatientChatsActivity
import com.example.medicare.user.settings.PatientSettingsActivity
import com.example.medicare.ui.base.RoleCatalogDialog
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class PatientHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientHomeBinding
    private val appointments = AppointmentRepository()
    private var selectedTab = TAB_HOME
    private lateinit var tabBack: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.patientNavAccount, binding.patientNavHome, binding.patientNavSearch,
            binding.patientNavBooking, binding.patientNavCatalog).forEach(::styleNavIcon)
        tabBack = onBackPressedDispatcher.addCallback(this, enabled = false) { showTab(TAB_HOME) }
        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB, TAB_HOME) ?: TAB_HOME
        NotificationRegistration.registerCurrentDevice()
        NotificationPermission.requestIfNeeded(this)

        binding.patientBrowseDoctors.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientSearchShortcut.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientMenuButton.setOnClickListener { showCatalog() }
        binding.patientHeaderNotifications.setOnClickListener { startActivity(Intent(this, PatientSettingsActivity::class.java)) }
        binding.patientNavAccount.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.patientNavHome.setOnClickListener { showTab(TAB_HOME); binding.patientHomeScroll.smoothScrollTo(0, 0) }
        binding.patientNavSearch.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientNavBooking.setOnClickListener { showTab(TAB_BOOKING) }
        binding.patientNavCatalog.setOnClickListener { showCatalog() }
        binding.patientBookingShortcut.setOnClickListener { showTab(TAB_BOOKING) }
        binding.patientChatShortcut.setOnClickListener { startActivity(Intent(this, PatientChatsActivity::class.java)) }
        binding.patientMyAppointments.setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        binding.patientMedicationReminders.setOnClickListener {
            startActivity(Intent(this, MedicationReminderActivity::class.java))
        }
        binding.patientMedicalHistory.setOnClickListener { startActivity(Intent(this, MedicalHistoryActivity::class.java)) }
        binding.patientMyReviews.setOnClickListener { startActivity(Intent(this, MyReviewsActivity::class.java)) }
        if (savedInstanceState == null || (selectedTab != TAB_HOME && supportFragmentManager.findFragmentById(binding.patientFragmentContainer.id) == null)) {
            showTab(selectedTab)
        } else updateNavigation()
        loadDashboard()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() { super.onResume(); loadDashboard() }

    fun showBooking(doctor: Doctor) = showTab(TAB_BOOKING, doctor)

    fun showTab(tab: Int, doctor: Doctor? = null) {
        selectedTab = tab
        if (tab != TAB_HOME) {
            val fragment = if (tab == TAB_SEARCH) DoctorDirectoryFragment()
                else PatientBookingFragment.newInstance(doctor)
            supportFragmentManager.beginTransaction()
                .replace(binding.patientFragmentContainer.id, fragment)
                .commit()
        }
        updateNavigation()
    }

    private fun updateNavigation() {
        binding.patientHomeScroll.visibility = if (selectedTab == TAB_HOME) View.VISIBLE else View.GONE
        binding.patientFragmentContainer.visibility = if (selectedTab == TAB_HOME) View.GONE else View.VISIBLE
        binding.patientHeaderTitle.text = when (selectedTab) {
            TAB_SEARCH -> "Tìm bác sĩ & chuyên khoa"
            TAB_BOOKING -> "Đặt lịch khám"
            else -> "Trang chủ bệnh nhân"
        }
        listOf(binding.patientNavHome to TAB_HOME, binding.patientNavSearch to TAB_SEARCH,
            binding.patientNavBooking to TAB_BOOKING).forEach { (view, tab) ->
            view.isSelected = selectedTab == tab
            view.setTextColor(ContextCompat.getColor(this,
                if (view.isSelected) R.color.patient_purple else R.color.manager_muted))
        }
        tabBack.isEnabled = selectedTab != TAB_HOME
    }

    private fun showCatalog() {
        val labels = arrayOf("🔐  Đăng ký & đăng nhập", "🏠  Trang chủ bệnh nhân",
            "🔍  Tìm bác sĩ & chuyên khoa", "📅  Đặt lịch khám", "📋  Quản lý lịch khám",
            "💬  Chat tư vấn bác sĩ", "📂  Hồ sơ sức khỏe", "💊  Nhắc thuốc & tái khám",
            "⭐  Đánh giá & phản hồi", "⚙️  Thông báo & cài đặt")
        RoleCatalogDialog.show(this, "Bệnh nhân · Các chức năng", getColor(R.color.patient_purple), labels,
            selectedIndex = when (selectedTab) { TAB_SEARCH -> 2; TAB_BOOKING -> 3; else -> 1 }) { index ->
                if (index == 1) { showTab(TAB_HOME); return@show }
                if (index == 2) { showTab(TAB_SEARCH); return@show }
                if (index == 3) { showTab(TAB_BOOKING); return@show }
                val destination = when (index) {
                    0 -> ProfileActivity::class.java
                    9 -> PatientSettingsActivity::class.java
                    4 -> MyAppointmentsActivity::class.java
                    5 -> PatientChatsActivity::class.java
                    6 -> MedicalHistoryActivity::class.java
                    7 -> MedicationReminderActivity::class.java
                    8 -> MyReviewsActivity::class.java
                    else -> null
                }
                if (destination != null) startActivity(Intent(this, destination))
                else { showTab(TAB_HOME); binding.patientHomeScroll.smoothScrollTo(0, 0) }
            }
    }

    private fun styleNavIcon(view: TextView) {
        val label = SpannableString(view.text)
        val iconEnd = label.indexOf('\n')
        if (iconEnd > 0) label.setSpan(RelativeSizeSpan(1.8f), 0, iconEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text = label
    }

    private fun loadDashboard() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { profile ->
            val name = profile.getString("fullName").orEmpty().trim()
            binding.patientHomeGreeting.text = if (name.isBlank()) "Xin chào!" else "Xin chào,\n$name!"
        }
        appointments.getAppointmentsForPatient(uid) { result ->
            val next = result.getOrDefault(emptyList()).filter { it.status in setOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED) && it.appointmentDate >= LocalDate.now().toString() }.minWithOrNull(compareBy({ it.appointmentDate }, { it.startTime }))
            binding.patientUpcomingAppointment.text = next?.let { "${it.doctorName} · ${it.appointmentDate} · ${it.startTime}" } ?: getString(com.example.medicare.R.string.patient_no_upcoming)
        }
        DoctorManagementRepository(this).getActiveDoctors { result ->
            val doctors = result.getOrDefault(emptyList())
            binding.patientPopularSpecialties.text = doctors.groupingBy { it.specialty }.eachCount().entries.sortedByDescending { it.value }.take(3).joinToString(" · ") { it.key }
            binding.patientFeaturedDoctors.text = doctors.take(3).joinToString(" · ") { it.fullName }
        }
    }

    companion object {
        const val TAB_HOME = 0
        const val TAB_SEARCH = 1
        const val TAB_BOOKING = 2
        private const val STATE_SELECTED_TAB = "patient_selected_tab"
    }
}
