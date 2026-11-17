package com.example.medicare.doctor.medical

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.medicare.databinding.ActivityDoctorPrescriptionsBinding

class DoctorPrescriptionsFragment : Fragment() {
    private var _binding: ActivityDoctorPrescriptionsBinding? = null
    private var screen: DoctorPrescriptionScreen? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val binding = ActivityDoctorPrescriptionsBinding.inflate(inflater, container, false)
        _binding = binding
        binding.doctorPrescriptionsHeader.visibility = View.GONE
        screen = DoctorPrescriptionScreen(requireActivity() as AppCompatActivity, binding) { isAdded && _binding != null }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        screen?.load()
    }

    override fun onDestroyView() {
        screen = null
        _binding = null
        super.onDestroyView()
    }
}
