package com.example.medicare.doctor.dashboard

import android.app.DatePickerDialog
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.ActivityDoctorHomeBinding
import com.example.medicare.databinding.ItemDoctorAppointmentBinding
import com.example.medicare.databinding.SheetDoctorVisitBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.example.medicare.doctor.DoctorHomeActivity
import com.example.medicare.doctor.medical.CompleteAppointmentActivity
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class DoctorDashboardFragment : Fragment() {
    companion object {
        const val ARG_COMPLETED_ONLY = "completed_only"
        const val ARG_SHOW_OVERVIEW = "show_overview"
    }
    private var _binding: ActivityDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private val repository = AppointmentRepository()
    private var visitDialog: BottomSheetDialog? = null
    private var appointments: List<Appointment> = emptyList()
    private var selectedDate: LocalDate? = null
    private var selectedStatus: AppointmentStatus? = null
    private val showOverview get() = arguments?.getBoolean(ARG_SHOW_OVERVIEW) == true
    private val showExam get() = arguments?.getBoolean(ARG_COMPLETED_ONLY) == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = ActivityDoctorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.doctorHero.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.doctorStatsTop.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.doctorStatsBottom.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.doctorUtilities.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.doctorAppointmentsSection.visibility = if (showOverview) View.VISIBLE else View.GONE
        binding.doctorAppointmentsHeader.visibility = if (!showOverview && !showExam) View.VISIBLE else View.GONE
        binding.doctorExamHeading.visibility = if (showExam) View.VISIBLE else View.GONE
        binding.doctorInfoBox.visibility = if (showOverview) View.GONE else View.VISIBLE
        binding.doctorInfoBox.text = if (showExam)
            "ℹ️ Lưu kết quả khám khi đang thực hiện lượt khám. Sau khi công bố, bệnh nhân có thể xem trong Hồ sơ sức khỏe."
        else "ℹ️ Bác sĩ chỉ nhận lịch do Admin đã duyệt. Nếu muốn nghỉ hoặc đổi ca, hãy gửi yêu cầu tại Lịch làm việc."
        binding.doctorAppointmentsSection.setOnClickListener {
            (activity as? DoctorHomeActivity)?.showTab(DoctorHomeActivity.TAB_APPOINTMENTS)
        }
        binding.doctorHomeScroll.post { binding.doctorHomeScroll.scrollTo(0, 0) }
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { profile ->
                    if (_binding != null) {
                        val name = profile.getString("fullName").orEmpty().trim()
                        if (name.isNotBlank()) binding.doctorGreeting.text = "Chào BS.\n$name 👋"
                    }
                }
        }
        binding.doctorScheduleDate.setOnClickListener { pickDate() }
        binding.doctorConfirmedTab.setOnClickListener { selectStatus(AppointmentStatus.CONFIRMED) }
        binding.doctorInProgressTab.setOnClickListener { selectStatus(AppointmentStatus.IN_PROGRESS) }
        binding.doctorCompletedTab.setOnClickListener { selectStatus(AppointmentStatus.COMPLETED) }
        binding.doctorAllTab.setOnClickListener { selectStatus(null) }
        binding.doctorSearchPatients.doAfterTextChanged { render() }
        binding.doctorExamShortcut.setOnClickListener { (activity as? DoctorHomeActivity)?.showTab(DoctorHomeActivity.TAB_EXAM) }
        binding.doctorPrescriptionShortcut.setOnClickListener {
            (activity as? DoctorHomeActivity)?.showTab(DoctorHomeActivity.TAB_PRESCRIPTIONS)
        }
        binding.doctorChatShortcut.setOnClickListener { (activity as? DoctorHomeActivity)?.showTab(DoctorHomeActivity.TAB_CHATS) }
        binding.doctorScheduleShortcut.setOnClickListener { (activity as? DoctorHomeActivity)?.showTab(DoctorHomeActivity.TAB_SCHEDULE) }
        selectStatus(selectedStatus)
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.getAppointmentsForDoctor(uid) { result ->
            if (_binding == null) return@getAppointmentsForDoctor
            result.onSuccess { appointments = it; render() }.onFailure {
                binding.doctorAppointmentsEmpty.text = it.message ?: getString(R.string.doctor_appointments_load_failed)
                binding.doctorAppointmentsEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun pickDate() {
        val day = selectedDate ?: LocalDate.now()
        val calendar = Calendar.getInstance().apply { set(day.year, day.monthValue - 1, day.dayOfMonth) }
        DatePickerDialog(requireContext(), { _, year, month, date -> selectedDate = LocalDate.of(year, month + 1, date); render() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            .apply { setButton(AlertDialog.BUTTON_NEUTRAL, "Tất cả ngày") { _, _ -> selectedDate = null; render() } }
            .show()
    }

    private fun selectStatus(status: AppointmentStatus?) { selectedStatus = status; render() }

    private fun render() {
        if (_binding == null) return
        binding.doctorScheduleDate.text = (selectedDate ?: LocalDate.now()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        mapOf(null to binding.doctorAllTab, AppointmentStatus.CONFIRMED to binding.doctorConfirmedTab,
            AppointmentStatus.IN_PROGRESS to binding.doctorInProgressTab, AppointmentStatus.COMPLETED to binding.doctorCompletedTab).forEach { (status, view) ->
            view.isSelected = status == selectedStatus
            view.setTextColor(ContextCompat.getColor(requireContext(), if (view.isSelected) R.color.white else R.color.manager_muted))
        }
        val day = appointments.filter { it.appointmentDate == LocalDate.now().toString() }
        binding.doctorConfirmedCount.text = appointments.count { it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS) }.toString()
        binding.doctorPendingCount.text = appointments.count { it.status == AppointmentStatus.PENDING }.toString()
        binding.doctorPatientCount.text = day.map { it.patientId }.distinct().size.toString()
        binding.doctorCompletedCount.text = appointments.count { it.status == AppointmentStatus.COMPLETED }.toString()
        binding.doctorDailySummary.text = getString(R.string.doctor_daily_summary, day.size, day.count { it.status == AppointmentStatus.CONFIRMED }, day.count { it.status == AppointmentStatus.ARRIVED }, day.count { it.status == AppointmentStatus.COMPLETED })
        val visible = if (showOverview) {
            appointments.filter { it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS) }
                .sortedWith(compareBy<Appointment> { it.appointmentDate }.thenBy { it.startTime }).take(3)
        } else if (showExam) {
            appointments.filter { it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED) }
                .sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenBy { it.startTime })
        } else {
            appointments.filter { it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED) }
                .filter { selectedDate == null || it.appointmentDate == selectedDate.toString() }
                .filter { selectedStatus == null || if (selectedStatus == AppointmentStatus.IN_PROGRESS)
                    it.status in setOf(AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS) else it.status == selectedStatus }
        }
        binding.doctorAppointmentsEmpty.text = if (showOverview)
            "Chưa có lịch khám đã xác nhận. Khi Admin duyệt, lịch sẽ xuất hiện tại đây."
        else if (showExam) "Chưa có lần khám được duyệt."
        else getString(R.string.doctor_no_appointments)
        binding.doctorAppointmentsList.removeAllViews()
        binding.doctorAppointmentsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach(::addAppointment)
    }

    private fun addAppointment(appointment: Appointment) {
        val row = ItemDoctorAppointmentBinding.inflate(layoutInflater, binding.doctorAppointmentsList, false)
        row.doctorAppointmentInitials.text = if (showExam) "🩺" else "👤"
        row.doctorAppointmentPatientName.text = appointment.patientName.ifBlank { getString(R.string.doctor_patient) }
        val date = runCatching { LocalDate.parse(appointment.appointmentDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
            .getOrDefault(appointment.appointmentDate)
        row.doctorAppointmentTime.text = "$date · ${appointment.startTime} · ${appointment.reason.ifBlank { appointment.specialtyName }}"
        row.doctorAppointmentStatus.text = if (showExam)
            if (appointment.status == AppointmentStatus.COMPLETED) "✅ Đã công bố kết quả" else "✍️ Chưa có kết quả công bố"
        else label(appointment.status)
        row.doctorAppointmentStatus.setTextColor(ContextCompat.getColor(requireContext(), if (appointment.status == AppointmentStatus.COMPLETED) R.color.manager_green else R.color.manager_orange))
        row.root.setOnClickListener { showAppointmentActions(appointment) }
        binding.doctorAppointmentsList.addView(row.root)
    }

    private fun showAppointmentActions(appointment: Appointment) {
        visitDialog?.dismiss()
        val sheet = SheetDoctorVisitBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        visitDialog = dialog
        dialog.setContentView(sheet.root)
        var current = appointment
        sheet.doctorVisitName.text = appointment.patientName
        sheet.doctorVisitId.text = appointment.id
        sheet.doctorVisitDoctor.text = "BS. ${appointment.doctorName}"
        sheet.doctorVisitSpecialty.text = appointment.specialtyName
        sheet.doctorVisitDate.text = runCatching { LocalDate.parse(appointment.appointmentDate)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(appointment.appointmentDate)
        sheet.doctorVisitTime.text = "${appointment.startTime} – ${appointment.endTime}"
        sheet.doctorVisitReason.text = appointment.reason.ifBlank { getString(R.string.doctor_no_reason) }
        fun renderState() {
            sheet.doctorVisitStatus.text = label(current.status)
            sheet.doctorVisitArrived.visibility = if (current.status == AppointmentStatus.CONFIRMED) View.VISIBLE else View.GONE
            sheet.doctorVisitExam.visibility = if (current.status in setOf(AppointmentStatus.CONFIRMED,
                AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)) View.VISIBLE else View.GONE
            sheet.doctorVisitExam.isEnabled = current.status != AppointmentStatus.CONFIRMED
            sheet.doctorVisitExam.text = when (current.status) {
                AppointmentStatus.IN_PROGRESS -> "Ghi kết quả khám"
                AppointmentStatus.COMPLETED -> "Xem kết quả khám"
                else -> "Bắt đầu khám"
            }
            sheet.doctorVisitHint.text = when (current.status) {
                AppointmentStatus.CONFIRMED -> "Xác nhận bệnh nhân đã đến trước khi bắt đầu khám."
                AppointmentStatus.ARRIVED -> "✓ Đã ghi nhận bệnh nhân đến. Bạn có thể bắt đầu khám."
                AppointmentStatus.IN_PROGRESS -> "Đang khám · Nhập kết quả và lưu nháp hoặc hoàn tất lượt khám."
                AppointmentStatus.COMPLETED -> "✓ Lượt khám đã hoàn tất."
                else -> "Lịch khám chưa sẵn sàng để tiếp nhận."
            }
            sheet.doctorVisitChat.visibility = sheet.doctorVisitExam.visibility
        }
        fun updateStatus(arrived: Boolean) {
            sheet.doctorVisitArrived.isEnabled = false
            sheet.doctorVisitExam.isEnabled = false
            val action = if (arrived) repository::markPatientArrived else repository::startAppointment
            action(current.id) { result ->
                if (_binding == null) return@action
                result.onSuccess {
                    current = current.copy(status = if (arrived) AppointmentStatus.ARRIVED else AppointmentStatus.IN_PROGRESS)
                    load()
                    if (!dialog.isShowing) return@onSuccess
                    if (arrived) renderState() else { dialog.dismiss(); openAppointment(current) }
                }.onFailure {
                    if (!dialog.isShowing) return@onFailure
                    sheet.doctorVisitArrived.isEnabled = true
                    renderState()
                    Toast.makeText(requireContext(), it.message ?: getString(R.string.doctor_start_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
        sheet.doctorVisitClose.setOnClickListener { dialog.dismiss() }
        sheet.doctorVisitArrived.setOnClickListener { updateStatus(true) }
        sheet.doctorVisitExam.setOnClickListener {
            if (current.status == AppointmentStatus.ARRIVED) updateStatus(false)
            else { dialog.dismiss(); openAppointment(current) }
        }
        sheet.doctorVisitChat.setOnClickListener { dialog.dismiss(); openChat(current) }
        renderState()
        dialog.show()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }

    private fun label(status: AppointmentStatus) = getString(when (status) {
        AppointmentStatus.CONFIRMED -> R.string.doctor_status_confirmed
        AppointmentStatus.ARRIVED -> R.string.doctor_status_arrived
        AppointmentStatus.IN_PROGRESS -> R.string.doctor_status_in_progress
        AppointmentStatus.COMPLETED -> R.string.doctor_status_completed
        AppointmentStatus.PENDING -> R.string.manager_pending
        AppointmentStatus.REJECTED -> R.string.manager_reject
        AppointmentStatus.CANCELLED -> R.string.doctor_status_cancelled
    })

    private fun openChat(a: Appointment) = (activity as? DoctorHomeActivity)?.showChatWithPatient(a.patientId)
    private fun openAppointment(a: Appointment) = startActivity(Intent(requireContext(), CompleteAppointmentActivity::class.java).putExtra(CompleteAppointmentActivity.EXTRA_ID, a.id).putExtra(CompleteAppointmentActivity.EXTRA_PATIENT_ID, a.patientId).putExtra(CompleteAppointmentActivity.EXTRA_PATIENT_NAME, a.patientName).putExtra(CompleteAppointmentActivity.EXTRA_DOCTOR_ID, a.doctorId).putExtra(CompleteAppointmentActivity.EXTRA_DOCTOR_NAME, a.doctorName).putExtra(CompleteAppointmentActivity.EXTRA_SPECIALTY, a.specialtyName).putExtra(CompleteAppointmentActivity.EXTRA_SCHEDULE_ID, a.scheduleId).putExtra(CompleteAppointmentActivity.EXTRA_DATE, a.appointmentDate).putExtra(CompleteAppointmentActivity.EXTRA_START_TIME, a.startTime).putExtra(CompleteAppointmentActivity.EXTRA_END_TIME, a.endTime).putExtra(CompleteAppointmentActivity.EXTRA_REASON, a.reason).putExtra(CompleteAppointmentActivity.EXTRA_STATUS, a.status.name))
    override fun onDestroyView() { visitDialog?.dismiss(); visitDialog = null; _binding = null; super.onDestroyView() }
}
