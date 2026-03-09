package com.dial112.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentProfileBinding
import com.dial112.domain.model.Resource
import com.dial112.domain.model.UserRole
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var isEditMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        setupClickListeners()
        observeViewModel()
        viewModel.loadProfile()
    }

    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener { toggleEditMode() }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim()
            val phone = binding.etPhone.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                binding.tilName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            viewModel.updateProfile(name, phone)
        }

        binding.btnSosHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_sosHistoryFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        binding.etName.isEnabled = isEditMode
        binding.etPhone.isEnabled = isEditMode
        binding.btnSave.isVisible = isEditMode
        binding.btnEdit.setImageResource(
            if (isEditMode) R.drawable.ic_close else R.drawable.ic_edit
        )
    }

    private fun observeViewModel() {
        viewModel.profile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.loadingOverlay.isVisible = true
                is Resource.Success -> {
                    binding.loadingOverlay.isVisible = false
                    val user = resource.data
                    binding.etName.setText(user.name)
                    binding.etPhone.setText(user.phone)
                    binding.tvEmail.text = user.email
                    binding.tvAvatarInitials.text = user.name.take(2).uppercase()
                    binding.chipRole.text = user.role.value.uppercase()

                    if (user.role == UserRole.POLICE) {
                        binding.cardPoliceInfo.isVisible = true
                        binding.chipRole.setChipBackgroundColorResource(com.dial112.R.color.accent_orange)
                        binding.tvBadgeId.text = user.badgeId ?: "N/A"
                        binding.tvStation.text = user.station ?: "Not assigned"
                        binding.btnSosHistory.isVisible = false
                    }
                }
                is Resource.Error -> {
                    binding.loadingOverlay.isVisible = false
                    showSnackbar(resource.message)
                }
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.loadingOverlay.isVisible = true
                is Resource.Success -> {
                    binding.loadingOverlay.isVisible = false
                    toggleEditMode()
                    showSnackbar("Profile updated successfully")
                }
                is Resource.Error -> {
                    binding.loadingOverlay.isVisible = false
                    showSnackbar(resource.message)
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
