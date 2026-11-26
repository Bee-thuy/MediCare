package com.example.medicare.manager.doctor

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.ActivityAddSpecialtyBinding
import com.example.medicare.databinding.ItemSpecialtyIconOptionBinding
import com.example.medicare.databinding.SheetOptionPickerBinding
import com.example.medicare.model.Specialty
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class AddSpecialtyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSpecialtyBinding
    private var selectedIconKey = "general"
    private var editingId: String? = null
    private var editingActive = true

    companion object {
        const val EXTRA_SPECIALTY_ID = "specialty_id"
        const val EXTRA_SPECIALTY_NAME = "specialty_name"
        const val EXTRA_SPECIALTY_DESCRIPTION = "specialty_description"
        const val EXTRA_SPECIALTY_ICON = "specialty_icon"
        const val EXTRA_SPECIALTY_ACTIVE = "specialty_active"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSpecialtyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * .88f).toInt())
        window.setGravity(Gravity.BOTTOM)

        editingId = savedInstanceState?.getString(EXTRA_SPECIALTY_ID) ?: intent.getStringExtra(EXTRA_SPECIALTY_ID)
        selectedIconKey = savedInstanceState?.getString(EXTRA_SPECIALTY_ICON)
            ?: intent.getStringExtra(EXTRA_SPECIALTY_ICON)
            ?: "general"
        editingActive = savedInstanceState?.getBoolean(EXTRA_SPECIALTY_ACTIVE)
            ?: intent.getBooleanExtra(EXTRA_SPECIALTY_ACTIVE, true)

        val editing = !editingId.isNullOrBlank()
        binding.addSpecialtyTitle.text = if (editing) "Sửa chuyên khoa" else "Thêm chuyên khoa"
        binding.addSpecialtySubtitle.text = if (editing) {
            "Cập nhật thông tin hiển thị cho bệnh nhân và bác sĩ"
        } else {
            "Tên và biểu tượng hiển thị cho bệnh nhân"
        }
        binding.addSpecialtySave.text = if (editing) "Lưu thay đổi" else "Lưu chuyên khoa"

        if (savedInstanceState == null) {
            binding.addSpecialtyName.setText(intent.getStringExtra(EXTRA_SPECIALTY_NAME).orEmpty())
            binding.addSpecialtyDescription.setText(intent.getStringExtra(EXTRA_SPECIALTY_DESCRIPTION).orEmpty())
        }
        renderIconChoice()

        binding.addSpecialtyBack.setOnClickListener { finish() }
        binding.addSpecialtyCancel.setOnClickListener { finish() }
        binding.addSpecialtyIconChoice.setOnClickListener { chooseIcon() }
        binding.addSpecialtySave.setOnClickListener { saveSpecialty() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_SPECIALTY_ID, editingId)
        outState.putString(EXTRA_SPECIALTY_ICON, selectedIconKey)
        outState.putBoolean(EXTRA_SPECIALTY_ACTIVE, editingActive)
        super.onSaveInstanceState(outState)
    }

    private fun renderIconChoice() {
        val option = SpecialtyVisuals.forKey(selectedIconKey)
        binding.addSpecialtyIconPreview.setImageResource(option.drawable)
        binding.addSpecialtyIconLabel.text = option.label
    }

    private fun chooseIcon() {
        val sheet = SheetOptionPickerBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        sheet.optionPickerTitle.text = "Chọn biểu tượng"
        SpecialtyVisuals.options.forEach { option ->
            val row = ItemSpecialtyIconOptionBinding.inflate(LayoutInflater.from(this), sheet.optionPickerList, false)
            row.specialtyIconOptionImage.setImageResource(option.drawable)
            row.specialtyIconOptionLabel.text = option.label
            row.specialtyIconOptionCheck.visibility =
                if (option.key == selectedIconKey) View.VISIBLE else View.GONE
            row.root.setOnClickListener {
                selectedIconKey = option.key
                renderIconChoice()
                dialog.dismiss()
            }
            sheet.optionPickerList.addView(row.root)
        }
        dialog.show()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }

    private fun saveSpecialty() {
        val name = binding.addSpecialtyName.text?.toString().orEmpty().trim()
        val description = binding.addSpecialtyDescription.text?.toString().orEmpty().trim()
        binding.addSpecialtyNameLayout.error =
            if (name.isEmpty()) getString(R.string.manager_required_field) else null
        if (binding.addSpecialtyNameLayout.error != null) return

        binding.addSpecialtySave.isEnabled = false
        val repository = DoctorManagementRepository(this)
        val callback: (Result<Unit>) -> Unit = { result ->
            binding.addSpecialtySave.isEnabled = true
            result.onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure {
                Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
            }
        }
        val id = editingId
        if (id.isNullOrBlank()) {
            repository.addSpecialty(name, description, selectedIconKey, callback)
        } else {
            repository.updateSpecialty(
                Specialty(id = id, name = name, description = description, isActive = editingActive, iconKey = selectedIconKey),
                name,
                description,
                selectedIconKey,
                callback,
            )
        }
    }
}
