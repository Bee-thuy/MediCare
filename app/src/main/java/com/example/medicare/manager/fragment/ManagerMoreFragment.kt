package com.example.medicare.manager.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.FragmentManagerMoreBinding
import com.example.medicare.manager.docter.SpecialtyActivity
import com.example.medicare.manager.docter.WorkShiftActivity
import com.example.medicare.ui.auth.LoginActivity

class ManagerMoreFragment : Fragment() {
    private var _binding: FragmentManagerMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerMoreSpecialties.setOnClickListener {
            startActivity(Intent(requireContext(), SpecialtyActivity::class.java))
        }
        binding.managerMoreWorkShifts.setOnClickListener {
            startActivity(Intent(requireContext(), WorkShiftActivity::class.java))
        }
        binding.managerMoreSignOut.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
