package com.example.medicare.doctor.schedule

import android.app.DatePickerDialog
import android.graphics.Color
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.schedule.DoctorLeaveRequestRepository
import com.example.medicare.databinding.DialogDoctorLeaveRequestBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Shared request form for the doctor schedule tab and standalone screen. */
object DoctorLeaveRequestSheet {
    private val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun show(
        activity: AppCompatActivity,
        initialDate: LocalDate,
        repository: DoctorLeaveRequestRepository,
        onSaved: () -> Unit,
    ) {
        val form = DialogDoctorLeaveRequestBinding.inflate(activity.layoutInflater)
        val dialog = BottomSheetDialog(activity)
        var selectedDate = initialDate
        form.doctorLeaveDate.text = selectedDate.format(displayDate)
        form.doctorLeaveDate.setOnClickListener {
            DatePickerDialog(activity, { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                form.doctorLeaveDate.text = selectedDate.format(displayDate)
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }
        form.doctorLeaveType.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Xin nghỉ", "Đổi ca"),
        )
        form.doctorLeaveClose.setOnClickListener { dialog.dismiss() }
        form.doctorLeaveSubmit.setOnClickListener {
            val reason = form.doctorLeaveReason.text.toString().trim()
            if (reason.isBlank()) {
                form.doctorLeaveReason.error = activity.getString(R.string.doctor_leave_invalid)
                return@setOnClickListener
            }
            form.doctorLeaveSubmit.isEnabled = false
            val type = form.doctorLeaveType.selectedItem.toString()
            repository.submit(selectedDate.toString(), "$type: $reason") { result ->
                result.onSuccess {
                    dialog.dismiss()
                    onSaved()
                    Toast.makeText(activity, R.string.doctor_leave_submitted, Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    form.doctorLeaveSubmit.isEnabled = true
                    Toast.makeText(
                        activity,
                        error.message ?: activity.getString(R.string.manager_save_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
        dialog.setContentView(form.root)
        dialog.show()
        dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}
