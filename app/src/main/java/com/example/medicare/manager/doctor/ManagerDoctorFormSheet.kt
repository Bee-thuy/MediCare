package com.example.medicare.manager.doctor

import android.app.Activity
import android.graphics.Color
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.example.medicare.R
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Admin doctor creation form styled like the HTML detail modal. */
object ManagerDoctorFormSheet {
    fun show(activity: Activity, onCreated: () -> Unit) {
        val repository = DoctorManagementRepository(activity)
        repository.getSpecialties { result ->
            if (activity.isFinishing) return@getSpecialties
            val specialties = result.getOrDefault(emptyList()).filter { it.isActive }
            if (specialties.isEmpty()) {
                Toast.makeText(activity, result.exceptionOrNull()?.message ?: "Hãy tạo chuyên khoa trước khi thêm bác sĩ.", Toast.LENGTH_LONG).show()
                return@getSpecialties
            }
            val sheet = BottomSheetDialog(activity)
            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 20))
                setBackgroundResource(R.drawable.patient_record_sheet_background)
            }
            val heading = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
            heading.addView(TextView(activity).apply {
                text = "Thêm bác sĩ"; textSize = 17f; setTextColor(activity.getColor(R.color.manager_ink))
                typeface = resources.getFont(R.font.inter_bold)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            heading.addView(TextView(activity).apply {
                text = "×"; textSize = 22f; gravity = Gravity.CENTER
                setTextColor(activity.getColor(R.color.manager_ink))
                setBackgroundResource(R.drawable.manager_avatar_placeholder)
                setOnClickListener { sheet.dismiss() }
            }, LinearLayout.LayoutParams(dp(activity, 34), dp(activity, 34)))
            root.addView(heading)
            val scroll = ScrollView(activity).apply { isVerticalScrollBarEnabled = false }
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(activity, 10) })
            val fields = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            scroll.addView(fields)
            val name = field(activity, fields, "Họ tên bác sĩ", "Nguyễn Văn A")
            val specialty = AutoCompleteTextView(activity).apply {
                inputType = InputType.TYPE_NULL
                threshold = 0
                setAdapter(ArrayAdapter(activity, android.R.layout.simple_list_item_1, specialties.map { it.name }))
                setOnClickListener { showDropDown() }
            }
            addField(activity, fields, "Chuyên khoa", specialty)
            val phone = field(activity, fields, "Số điện thoại", "09xxxxxxxx", InputType.TYPE_CLASS_PHONE)
            val experience = field(activity, fields, "Số năm kinh nghiệm", "5", InputType.TYPE_CLASS_NUMBER)
            fields.addView(TextView(activity).apply {
                text = "Thông tin đăng nhập"; textSize = 12f
                setTextColor(activity.getColor(R.color.manager_ink)); typeface = resources.getFont(R.font.inter_bold)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 14) })
            val email = field(activity, fields, "Email đăng nhập", "bacsi@example.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            val password = field(activity, fields, "Mật khẩu tạm (ít nhất 8 ký tự)", "Nhập mật khẩu", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            val qualification = field(activity, fields, "Bằng cấp (tùy chọn)", "Bác sĩ chuyên khoa")
            val biography = field(activity, fields, "Giới thiệu (tùy chọn)", "Kinh nghiệm và chuyên môn")
            val active = SwitchCompat(activity).apply {
                text = "Kích hoạt tài khoản ngay"; isChecked = true; textSize = 12f
                setTextColor(activity.getColor(R.color.manager_ink))
            }
            fields.addView(active, LinearLayout.LayoutParams(-1, dp(activity, 46)))
            val save = TextView(activity).apply {
                text = "Lưu thông tin"; gravity = Gravity.CENTER; textSize = 13f
                typeface = resources.getFont(R.font.inter_bold); setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.manager_button_background)
            }
            root.addView(save, LinearLayout.LayoutParams(-1, dp(activity, 46)).apply { topMargin = dp(activity, 10) })
            save.setOnClickListener {
                val selected = specialties.firstOrNull { it.name == specialty.text.toString().trim() }
                val fullName = name.text.toString().trim()
                val emailValue = email.text.toString().trim()
                val passwordValue = password.text.toString()
                when {
                    fullName.isBlank() -> fail(name, "Nhập họ tên bác sĩ.")
                    selected == null -> fail(specialty, "Chọn chuyên khoa.")
                    phone.text.isNullOrBlank() -> fail(phone, "Nhập số điện thoại.")
                    experience.text.toString().toIntOrNull() == null -> fail(experience, "Nhập số năm kinh nghiệm.")
                    !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> fail(email, "Email chưa hợp lệ.")
                    passwordValue.length < 8 -> fail(password, "Mật khẩu cần ít nhất 8 ký tự.")
                    else -> {
                        save.isEnabled = false
                        repository.createDoctorAccount(fullName, selected, emailValue, passwordValue,
                            phone.text.toString().trim(), experience.text.toString().trim(),
                            qualification.text.toString().trim(), biography.text.toString().trim(), active.isChecked) { creation ->
                            save.isEnabled = true
                            creation.onSuccess { sheet.dismiss(); onCreated() }
                                .onFailure { Toast.makeText(activity, it.message ?: "Không thêm được bác sĩ.", Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
            root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * .84f).toInt())
            sheet.setContentView(root)
            activity.window.decorView.post { if (!activity.isFinishing) sheet.show() }
        }
    }

    private fun field(activity: Activity, parent: LinearLayout, label: String, hint: String,
                      inputType: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(activity).apply {
        this.hint = hint; this.inputType = inputType; isSingleLine = true
        addField(activity, parent, label, this)
    }

    private fun addField(activity: Activity, parent: LinearLayout, label: String, input: EditText) {
        parent.addView(TextView(activity).apply {
            text = label; textSize = 11f; typeface = resources.getFont(R.font.inter_bold)
            setTextColor(activity.getColor(R.color.manager_ink))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(activity, 6); topMargin = dp(activity, 8) })
        input.apply {
            textSize = 12f
            setTextColor(activity.getColor(R.color.manager_ink))
            setHintTextColor(activity.getColor(R.color.manager_muted))
            setPadding(dp(activity, 12), 0, dp(activity, 12), 0)
            setBackgroundResource(R.drawable.manager_form_field_background)
        }
        parent.addView(input, LinearLayout.LayoutParams(-1, dp(activity, 44)))
    }

    private fun fail(input: EditText, message: String) { input.error = message; input.requestFocus() }
    private fun dp(activity: Activity, value: Int) = (value * activity.resources.displayMetrics.density + .5f).toInt()
}
