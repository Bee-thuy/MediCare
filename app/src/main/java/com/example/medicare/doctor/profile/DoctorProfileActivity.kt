package com.example.medicare.doctor.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.ActivityDoctorProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

/** HTML-style doctor profile summary backed by the existing Firebase profile editor. */
class DoctorProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.doctorProfileBack.setOnClickListener { finish() }
        binding.doctorProfileEdit.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.doctorProfilePassword.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)
                .putExtra(ProfileActivity.EXTRA_OPEN_PASSWORD, true))
        }
        binding.doctorProfileLogout.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { user ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                binding.doctorProfileName.text = "BS. ${user.getString("fullName").orEmpty()}"
                binding.doctorProfilePhone.text = user.getString("phone").orEmpty().ifBlank { "Chưa cập nhật" }
            }
            .addOnFailureListener { Toast.makeText(this, "Không tải được hồ sơ bác sĩ.", Toast.LENGTH_SHORT).show() }
        FirebaseFirestore.getInstance().collection("doctors").document(uid).get()
            .addOnSuccessListener { doctor ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                val specialty = doctor.getString("specialtyName").orEmpty().ifBlank { "Chưa cập nhật" }
                val experience = doctor.getString("experienceYears").orEmpty().ifBlank { "Chưa cập nhật" }
                binding.doctorProfileSpecialty.text = "$specialty · ${if (experience == "Chưa cập nhật") experience else "$experience năm kinh nghiệm"}"
                binding.doctorProfileFieldSpecialty.text = specialty
                binding.doctorProfileExperience.text = if (experience == "Chưa cập nhật") experience else "$experience năm"
                val inactive = doctor.getBoolean("isActive") == false
                binding.doctorProfileStatus.text = if (inactive) "Đã khóa" else "Đang hoạt động"
                binding.doctorProfileStatus.setBackgroundResource(if (inactive) R.drawable.manager_status_inactive_background else R.drawable.manager_status_active_background)
                binding.doctorProfileStatus.setTextColor(getColor(if (inactive) R.color.manager_red else R.color.doctor_teal_dark))
            }
            .addOnFailureListener { Toast.makeText(this, "Không tải được chuyên môn bác sĩ.", Toast.LENGTH_SHORT).show() }
        DoctorReviewRepository().getAll { result ->
            if (isFinishing || isDestroyed) return@getAll
            val ratings = result.getOrDefault(emptyList()).filter { it.doctorId == uid }.map { it.rating }
            binding.doctorProfileRating.text = if (ratings.isEmpty()) "⭐ Chưa có đánh giá" else
                "⭐ ${String.format(Locale.getDefault(), "%.1f", ratings.average())} / 5"
        }
    }
}
