package com.example.medicare.manager.docter

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.databinding.ActivitySpecialtyBinding
import com.example.medicare.databinding.ItemManagedSpecialtyBinding

class SpecialtyActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpecialtyBinding
    private var specialties: List<ManagedSpecialty> = emptyList()
    private var doctors: List<ManagedDoctor> = emptyList()
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
            )
            binding.manageSpecialtiesList.addView(itemBinding.root)
        }
    }
}
