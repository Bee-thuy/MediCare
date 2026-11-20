package com.example.medicare.doctor.profile

import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.databinding.SheetDoctorEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/** The compact profile editor shared by the doctor tab and standalone profile page. */
object DoctorProfileEditor {
    fun show(activity: AppCompatActivity, onSaved: (name: String, phone: String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val sheet = SheetDoctorEditProfileBinding.inflate(activity.layoutInflater)
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(sheet.root)
        sheet.doctorEditSave.isEnabled = false
        sheet.doctorEditClose.setOnClickListener { dialog.dismiss() }
        sheet.doctorEditSave.setOnClickListener {
            val name = sheet.doctorEditName.text.toString().trim()
            val phone = sheet.doctorEditPhone.text.toString().trim()
            if (name.isBlank()) {
                sheet.doctorEditName.error = activity.getString(R.string.manager_required_field)
                return@setOnClickListener
            }
            sheet.doctorEditSave.isEnabled = false
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(mapOf("fullName" to name, "phone" to phone,
                    "updatedAt" to FieldValue.serverTimestamp()))
                .addOnSuccessListener {
                    onSaved(name, phone)
                    dialog.dismiss()
                    Toast.makeText(activity, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    sheet.doctorEditSave.isEnabled = true
                    Toast.makeText(activity,
                        error.message ?: activity.getString(R.string.manager_save_failed),
                        Toast.LENGTH_LONG).show()
                }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { profile ->
                if (!dialog.isShowing) return@addOnSuccessListener
                sheet.doctorEditName.setText(profile.getString("fullName").orEmpty())
                sheet.doctorEditPhone.setText(profile.getString("phone").orEmpty())
                sheet.doctorEditSave.isEnabled = true
            }
            .addOnFailureListener { error ->
                if (!dialog.isShowing) return@addOnFailureListener
                Toast.makeText(activity,
                    error.message ?: "Không tải được hồ sơ bác sĩ.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
    }
}
