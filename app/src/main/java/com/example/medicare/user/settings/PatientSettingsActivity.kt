package com.example.medicare.user.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.notification.NotificationRepository
import com.example.medicare.databinding.ActivityPatientSettingsBinding
import com.example.medicare.databinding.SheetPatientChangePasswordBinding
import com.example.medicare.databinding.SheetPatientEditProfileBinding
import com.example.medicare.model.AppNotification
import com.example.medicare.notification.NotificationsActivity
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.user.PatientScreenChrome
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientSettingsBinding
    private val notificationRepository = NotificationRepository()
    private var notificationListener: ListenerRegistration? = null
    private var patientName = ""
    private var patientPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PatientScreenChrome.bind(this, "Thông báo & cài đặt")
        binding.patientSettingsScroll.post { binding.patientSettingsScroll.scrollTo(0, 0) }
        binding.patientSettingsBack.setOnClickListener { finish() }
        binding.patientSettingsAccount.setOnClickListener { openProfile() }
        binding.patientSettingsProfile.setOnClickListener { openProfile() }
        binding.patientSettingsNotifications.setOnClickListener { openNotifications() }
        binding.patientSettingsPassword.setOnClickListener { changePassword() }
        binding.patientSettingsNotificationPrefs.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        binding.patientSettingsSignOut.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAccount()
    }

    private fun loadAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        binding.patientSettingsEmail.text = user.email.orEmpty()
        binding.patientSettingsCurrentAccount.text = user.email.orEmpty()
        binding.patientSettingsId.text = "Mã bệnh nhân: ${user.uid.take(8).uppercase()}"
        FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                patientName = document.getString("fullName").orEmpty()
                patientPhone = document.getString("phone").orEmpty()
                binding.patientSettingsName.text = patientName.ifBlank { user.email.orEmpty() }
            }
    }

    override fun onStart() {
        super.onStart()
        notificationListener = notificationRepository.observeMine { result ->
            if (isFinishing || isDestroyed) return@observeMine
            result.onSuccess { renderNotifications(it.take(4)) }
                .onFailure { renderNotifications(emptyList()) }
        }
    }

    private fun renderNotifications(notifications: List<AppNotification>) {
        binding.patientSettingsNotificationList.removeAllViews()
        binding.patientSettingsNotificationEmpty.visibility =
            if (notifications.isEmpty()) View.VISIBLE else View.GONE
        notifications.forEach { notification ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundResource(R.drawable.manager_card_background)
                layoutParams = LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(9) }
            }
            row.addView(TextView(this).apply {
                text = "🔔"
                textSize = 21f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.patient_avatar_background)
            }, LinearLayout.LayoutParams(dp(43), dp(43)))
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
            }
            details.addView(TextView(this).apply {
                text = notification.title.ifBlank { notification.body }
                textSize = 13f
                typeface = resources.getFont(R.font.inter_bold)
                setTextColor(getColor(R.color.manager_ink))
                maxLines = 2
            })
            details.addView(TextView(this).apply {
                text = if (notification.createdAt > 0)
                    SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi"))
                        .format(Date(notification.createdAt)) else ""
                textSize = 11f
                setTextColor(getColor(R.color.manager_muted))
            })
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.setOnClickListener { openNotifications() }
            binding.patientSettingsNotificationList.addView(row)
        }
    }

    private fun openProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val sheet = SheetPatientEditProfileBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        sheet.patientEditName.setText(patientName)
        sheet.patientEditPhone.setText(patientPhone)
        sheet.patientEditEmail.setText(user.email.orEmpty())
        sheet.patientEditClose.setOnClickListener { dialog.dismiss() }
        sheet.patientEditSave.setOnClickListener {
            val name = sheet.patientEditName.text.toString().trim()
            val phone = sheet.patientEditPhone.text.toString().trim()
            if (name.isBlank()) {
                sheet.patientEditName.error = getString(R.string.manager_required_field)
                return@setOnClickListener
            }
            sheet.patientEditSave.isEnabled = false
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .update(mapOf("fullName" to name, "phone" to phone,
                    "updatedAt" to FieldValue.serverTimestamp()))
                .addOnSuccessListener {
                    patientName = name
                    patientPhone = phone
                    binding.patientSettingsName.text = name
                    dialog.dismiss()
                    Toast.makeText(this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    sheet.patientEditSave.isEnabled = true
                    Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun changePassword() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val sheet = SheetPatientChangePasswordBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        sheet.patientPasswordClose.setOnClickListener { dialog.dismiss() }
        sheet.patientPasswordSave.setOnClickListener {
            val currentPassword = sheet.patientPasswordCurrent.text.toString()
            val newPassword = sheet.patientPasswordInput.text.toString()
            if (currentPassword.isBlank()) {
                sheet.patientPasswordCurrent.error = "Vui lòng nhập mật khẩu cũ"
                return@setOnClickListener
            }
            if (newPassword.length < 8) {
                sheet.patientPasswordInput.error = getString(R.string.auth_invalid_password)
                return@setOnClickListener
            }
            val email = user.email
            if (email.isNullOrBlank()) {
                Toast.makeText(this, "Tài khoản chưa có email để xác thực", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            sheet.patientPasswordSave.isEnabled = false
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword))
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            dialog.dismiss()
                            Toast.makeText(this, R.string.profile_password_changed, Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            sheet.patientPasswordSave.isEnabled = true
                            Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { error ->
                    sheet.patientPasswordSave.isEnabled = true
                    if (error is FirebaseAuthInvalidCredentialsException) {
                        sheet.patientPasswordCurrent.error = "Mật khẩu cũ không đúng"
                    } else {
                        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                    }
                }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    private fun openNotifications() = startActivity(Intent(this, NotificationsActivity::class.java))
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onStop() {
        notificationListener?.remove()
        notificationListener = null
        super.onStop()
    }
}
