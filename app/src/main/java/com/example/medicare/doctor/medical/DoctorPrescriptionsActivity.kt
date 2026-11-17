package com.example.medicare.doctor.medical

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.databinding.ActivityDoctorPrescriptionsBinding

class DoctorPrescriptionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorPrescriptionsBinding
    private lateinit var screen: DoctorPrescriptionScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorPrescriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screen = DoctorPrescriptionScreen(this, binding) { !isFinishing && !isDestroyed }
        binding.doctorPrescriptionsBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        screen.load()
    }
}
