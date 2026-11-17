package com.example.medicare.manager.doctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.databinding.ActivitySpecialtyBinding
import com.example.medicare.databinding.ItemManagedSpecialtyBinding
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.model.Doctor
import com.example.medicare.model.Specialty
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class SpecialtyActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpecialtyBinding
    private var specialties: List<Specialty> = emptyList()
    private var doctors: List<Doctor> = emptyList()
    private val addSpecialty = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpecialtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.manageSpecialtiesBack.setOnClickListener { finish() }
        binding.manageSpecialtiesAdd.setOnClickListener {
            addSpecialty.launch(Intent(this, AddSpecialtyActivity::class.java))
        }
        binding.manageSpecialtiesBottomAdd.setOnClickListener {
            addSpecialty.launch(Intent(this, AddSpecialtyActivity::class.java))
        }
        binding.manageSpecialtiesSearch.doAfterTextChanged { showSpecialties(it?.toString().orEmpty()) }
        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadData()
    }

    private fun loadData() {
        val repository = DoctorManagementRepository(this)
        repository.getSpecialties { specialtiesResult ->
            specialties = specialtiesResult.getOrDefault(emptyList())
            repository.getDoctors { doctorsResult ->
                doctors = doctorsResult.getOrDefault(emptyList())
                showSpecialties(binding.manageSpecialtiesSearch.text?.toString().orEmpty())
            }
        }
    }

    private fun showSpecialties(query: String) {
        val filtered = specialties.filter { it.name.contains(query, ignoreCase = true) }
        binding.manageSpecialtiesList.removeAllViews()
        binding.manageSpecialtiesEmpty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        filtered.forEach { specialty ->
            val itemBinding = ItemManagedSpecialtyBinding.inflate(layoutInflater, binding.manageSpecialtiesList, false)
            itemBinding.managedSpecialtyName.text = specialty.name
            val totalDoctors = doctors.count { it.specialtyId == specialty.id }
            itemBinding.managedSpecialtyCount.text = resources.getQuantityString(
                R.plurals.manager_doctor_count,
                totalDoctors,
                totalDoctors,
            ) + if (specialty.isActive) " · Đang hiển thị" else " · Đã ẩn"
            itemBinding.root.setOnClickListener { showActions(specialty) }
            binding.manageSpecialtiesList.addView(itemBinding.root)
        }
    }

    private fun showActions(specialty: Specialty) {
        val actions = arrayOf("Chỉnh sửa", if (specialty.isActive) "Tạm ngưng" else "Kích hoạt", "Xóa có kiểm soát")
        val count = doctors.count { it.specialtyId == specialty.id }
        AlertDialog.Builder(this).setTitle("${specialty.name} · $count bác sĩ")
            .setItems(actions) { _, which ->
            when (which) {
                0 -> editSpecialty(specialty)
                1 -> DoctorManagementRepository(this).setSpecialtyActive(specialty.id, !specialty.isActive, ::handleResult)
                2 -> AlertDialog.Builder(this).setTitle("Xóa chuyên khoa?")
                    .setMessage("Chỉ xóa được khi chưa có bác sĩ và lịch làm việc sử dụng chuyên khoa này.")
                    .setNegativeButton(R.string.manager_cancel, null)
                    .setPositiveButton("Xóa") { _, _ -> DoctorManagementRepository(this).deleteSpecialtyIfUnused(specialty, ::handleResult) }
                    .show()
            }
        }.show()
    }

    private fun editSpecialty(specialty: Specialty) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(52, 16, 52, 0) }
        val name = EditText(this).apply { hint = "Tên chuyên khoa"; setText(specialty.name) }
        val description = EditText(this).apply { hint = "Mô tả"; setText(specialty.description); minLines = 3 }
        box.addView(name); box.addView(description)
        AlertDialog.Builder(this).setTitle("Chỉnh sửa chuyên khoa").setView(box)
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton("Lưu") { _, _ ->
                val value = name.text.toString().trim()
                if (value.isBlank()) Toast.makeText(this, "Tên chuyên khoa không được để trống.", Toast.LENGTH_SHORT).show()
                else DoctorManagementRepository(this).updateSpecialty(specialty, value, description.text.toString(), ::handleResult)
            }.show()
    }

    private fun handleResult(result: Result<Unit>) {
        result.onSuccess { loadData() }.onFailure { Toast.makeText(this, it.message ?: "Không thể lưu thay đổi.", Toast.LENGTH_LONG).show() }
    }
}
