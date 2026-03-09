package com.dial112.ui.vehicle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.databinding.FragmentVehicleBinding
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VehicleFragment : Fragment() {

    private var _binding: FragmentVehicleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VehicleViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val plate = binding.etPlateNumber.text?.toString()?.trim() ?: ""
            if (plate.isBlank()) {
                Snackbar.make(binding.root, "Enter a plate number", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.searchVehicle(plate)
        }
    }

    private fun observeViewModel() {
        viewModel.vehicleState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.cardResult.isVisible = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.cardResult.isVisible = true
                    val v = resource.data
                    binding.tvPlate.text = v.plateNumber
                    binding.tvOwner.text = v.ownerName
                    binding.tvType.text = v.vehicleType
                    binding.tvModel.text = v.model
                    binding.tvColor.text = v.color
                    binding.tvStolenStatus.text = if (v.isStolen) "STOLEN" else "Clean"
                    binding.tvStolenStatus.setTextColor(
                        resources.getColor(
                            if (v.isStolen) android.R.color.holo_red_dark else android.R.color.holo_green_dark,
                            null
                        )
                    )
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.cardResult.isVisible = false
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
