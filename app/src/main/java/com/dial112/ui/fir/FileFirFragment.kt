package com.dial112.ui.fir

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentFileFirBinding
import com.dial112.domain.model.CaseCategory
import com.dial112.domain.model.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FileFirFragment : Fragment() {

    private var _binding: FragmentFileFirBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FileFirViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var currentLat = 0.0
    private var currentLng = 0.0

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivPreview.setImageURI(it)
            binding.ivPreview.isVisible = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileFirBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupCategoryChips()
        setupClickListeners()
        observeViewModel()
        getCurrentLocation()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupCategoryChips() {
        // Default selection
        binding.chipTheft.isChecked = true
    }

    private fun getSelectedCategory(): String {
        return when (binding.chipGroupCategory.checkedChipId) {
            R.id.chipTheft -> CaseCategory.THEFT.name
            R.id.chipViolence -> CaseCategory.VIOLENCE.name
            R.id.chipFraud -> CaseCategory.FRAUD.name
            R.id.chipCybercrime -> CaseCategory.CYBERCRIME.name
            R.id.chipHarassment -> CaseCategory.HARASSMENT.name
            R.id.chipOther -> CaseCategory.OTHER.name
            else -> CaseCategory.OTHER.name
        }
    }

    private fun setupClickListeners() {
        binding.cardImage.setOnClickListener { pickImage.launch("image/*") }

        binding.btnAutoClassify.setOnClickListener {
            val desc = binding.etDescription.text?.toString()?.trim() ?: ""
            if (desc.isBlank()) {
                Snackbar.make(binding.root, "Enter a description first", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.autoClassify(desc)
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim() ?: ""
            val desc = binding.etDescription.text?.toString()?.trim() ?: ""
            val category = getSelectedCategory()

            if (title.isBlank() || desc.isBlank()) {
                Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagePath = selectedImageUri?.let { uri ->
                requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                    val file = java.io.File(requireContext().cacheDir, "fir_image.jpg")
                    file.outputStream().use { stream.copyTo(it) }
                    file.absolutePath
                }
            }

            viewModel.fileCase(title, desc, category, currentLat, currentLng, imagePath)
        }
    }

    private fun observeViewModel() {
        viewModel.categoryState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnAutoClassify.isEnabled = false
                    binding.btnAutoClassify.text = "Detecting…"
                }
                is Resource.Success -> {
                    binding.btnAutoClassify.isEnabled = true
                    binding.btnAutoClassify.text = "Detect Category"
                    val detectedCategory = resource.data.category.uppercase()
                    val chipId = when (detectedCategory) {
                        "THEFT" -> R.id.chipTheft
                        "VIOLENCE" -> R.id.chipViolence
                        "FRAUD" -> R.id.chipFraud
                        "CYBERCRIME" -> R.id.chipCybercrime
                        "HARASSMENT" -> R.id.chipHarassment
                        else -> R.id.chipOther
                    }
                    binding.chipGroupCategory.check(chipId)
                    val pct = (resource.data.confidence * 100).toInt()
                    Snackbar.make(binding.root, "Detected: $detectedCategory ($pct% confidence)", Snackbar.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    binding.btnAutoClassify.isEnabled = true
                    binding.btnAutoClassify.text = "Detect Category"
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fileCaseState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnSubmit.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnSubmit.isEnabled = true
                    Snackbar.make(binding.root, "FIR filed successfully!", Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnSubmit.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation.addOnSuccessListener { loc ->
                loc?.let { currentLat = it.latitude; currentLng = it.longitude }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
