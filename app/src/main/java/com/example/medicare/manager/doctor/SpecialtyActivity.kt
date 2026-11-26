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
import com.example.medicare.manager.ManagerDetailSheets
import com.example.medicare.manager.ManagerAuxNavigation
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
        ManagerAuxNavigation.bind(this)

        binding.manageSpecialtiesBack.setOnClickListener { ManagerAuxNavigation.showCatalog(this) }
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
            itemBinding.managedSpecialtyIcon.setImageResource(SpecialtyVisuals.forSpecialty(specialty).drawable)
            val totalDoctors = doctors.count { it.specialtyId == specialty.id }
            itemBinding.managedSpecialtyCount.text = resources.getQuantityString(
                R.plurals.manager_doctor_count,
                totalDoctors,
                totalDoctors,
            )
            itemBinding.managedSpecialtyStatus.text = if (specialty.isActive) "Hiển thị" else "Đã ẩn"
            itemBinding.managedSpecialtyStatus.setBackgroundResource(
                if (specialty.isActive) R.drawable.manager_status_active_background
                else R.drawable.manager_status_inactive_background,
            )
            itemBinding.managedSpecialtyStatus.setTextColor(
                getColor(if (specialty.isActive) R.color.manager_green else R.color.manager_muted),
            )
            itemBinding.root.setOnClickListener { showActions(specialty) }
            itemBinding.root.setOnLongClickListener { confirmDelete(specialty); true }
            binding.manageSpecialtiesList.addView(itemBinding.root)
        }
    }

    private fun showActions(specialty: Specialty) {
        val count = doctors.count { it.specialtyId == specialty.id }
        ManagerDetailSheets.specialty(this, specialty, count,
            onEdit = { editSpecialty(specialty) },
            onToggle = { DoctorManagementRepository(this).setSpecialtyActive(specialty.id,
                !specialty.isActive, ::handleResult) })
    }

    private fun confirmDelete(specialty: Specialty) {
        AlertDialog.Builder(this).setTitle("Xóa chuyên khoa?")
            .setMessage("Chỉ xóa được khi chưa có bác sĩ và lịch làm việc sử dụng chuyên khoa này.")
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton("Xóa") { _, _ ->
                DoctorManagementRepository(this).deleteSpecialtyIfUnused(specialty, ::handleResult)
            }.show()
    }

    private fun editSpecialty(specialty: Specialty) {
        addSpecialty.launch(
            Intent(this, AddSpecialtyActivity::class.java)
                .putExtra(AddSpecialtyActivity.EXTRA_SPECIALTY_ID, specialty.id)
                .putExtra(AddSpecialtyActivity.EXTRA_SPECIALTY_NAME, specialty.name)
                .putExtra(AddSpecialtyActivity.EXTRA_SPECIALTY_DESCRIPTION, specialty.description)
                .putExtra(AddSpecialtyActivity.EXTRA_SPECIALTY_ICON, SpecialtyVisuals.forSpecialty(specialty).key)
                .putExtra(AddSpecialtyActivity.EXTRA_SPECIALTY_ACTIVE, specialty.isActive),
        )
    }

    private fun handleResult(result: Result<Unit>) {
        result.onSuccess { loadData() }.onFailure { Toast.makeText(this, it.message ?: "Không thể lưu thay đổi.", Toast.LENGTH_LONG).show() }
    }
}
