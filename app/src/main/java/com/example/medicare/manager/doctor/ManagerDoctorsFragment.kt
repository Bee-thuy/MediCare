package com.example.medicare.manager.doctor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.DialogInterface
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.example.medicare.R
import com.example.medicare.databinding.FragmentManagerDoctorsBinding
import com.example.medicare.databinding.ItemManagedDoctorBinding
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.manager.doctor.AddDoctorActivity
import com.example.medicare.model.Doctor
import com.example.medicare.model.Specialty

class ManagerDoctorsFragment : Fragment() {
    private var _binding: FragmentManagerDoctorsBinding? = null
    private val binding get() = _binding!!
    private var doctors: List<Doctor> = emptyList()
    private var selectedSpecialty = ""
    private var selectedStatus = ""
    private val addDoctor = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && _binding != null) renderDoctors()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerDoctorsAdd.setOnClickListener { openAddDoctor() }
        binding.managerDoctorsBottomAdd.setOnClickListener { openAddDoctor() }
        binding.managerDoctorsFiltersToggle.setOnClickListener {
            val expanded = binding.managerDoctorsFiltersPanel.visibility != View.VISIBLE
            binding.managerDoctorsFiltersPanel.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.managerDoctorsFiltersToggle.text = if (expanded) "Ẩn bộ lọc  ▴" else "Lọc chuyên khoa, trạng thái  ▾"
        }
        binding.managerDoctorsSearch.doAfterTextChanged { query -> showDoctors(query?.toString().orEmpty()) }
        binding.managerDoctorsSpecialtyFilter.setOnItemClickListener { _, _, position, _ ->
            selectedSpecialty = if (position == 0) "" else binding.managerDoctorsSpecialtyFilter.adapter.getItem(position).toString()
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
        binding.managerDoctorsStatusFilter.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = if (position == 0) "" else binding.managerDoctorsStatusFilter.adapter.getItem(position).toString()
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
        loadSpecialties()
        renderDoctors()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderDoctors()
    }

    private fun openAddDoctor() {
        addDoctor.launch(Intent(requireContext(), AddDoctorActivity::class.java))
    }

    private fun loadSpecialties() {
        DoctorManagementRepository(requireContext()).getSpecialties { result ->
            if (_binding == null) return@getSpecialties
            val labels = listOf(getString(R.string.manager_all_specialties)) +
                result.getOrDefault(emptyList<Specialty>()).map { it.name }
            binding.managerDoctorsSpecialtyFilter.setAdapter(
                android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels),
            )
            binding.managerDoctorsStatusFilter.setAdapter(
                android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    listOf(getString(R.string.manager_all_statuses), getString(R.string.manager_active), getString(R.string.manager_inactive)),
                ),
            )
        }
    }

    private fun renderDoctors() {
        DoctorManagementRepository(requireContext()).getDoctors { result ->
            if (_binding == null) return@getDoctors
            doctors = result.getOrDefault(emptyList())
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
    }

    private fun showDoctors(query: String) {
        if (_binding == null) return
        val displayedDoctors = doctors.filter { doctor ->
            (doctor.fullName.contains(query, ignoreCase = true) ||
                doctor.specialty.contains(query, ignoreCase = true)) &&
                (selectedSpecialty.isEmpty() || doctor.specialty == selectedSpecialty) &&
                (selectedStatus.isEmpty() ||
                    (selectedStatus == getString(R.string.manager_active) && doctor.isActive) ||
                    (selectedStatus == getString(R.string.manager_inactive) && !doctor.isActive))
        }
        binding.managerDoctorsList.removeAllViews()
        binding.managerDoctorsEmpty.visibility = if (displayedDoctors.isEmpty()) View.VISIBLE else View.GONE
        displayedDoctors.forEach { doctor ->
                val itemBinding = ItemManagedDoctorBinding.inflate(layoutInflater, binding.managerDoctorsList, false)
                itemBinding.managedDoctorName.text = doctor.fullName
                itemBinding.managedDoctorSpecialty.text = "BS. ${doctor.specialty}"
                itemBinding.managedDoctorStatus.apply {
                    text = getString(if (doctor.isActive) R.string.manager_active else R.string.manager_inactive)
                    setTextColor(ContextCompat.getColor(requireContext(), if (doctor.isActive) R.color.manager_green else R.color.manager_red))
                    setBackgroundResource(if (doctor.isActive) R.drawable.manager_status_active_background else R.drawable.manager_status_inactive_background)
                }
                itemBinding.root.setOnClickListener { showDoctorDetail(doctor) }
                binding.managerDoctorsList.addView(itemBinding.root)
        }
    }

    private fun showDoctorDetail(doctor: Doctor) {
        val details = listOf(
            "Chuyên khoa: ${doctor.specialty}",
            "Điện thoại: ${doctor.phone.ifBlank { "Chưa cập nhật" }}",
            "Email: ${doctor.email.ifBlank { "Chưa cập nhật" }}",
            "Kinh nghiệm: ${doctor.experienceYears.takeIf { it.isNotBlank() }?.let { "$it năm" } ?: "Chưa cập nhật"}",
            "Trạng thái: ${if (doctor.isActive) "Hoạt động" else "Đã khóa"}",
        ).joinToString("\n")
        AlertDialog.Builder(requireContext())
            .setTitle("BS. ${doctor.fullName}")
            .setMessage(details)
            .setNegativeButton("Đóng", null)
            .setNeutralButton(if (doctor.isActive) "Khóa" else "Mở khóa") { _, _ -> confirmStatusChange(doctor) }
            .setPositiveButton("Sửa thông tin") { _, _ -> editDoctor(doctor) }
            .show()
    }

    private fun editDoctor(doctor: Doctor) {
        DoctorManagementRepository(requireContext()).getSpecialties { result ->
            if (!isAdded) return@getSpecialties
            val specialties = result.getOrDefault(emptyList()).filter { it.isActive || it.id == doctor.specialtyId }
            if (specialties.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa có chuyên khoa để chỉnh sửa.", Toast.LENGTH_LONG).show()
                return@getSpecialties
            }
            val form = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(8), dp(20), 0)
            }
            val name = editField("Họ tên bác sĩ", doctor.fullName)
            val phone = editField("Số điện thoại", doctor.phone).apply { inputType = InputType.TYPE_CLASS_PHONE }
            val experience = editField("Số năm kinh nghiệm", doctor.experienceYears).apply { inputType = InputType.TYPE_CLASS_NUMBER }
            val specialty = AutoCompleteTextView(requireContext()).apply {
                hint = "Chuyên khoa"
                inputType = InputType.TYPE_NULL
                setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, specialties.map { it.name }))
                setText(doctor.specialty, false)
                setOnClickListener { showDropDown() }
            }
            listOf(name, specialty, phone, experience).forEach { field ->
                form.addView(field, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(8) })
            }
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Sửa thông tin bác sĩ")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create()
            dialog.setOnShowListener {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val selected = specialties.firstOrNull { it.name == specialty.text.toString().trim() }
                    val fullName = name.text.toString().trim()
                    val number = phone.text.toString().trim()
                    if (fullName.isBlank() || number.isBlank() || selected == null) {
                        Toast.makeText(requireContext(), "Nhập họ tên, số điện thoại và chọn chuyên khoa.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
                    DoctorManagementRepository(requireContext()).updateDoctor(
                        doctor.id, fullName, selected, number, experience.text.toString().trim()
                    ) { update ->
                        if (!isAdded) return@updateDoctor
                        update.onSuccess { dialog.dismiss(); renderDoctors() }
                            .onFailure {
                                dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
                                Toast.makeText(requireContext(), it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
            dialog.show()
        }
    }

    private fun editField(hint: String, value: String) = EditText(requireContext()).apply {
        this.hint = hint
        setText(value)
        setSingleLine(true)
        textSize = 13f
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun confirmStatusChange(doctor: Doctor) {
        val activate = !doctor.isActive
        AlertDialog.Builder(requireContext())
            .setTitle(if (activate) R.string.manager_enable_doctor_title else R.string.manager_disable_doctor_title)
            .setMessage(getString(if (activate) R.string.manager_enable_doctor_message else R.string.manager_disable_doctor_message, doctor.fullName))
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton(if (activate) R.string.manager_unlock_patient else R.string.manager_lock_patient) { _, _ ->
                DoctorManagementRepository(requireContext()).setDoctorActive(doctor.id, activate) { result ->
                    result.onSuccess { renderDoctors() }
                        .onFailure { Toast.makeText(requireContext(), it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show() }
                }
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
