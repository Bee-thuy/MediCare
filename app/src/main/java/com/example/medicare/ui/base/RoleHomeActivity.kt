package com.example.medicare.ui.base

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.ActivityRoleHomeBinding
import com.example.medicare.ui.auth.LoginActivity

abstract class RoleHomeActivity : FullscreenActivity() {
    private lateinit var binding: ActivityRoleHomeBinding

    protected fun showHome(@StringRes title: Int, @StringRes description: Int) {
        binding = ActivityRoleHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.roleHomeTitle.setText(title)
        binding.roleHomeDescription.setText(description)
        binding.roleHomeLogout.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
