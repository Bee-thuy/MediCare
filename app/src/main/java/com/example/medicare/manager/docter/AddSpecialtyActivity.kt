package com.example.medicare.manager.docter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.databinding.ActivityAddSpecialtyBinding

class AddSpecialtyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSpecialtyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSpecialtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addSpecialtyBack.setOnClickListener { finish() }
        binding.addSpecialtySave.setOnClickListener { saveSpecialty() }
    }

    private fun saveSpecialty() {
        val name = binding.addSpecialtyName.text?.toString().orEmpty().trim()
        val description = binding.addSpecialtyDescription.text?.toString().orEmpty().trim()
        binding.addSpecialtyNameLayout.error = if (name.isEmpty()) getString(R.string.manager_required_field) else null
        if (binding.addSpecialtyNameLayout.error != null) return

        binding.addSpecialtySave.isEnabled = false
        DoctorManagementRepository(this).addSpecialty(name, description) { result ->
            binding.addSpecialtySave.isEnabled = true
            result.onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure {
                Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
