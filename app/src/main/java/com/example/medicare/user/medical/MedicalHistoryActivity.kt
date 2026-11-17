package com.example.medicare.user.medical

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityMedicalHistoryBinding
import com.google.firebase.auth.FirebaseAuth

class MedicalHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicalHistoryBinding
    override fun onCreate(state: Bundle?) { super.onCreate(state); binding = ActivityMedicalHistoryBinding.inflate(layoutInflater); setContentView(binding.root); binding.medicalHistoryBack.setOnClickListener { finish() }; load() }
    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        MedicalRecordRepository().getRecordsForPatient(uid) { result -> result.onSuccess { records ->
            binding.medicalHistoryList.removeAllViews(); binding.medicalHistoryEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            records.forEach { record ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundResource(R.drawable.manager_card_background); layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = 10 } }
                row.addView(TextView(this).apply { text = "${record.doctorName} · ${record.appointmentDate}"; textSize = 16f; setTextColor(getColor(R.color.manager_ink)) })
                row.addView(TextView(this).apply { text = record.diagnosis.ifBlank { getString(R.string.patient_no_information) }; textSize = 13f; setTextColor(getColor(R.color.manager_muted)); setPadding(0,6,0,0) })
                row.setOnClickListener { startActivity(Intent(this, MedicalRecordActivity::class.java).putExtra(MedicalRecordActivity.EXTRA_APPOINTMENT_ID, record.appointmentId).putExtra(MedicalRecordActivity.EXTRA_DOCTOR_NAME, record.doctorName).putExtra(MedicalRecordActivity.EXTRA_SPECIALTY, record.specialtyName).putExtra(MedicalRecordActivity.EXTRA_DATE, record.appointmentDate)) }
                binding.medicalHistoryList.addView(row)
            }
        } }
    }
}
