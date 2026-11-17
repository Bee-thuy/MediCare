package com.example.medicare.user.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.ActivityPatientSettingsBinding
import com.example.medicare.notification.NotificationsActivity
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.patientSettingsBack.setOnClickListener { finish() }
        binding.patientSettingsAccount.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.patientSettingsProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.patientSettingsNotifications.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        binding.patientSettingsPassword.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).putExtra(ProfileActivity.EXTRA_OPEN_PASSWORD, true))
        }
        binding.patientSettingsNotificationPrefs.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        binding.patientSettingsSignOut.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.patientSettingsAccount.text = "👤   ${currentUser?.email.orEmpty()}   ›"
        currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (!isFinishing && !isDestroyed) {
                        val name = document.getString("fullName").orEmpty().ifBlank { currentUser.email.orEmpty() }
                        binding.patientSettingsAccount.text = "👤   $name   ›"
                    }
                }
        }
    }
}
