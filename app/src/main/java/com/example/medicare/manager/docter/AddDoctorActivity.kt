package com.example.medicare.manager.docter

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.databinding.ActivityAddDoctorBinding

class AddDoctorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddDoctorBinding
    private var specialties: List<ManagedSpecialty> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addDoctorBack.setOnClickListener { finish() }
        binding.addDoctorSave.setOnClickListener { saveDoctor() }
        loadSpecialties()
    }

    private fun loadSpecialties() {
        DoctorManagementRepository(this).getSpecialties { result ->
            result.onSuccess { loadedSpecialties ->
                specialties = loadedSpecialties
                binding.addDoctorSpecialty.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, specialties.map { it.name }),
                )
            }.onFailure { showError(it) }
        }
    }

    private fun saveDoctor() {
        val fullName = binding.addDoctorFullName.text?.toString().orEmpty().trim()
        val specialty = binding.addDoctorSpecialty.text?.toString().orEmpty().trim()
        val email = binding.addDoctorEmail.text?.toString().orEmpty().trim()
        val phone = binding.addDoctorPhone.text?.toString().orEmpty().trim()
        val experience = binding.addDoctorExperience.text?.toString().orEmpty().trim()
        val qualification = binding.addDoctorQualification.text?.toString().orEmpty().trim()
        val biography = binding.addDoctorBiography.text?.toString().orEmpty().trim()
        val password = binding.addDoctorPassword.text?.toString().orEmpty()
        val selectedSpecialty = specialties.firstOrNull { it.name == specialty }

        binding.addDoctorFullNameLayout.error = if (fullName.isEmpty()) getString(R.string.manager_required_field) else null
        binding.addDoctorSpecialtyLayout.error = when {
            specialty.isEmpty() -> getString(R.string.manager_required_field)
            selectedSpecialty == null -> getString(R.string.manager_select_specialty)
            else -> null
        }
        binding.addDoctorEmailLayout.error = when {
            email.isEmpty() -> getString(R.string.manager_required_field)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> getString(R.string.auth_invalid_email)
            else -> null
        }
        binding.addDoctorPhoneLayout.error = if (phone.isEmpty()) getString(R.string.manager_required_field) else null
        binding.addDoctorPasswordLayout.error = if (password.length < 8) getString(R.string.auth_invalid_password) else null
        if (listOf(
                binding.addDoctorFullNameLayout.error,
                binding.addDoctorSpecialtyLayout.error,
                binding.addDoctorEmailLayout.error,
                binding.addDoctorPhoneLayout.error,
                binding.addDoctorPasswordLayout.error,
            ).any { it != null }) return

        binding.addDoctorSave.isEnabled = false
        DoctorManagementRepository(this).createDoctorAccount(
            fullName = fullName,
            specialty = selectedSpecialty!!,
            email = email,
            password = password,
            phone = phone,
            experienceYears = experience,
            qualification = qualification,
            biography = biography,
            isActive = binding.addDoctorActive.isChecked,
        ) { result ->
            binding.addDoctorSave.isEnabled = true
            result.onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure { showError(it) }
        }
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
    }
}
