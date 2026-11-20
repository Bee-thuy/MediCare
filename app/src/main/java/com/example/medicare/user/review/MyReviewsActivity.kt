package com.example.medicare.user.review

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.ActivityMyReviewsBinding
import com.example.medicare.databinding.DialogPatientReviewBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.DoctorReview
import com.example.medicare.user.PatientScreenChrome
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Completed visits and service feedback, arranged like the patient HTML reference. */
class MyReviewsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyReviewsBinding
    private val reviewsRepository = DoctorReviewRepository()
    private var completed: List<Appointment> = emptyList()
    private var reviews: List<DoctorReview> = emptyList()
    private var appointmentsLoaded = false
    private var reviewsLoaded = false
    private var pendingAppointmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReviewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PatientScreenChrome.bind(this, "Đánh giá & phản hồi")
        binding.myReviewsBack.setOnClickListener { finish() }
        binding.myReviewsSendFeedback.setOnClickListener { sendFeedback() }
        pendingAppointmentId = intent.getStringExtra(EXTRA_OPEN_APPOINTMENT_ID)
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        appointmentsLoaded = false
        reviewsLoaded = false
        AppointmentRepository().getAppointmentsForPatient(uid) { result ->
            if (isFinishing || isDestroyed) return@getAppointmentsForPatient
            appointmentsLoaded = true
            result.onSuccess { completed = it.filter { appointment -> appointment.status == AppointmentStatus.COMPLETED } }
                .onFailure { toast(it.message ?: "Không tải được lịch khám.") }
            if (reviewsLoaded) render()
        }
        reviewsRepository.getMine { result ->
            if (isFinishing || isDestroyed) return@getMine
            reviewsLoaded = true
            result.onSuccess { reviews = it }.onFailure { toast(it.message ?: "Không tải được đánh giá.") }
            if (appointmentsLoaded) render()
        }
    }

    private fun render() {
        binding.myReviewsList.removeAllViews()
        binding.myReviewsEmpty.visibility = if (completed.isEmpty()) View.VISIBLE else View.GONE
        completed.forEach { appointment ->
            val existing = reviews.firstOrNull { it.appointmentId == appointment.id }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundResource(R.drawable.manager_card_background)
                layoutParams = LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(9) }
            }
            row.addView(TextView(this).apply {
                text = "👨🏻‍⚕️"
                textSize = 22f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.patient_avatar_background)
            }, LinearLayout.LayoutParams(dp(43), dp(43)))
            val detail = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
            }
            detail.addView(TextView(this).apply {
                text = "BS. ${appointment.doctorName}"
                textSize = 12f
                typeface = resources.getFont(R.font.inter_bold)
                setTextColor(getColor(R.color.manager_ink))
            })
            detail.addView(TextView(this).apply {
                text = "Hoàn thành: ${formatDate(appointment.appointmentDate)}"
                textSize = 10f
                setTextColor(getColor(R.color.manager_muted))
            })
            detail.addView(TextView(this).apply {
                text = if (existing == null) "Chạm để đánh giá" else "⭐ Đã đánh giá · ${existing.rating}/5"
                textSize = 10f
                setTextColor(getColor(R.color.manager_muted))
            })
            row.addView(detail, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = "›"
                textSize = 24f
                setTextColor(getColor(R.color.manager_muted))
            })
            row.setOnClickListener {
                if (existing == null) showReviewSheet(appointment)
                else toast("Bạn đã đánh giá lần khám này.")
            }
            binding.myReviewsList.addView(row)
        }
        pendingAppointmentId?.let { id ->
            pendingAppointmentId = null
            completed.firstOrNull { it.id == id && reviews.none { review -> review.appointmentId == id } }
                ?.let(::showReviewSheet)
        }
    }

    private fun showReviewSheet(appointment: Appointment) {
        val sheet = BottomSheetDialog(this)
        val form = DialogPatientReviewBinding.inflate(layoutInflater)
        form.reviewDialogDoctor.text = "BS. ${appointment.doctorName}"
        form.reviewDialogDate.text = "Lịch hoàn thành ngày ${formatDate(appointment.appointmentDate)}"
        form.reviewDialogClose.setOnClickListener { sheet.dismiss() }
        form.reviewDialogSend.setOnClickListener {
            val rating = form.reviewDialogRating.rating.toInt()
            if (rating !in 1..5) return@setOnClickListener toast("Vui lòng chọn số sao.")
            form.reviewDialogSend.isEnabled = false
            reviewsRepository.save(appointment.id, appointment.doctorId, appointment.doctorName,
                rating, form.reviewDialogComment.text.toString().trim()) { result ->
                form.reviewDialogSend.isEnabled = true
                result.onSuccess {
                    sheet.dismiss()
                    reviewsRepository.getMine { fresh ->
                        fresh.onSuccess { reviews = it; render() }
                            .onFailure { toast(it.message ?: "Không tải được đánh giá.") }
                    }
                    toast("Đã gửi đánh giá.")
                }.onFailure { toast(it.message ?: "Không gửi được đánh giá.") }
            }
        }
        sheet.setContentView(form.root)
        form.root.requestFocus()
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sheet.show()
    }

    private fun sendFeedback() {
        val text = binding.myReviewsFeedback.text.toString().trim()
        if (text.isEmpty()) return toast("Vui lòng nhập phản hồi.")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return toast("Vui lòng đăng nhập lại.")
        binding.myReviewsSendFeedback.isEnabled = false
        val document = FirebaseFirestore.getInstance().collection("serviceFeedback").document()
        document.set(mapOf("id" to document.id, "patientId" to uid, "message" to text,
            "createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener {
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                binding.myReviewsSendFeedback.isEnabled = true
                binding.myReviewsFeedback.text?.clear()
                toast("Đã gửi phản hồi.")
            }.addOnFailureListener {
                if (isFinishing || isDestroyed) return@addOnFailureListener
                binding.myReviewsSendFeedback.isEnabled = true
                toast(it.message ?: "Không gửi được phản hồi.")
            }
    }

    private fun formatDate(value: String): String = try {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) { value }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_OPEN_APPOINTMENT_ID = "open_review_appointment_id" }
}
