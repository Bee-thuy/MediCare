package com.example.medicare.manager.doctor

import com.example.medicare.data.doctor.DoctorManagementRepository

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.medicare.R
import com.example.medicare.databinding.ActivityAddSpecialtyBinding

class AddSpecialtyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSpecialtyBinding
    private var selectedIconKey = "general"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSpecialtyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        selectedIconKey = savedInstanceState?.getString("selected_icon") ?: "general"
        renderIconChoice()

        binding.addSpecialtyBack.setOnClickListener { finish() }
        binding.addSpecialtyIconChoice.setOnClickListener { chooseIcon() }
        binding.addSpecialtySave.setOnClickListener { saveSpecialty() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selected_icon", selectedIconKey)
        super.onSaveInstanceState(outState)
    }

    private fun renderIconChoice() {
        binding.addSpecialtyIconChoice.text = SpecialtyVisuals.forKey(selectedIconKey).symbol
    }

    private fun chooseIcon() {
        val options = SpecialtyVisuals.options
        AlertDialog.Builder(this).setTitle("Chọn biểu tượng")
            .setSingleChoiceItems(options.map { "${it.symbol}  ${it.label}" }.toTypedArray(),
                options.indexOfFirst { it.key == selectedIconKey }) { dialog, index ->
                selectedIconKey = options[index].key
                renderIconChoice()
                dialog.dismiss()
            }.setNegativeButton("Đóng", null).show()
    }

    private fun saveSpecialty() {
        val name = binding.addSpecialtyName.text?.toString().orEmpty().trim()
        val description = ""
        binding.addSpecialtyNameLayout.error = if (name.isEmpty()) getString(R.string.manager_required_field) else null
        if (binding.addSpecialtyNameLayout.error != null) return

        binding.addSpecialtySave.isEnabled = false
        DoctorManagementRepository(this).addSpecialty(name, description, selectedIconKey) { result ->
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
