package com.example.medicare.user

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.Gravity
import android.widget.LinearLayout
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
import com.example.medicare.model.Appointment
import com.example.medicare.data.review.DoctorReviewRepository
import java.time.format.DateTimeFormatter
import com.example.medicare.user.reminder.MedicationReminderActivity
import com.example.medicare.notification.NotificationRegistration
import com.example.medicare.notification.NotificationPermission
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.user.medical.MedicalHistoryActivity
import com.example.medicare.user.review.MyReviewsActivity
import com.example.medicare.user.chat.PatientChatsActivity
import com.example.medicare.user.settings.PatientSettingsFragment
import com.example.medicare.notification.NotificationsActivity
import com.example.medicare.ui.base.RoleCatalogDialog
import com.example.medicare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class PatientHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientHomeBinding
    private val appointments = AppointmentRepository()
    private var nextAppointment: Appointment? = null
    private var selectedTab = TAB_HOME
    private lateinit var tabBack: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(binding.patientNavAccount, binding.patientNavHome, binding.patientNavSearch,
            binding.patientNavBooking, binding.patientNavCatalog).forEach(::styleNavIcon)
        tabBack = onBackPressedDispatcher.addCallback(this, enabled = false) { showTab(TAB_HOME) }
        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB)
            ?: intent.getIntExtra(EXTRA_TAB, TAB_HOME)
        NotificationRegistration.registerCurrentDevice()
        NotificationPermission.requestIfNeeded(this)

        binding.patientBrowseDoctors.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientAllDoctors.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientSearchShortcut.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientMenuButton.setOnClickListener { showCatalog() }
        binding.patientHeaderNotifications.setOnClickListener {
            if (selectedTab == TAB_ACCOUNT) startActivity(Intent(this, NotificationsActivity::class.java))
            else showTab(TAB_ACCOUNT)
        }
        binding.patientNavAccount.setOnClickListener { showTab(TAB_ACCOUNT) }
        binding.patientNavHome.setOnClickListener { showTab(TAB_HOME); binding.patientHomeScroll.smoothScrollTo(0, 0) }
        binding.patientNavSearch.setOnClickListener { showTab(TAB_SEARCH) }
        binding.patientNavBooking.setOnClickListener { showTab(TAB_BOOKING) }
        binding.patientNavCatalog.setOnClickListener { showCatalog() }
        binding.patientBookingShortcut.setOnClickListener { showTab(TAB_BOOKING) }
        binding.patientChatShortcut.setOnClickListener { startActivity(Intent(this, PatientChatsActivity::class.java)) }
        binding.patientMyAppointments.setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        binding.patientUpcomingAppointment.setOnClickListener {
            nextAppointment?.let { PatientDetailSheets.appointment(this, it, ::loadDashboard) }
                ?: startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        binding.patientMedicationReminders.setOnClickListener {
            startActivity(Intent(this, MedicationReminderActivity::class.java))
        }
        binding.patientMedicalHistory.setOnClickListener { startActivity(Intent(this, MedicalHistoryActivity::class.java)) }
        binding.patientMyReviews.setOnClickListener { startActivity(Intent(this, MyReviewsActivity::class.java)) }
        if (savedInstanceState == null || (selectedTab != TAB_HOME && supportFragmentManager.findFragmentById(binding.patientFragmentContainer.id) == null)) {
            showTab(selectedTab)
        } else updateNavigation()
        if (intent.getBooleanExtra(EXTRA_OPEN_CATALOG, false)) binding.root.post { showCatalog() }
        loadDashboard()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showTab(intent.getIntExtra(EXTRA_TAB, TAB_HOME))
        if (intent.getBooleanExtra(EXTRA_OPEN_CATALOG, false)) binding.root.post { showCatalog() }
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
            val fragment = when (tab) {
                TAB_ACCOUNT -> PatientSettingsFragment()
                TAB_SEARCH -> DoctorDirectoryFragment()
                else -> PatientBookingFragment.newInstance(doctor)
            }
            supportFragmentManager.beginTransaction()
                .replace(binding.patientFragmentContainer.id, fragment)
                .commit()
        } else {
            supportFragmentManager.findFragmentById(binding.patientFragmentContainer.id)?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
            loadDashboard()
        }
        updateNavigation()
    }

    private fun updateNavigation() {
        binding.patientHomeScroll.visibility = if (selectedTab == TAB_HOME) View.VISIBLE else View.GONE
        binding.patientFragmentContainer.visibility = if (selectedTab == TAB_HOME) View.GONE else View.VISIBLE
        binding.patientHeaderTitle.text = when (selectedTab) {
            TAB_ACCOUNT -> "Tài khoản"
            TAB_SEARCH -> "Tìm bác sĩ & chuyên khoa"
            TAB_BOOKING -> "Đặt lịch khám"
            else -> "Trang chủ bệnh nhân"
        }
        listOf(binding.patientNavHome to TAB_HOME, binding.patientNavSearch to TAB_SEARCH,
            binding.patientNavBooking to TAB_BOOKING, binding.patientNavAccount to TAB_ACCOUNT).forEach { (view, tab) ->
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
            selectedIndex = when (selectedTab) { TAB_ACCOUNT -> 9; TAB_SEARCH -> 2; TAB_BOOKING -> 3; else -> 1 }) { index ->
                if (index == 1) { showTab(TAB_HOME); return@show }
                if (index == 2) { showTab(TAB_SEARCH); return@show }
                if (index == 3) { showTab(TAB_BOOKING); return@show }
                if (index == 0 || index == 9) { showTab(TAB_ACCOUNT); return@show }
                val destination = when (index) {
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
            nextAppointment = next
            binding.patientUpcomingDoctor.text = next?.let { "BS. ${it.doctorName}" } ?: "Lịch khám sắp tới"
            binding.patientUpcomingTime.text = next?.let {
                "${runCatching { LocalDate.parse(it.appointmentDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(it.appointmentDate)} · ${it.startTime} · ${it.specialtyName}"
            } ?: getString(R.string.patient_no_upcoming)
            binding.patientUpcomingStatus.text = when (next?.status) {
                AppointmentStatus.PENDING -> "Chờ duyệt"
                AppointmentStatus.CONFIRMED -> "Đã xác nhận"
                else -> ""
            }
        }
        DoctorManagementRepository(this).getActiveDoctors { result ->
            val doctors = result.getOrDefault(emptyList())
            DoctorReviewRepository().getAll { reviewsResult ->
                if (isFinishing || isDestroyed) return@getAll
                val ratings = reviewsResult.getOrDefault(emptyList()).groupBy { it.doctorId }.mapValues { (_, values) ->
                    values.map { it.rating }.average() to values.size
                }
                renderFeaturedDoctors(doctors.take(2), ratings)
            }
        }
    }

    private fun renderFeaturedDoctors(doctors: List<Doctor>, ratings: Map<String, Pair<Double, Int>>) {
        binding.patientFeaturedDoctorsList.removeAllViews()
        if (doctors.isEmpty()) {
            binding.patientFeaturedDoctorsList.addView(TextView(this).apply {
                text = "Chưa có bác sĩ đang hoạt động."
                setTextColor(getColor(R.color.manager_muted))
                textSize = 11f
            })
            return
        }
        fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
        doctors.forEach { doctor ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), dp(11), dp(13), dp(11))
                setBackgroundResource(R.drawable.manager_card_background)
                layoutParams = LinearLayout.LayoutParams(-1, dp(74)).apply { bottomMargin = dp(9) }
            }
            row.addView(TextView(this).apply {
                text = "👨🏻‍⚕️"; textSize = 22f; gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.patient_avatar_background)
            }, LinearLayout.LayoutParams(dp(43), dp(43)))
            val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
            detail.addView(TextView(this).apply {
                text = "BS. ${doctor.fullName}"; textSize = 12f
                typeface = resources.getFont(R.font.inter_bold); setTextColor(getColor(R.color.manager_ink))
            })
            detail.addView(TextView(this).apply {
                val rating = ratings[doctor.id]?.let { "⭐ ${"%.1f".format(it.first)}" } ?: "Chưa có đánh giá"
                text = "${doctor.specialty} · $rating"; textSize = 10f
                setTextColor(getColor(R.color.manager_muted))
            })
            row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply { text = "›"; textSize = 22f; setTextColor(getColor(R.color.manager_muted)) })
            row.setOnClickListener { PatientDetailSheets.doctor(this, doctor, ratings[doctor.id]) { showBooking(doctor) } }
            binding.patientFeaturedDoctorsList.addView(row)
        }
    }

    companion object {
        const val TAB_HOME = 0
        const val TAB_SEARCH = 1
        const val TAB_BOOKING = 2
        const val TAB_ACCOUNT = 3
        const val EXTRA_TAB = "patient_tab"
        const val EXTRA_OPEN_CATALOG = "patient_open_catalog"
        private const val STATE_SELECTED_TAB = "patient_selected_tab"
    }
}
