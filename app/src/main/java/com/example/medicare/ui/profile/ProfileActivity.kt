package com.example.medicare.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val avatarPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val uid = auth.currentUser?.uid ?: return@registerForActivityResult
        if (uri != null) FirebaseStorage.getInstance().reference.child("avatars/$uid").putFile(uri).continueWithTask { FirebaseStorage.getInstance().reference.child("avatars/$uid").downloadUrl }.addOnSuccessListener { url -> firestore.collection("users").document(uid).update("avatarUrl", url.toString(), "updatedAt", FieldValue.serverTimestamp()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.profileBack.setOnClickListener { finish() }
        binding.profileSave.setOnClickListener { saveProfile() }
        binding.profileChooseAvatar.setOnClickListener { avatarPicker.launch("image/*") }
        binding.profileChangePassword.setOnClickListener { changePassword() }
        loadProfile()
        if (intent.getBooleanExtra(EXTRA_OPEN_PASSWORD, false)) {
            binding.root.post { if (!isFinishing) changePassword() }
        }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get().addOnSuccessListener { document ->
            binding.profileName.setText(document.getString("fullName").orEmpty())
            binding.profilePhone.setText(document.getString("phone").orEmpty())
            binding.profileEmail.setText(document.getString("email").orEmpty())
            if (document.getString("type") == "DOCTOR") {
                binding.profileDoctorInfo.visibility = View.VISIBLE
                loadDoctorProfile(uid)
            }
        }
    }

    private fun loadDoctorProfile(uid: String) {
        firestore.collection("doctors").document(uid).get().addOnSuccessListener { document ->
            binding.profileSpecialty.setText(document.getString("specialtyName").orEmpty())
            binding.profileExperience.setText(document.getString("experienceYears").orEmpty())
            binding.profileBiography.setText(document.getString("biography").orEmpty())
            binding.profileWorkplace.setText(document.getString("workplace").orEmpty())
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.profileName.text.toString().trim()
        if (name.isBlank()) { binding.profileName.error = getString(R.string.manager_required_field); return }
        firestore.collection("users").document(uid).update(mapOf(
            "fullName" to name,
            "phone" to binding.profilePhone.text.toString().trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener {
            if (binding.profileDoctorInfo.visibility == View.VISIBLE) {
                firestore.collection("doctors").document(uid).update(mapOf(
                    "biography" to binding.profileBiography.text.toString().trim(),
                    "workplace" to binding.profileWorkplace.text.toString().trim(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )).addOnSuccessListener { finish() }
                    .addOnFailureListener { Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_SHORT).show() }
            } else finish()
        }
            .addOnFailureListener { Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_SHORT).show() }
    }

    private fun changePassword() {
        val input = EditText(this).apply { hint = getString(R.string.profile_new_password); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle(R.string.profile_change_password).setView(input).setNegativeButton(R.string.manager_cancel, null).setPositiveButton(R.string.profile_change_password) { _, _ ->
            val password = input.text.toString(); if (password.length < 8) { Toast.makeText(this, R.string.auth_invalid_password, Toast.LENGTH_SHORT).show() } else auth.currentUser?.updatePassword(password)?.addOnSuccessListener { Toast.makeText(this, R.string.profile_password_changed, Toast.LENGTH_SHORT).show() }?.addOnFailureListener { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
        }.show()
    }

    companion object {
        const val EXTRA_OPEN_PASSWORD = "open_password"
    }
}
