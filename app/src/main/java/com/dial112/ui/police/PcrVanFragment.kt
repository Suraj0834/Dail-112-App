package com.dial112.ui.police

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dial112.R
import com.dial112.databinding.FragmentPcrVanBinding
import com.dial112.domain.model.PcrVan
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PcrVanFragment : Fragment() {

    private var _binding: FragmentPcrVanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PcrVanViewModel by viewModels()
    private lateinit var adapter: PcrVanAdapter

    private val statusOptions = listOf("Available", "Busy", "Off-Duty", "Maintenance")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPcrVanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupStatusDropdown()
        setupMyVanUpdateButton()
        observeViewModel()

        binding.btnRefresh.setOnClickListener {
            viewModel.loadAllVans()
            viewModel.loadMyVan()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupRecyclerView() {
        adapter = PcrVanAdapter { van ->
            // Only the assigned officer can edit their own van; handled via My Van section
        }
        binding.rvAllVans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllVans.adapter = adapter
    }

    private fun setupStatusDropdown() {
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusOptions
        )
        binding.actvStatus.setAdapter(statusAdapter)
    }

    private fun setupMyVanUpdateButton() {
        binding.btnUpdateVan.setOnClickListener {
            val selectedStatus = binding.actvStatus.text.toString()
            val notes = binding.etNotes.text?.toString()?.trim()

            if (selectedStatus.isBlank()) {
                Snackbar.make(binding.root, "Please select a status", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val myVanResource = viewModel.myVan.value
            if (myVanResource is Resource.Success && myVanResource.data != null) {
                viewModel.updateVanStatus(myVanResource.data.id, selectedStatus, notes)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.myVan.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.cardMyVan.isVisible = false
                    binding.tvNoVanAssigned.isVisible = false
                }
                is Resource.Success -> {
                    val van = resource.data
                    if (van != null) {
                        bindMyVanCard(van)
                        binding.cardMyVan.isVisible = true
                        binding.tvNoVanAssigned.isVisible = false
                    } else {
                        binding.cardMyVan.isVisible = false
                        binding.tvNoVanAssigned.isVisible = true
                    }
                }
                is Resource.Error -> {
                    binding.cardMyVan.isVisible = false
                    binding.tvNoVanAssigned.isVisible = true
                }
            }
        }

        viewModel.allVans.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBarVans.isVisible = true
                    binding.rvAllVans.isVisible = false
                    binding.tvNoVans.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBarVans.isVisible = false
                    val vans = resource.data ?: emptyList()
                    adapter.submitList(vans)
                    binding.rvAllVans.isVisible = vans.isNotEmpty()
                    binding.tvNoVans.isVisible = vans.isEmpty()
                }
                is Resource.Error -> {
                    binding.progressBarVans.isVisible = false
                    binding.tvNoVans.isVisible = true
                    binding.rvAllVans.isVisible = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnUpdateVan.isEnabled = false
                    binding.progressBarUpdate.isVisible = true
                }
                is Resource.Success -> {
                    binding.btnUpdateVan.isEnabled = true
                    binding.progressBarUpdate.isVisible = false
                    Snackbar.make(binding.root, "Van updated successfully", Snackbar.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.btnUpdateVan.isEnabled = true
                    binding.progressBarUpdate.isVisible = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindMyVanCard(van: PcrVan) {
        binding.tvMyVanName.text = van.vehicleName.ifBlank { "PCR Van" }
        binding.tvMyVanPlate.text = van.plateNo

        binding.chipMyVanStatus.text = van.status
        val chipColor = when (van.status.lowercase()) {
            "available"             -> R.color.accent_green
            "busy"                  -> R.color.primary_red
            "off-duty", "off_duty"  -> R.color.text_hint
            "maintenance"           -> R.color.accent_orange
            else                    -> R.color.accent_blue
        }
        binding.chipMyVanStatus.setChipBackgroundColorResource(chipColor)

        // Pre-populate the edit fields with current values
        binding.actvStatus.setText(van.status, false)
        binding.etNotes.setText(van.notes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
