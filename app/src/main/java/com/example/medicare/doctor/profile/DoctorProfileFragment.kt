package com.example.medicare.doctor.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.ActivityDoctorProfileBinding
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class DoctorProfileFragment : Fragment() {
    private var _binding: ActivityDoctorProfileBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val binding = ActivityDoctorProfileBinding.inflate(inflater, container, false)
        _binding = binding
        binding.doctorProfileHeader.visibility = View.GONE
        binding.doctorProfileEdit.setOnClickListener { startActivity(Intent(requireContext(), ProfileActivity::class.java)) }
        binding.doctorProfilePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java)
                .putExtra(ProfileActivity.EXTRA_OPEN_PASSWORD, true))
        }
        binding.doctorProfileLogout.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            requireActivity().finish()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { user ->
                val binding = _binding ?: return@addOnSuccessListener
                binding.doctorProfileName.text = "BS. ${user.getString("fullName").orEmpty()}"
                binding.doctorProfilePhone.text = user.getString("phone").orEmpty().ifBlank { "Chưa cập nhật" }
            }
            .addOnFailureListener { if (isAdded) Toast.makeText(requireContext(), "Không tải được hồ sơ bác sĩ.", Toast.LENGTH_SHORT).show() }
        FirebaseFirestore.getInstance().collection("doctors").document(uid).get()
            .addOnSuccessListener { doctor ->
                val binding = _binding ?: return@addOnSuccessListener
                val specialty = doctor.getString("specialtyName").orEmpty().ifBlank { "Chưa cập nhật" }
                val experience = doctor.getString("experienceYears").orEmpty().ifBlank { "Chưa cập nhật" }
                binding.doctorProfileSpecialty.text = "$specialty · ${if (experience == "Chưa cập nhật") experience else "$experience năm kinh nghiệm"}"
                binding.doctorProfileFieldSpecialty.text = specialty
                binding.doctorProfileExperience.text = if (experience == "Chưa cập nhật") experience else "$experience năm"
                val inactive = doctor.getBoolean("isActive") == false
                binding.doctorProfileStatus.text = if (inactive) "Đã khóa" else "Đang hoạt động"
                binding.doctorProfileStatus.setBackgroundResource(if (inactive) R.drawable.manager_status_inactive_background else R.drawable.manager_status_active_background)
                binding.doctorProfileStatus.setTextColor(requireContext().getColor(if (inactive) R.color.manager_red else R.color.doctor_teal_dark))
            }
            .addOnFailureListener { if (isAdded) Toast.makeText(requireContext(), "Không tải được chuyên môn bác sĩ.", Toast.LENGTH_SHORT).show() }
        DoctorReviewRepository().getAll { result ->
            val binding = _binding ?: return@getAll
            val ratings = result.getOrDefault(emptyList()).filter { it.doctorId == uid }.map { it.rating }
            binding.doctorProfileRating.text = if (ratings.isEmpty()) "⭐ Chưa có đánh giá" else
                "⭐ ${String.format(Locale.getDefault(), "%.1f", ratings.average())} / 5"
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
