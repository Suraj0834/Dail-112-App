package com.dial112.ui.sos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentActiveSosDetailBinding
import com.dial112.domain.model.Resource
import com.dial112.domain.model.SosLog
import com.dial112.domain.repository.ProfileRepository
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ActiveSosDetailFragment : Fragment() {

    private var _binding: FragmentActiveSosDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SosHistoryViewModel by viewModels()
    private val sosId: String by lazy { arguments?.getString("sosId") ?: "" }

    @Inject lateinit var profileRepository: ProfileRepository

    private var currentSos: SosLog? = null
    private var currentUserId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActiveSosDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Get current officer's ID for assignment
        runBlocking {
            val result = profileRepository.getProfile()
            if (result is Resource.Success) currentUserId = result.data.id
        }

        setupClickListeners()
        observeViewModel()
        // Load SOS details – fetch active list then find by ID
        viewModel.loadActiveSos()
        viewModel.loadHistory()
    }

    private fun setupClickListeners() {
        binding.btnAssignToMe.setOnClickListener {
            val sos = currentSos ?: return@setOnClickListener
            val officerId = currentUserId ?: run {
                showSnackbar("Could not determine officer ID")
                return@setOnClickListener
            }
            viewModel.assignOfficer(sos.id, officerId)
        }

        binding.btnNavigate.setOnClickListener {
            val sos = currentSos ?: return@setOnClickListener
            val geoUri = Uri.parse("google.navigation:q=${sos.latitude},${sos.longitude}&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${sos.latitude},${sos.longitude}")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }
    }

    private fun observeViewModel() {
        // Try active list first
        viewModel.activeSos.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val sos = resource.data.find { it.id == sosId }
                if (sos != null) bindSos(sos)
            }
        }

        // Fall back to history list
        viewModel.sosHistory.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success && currentSos == null) {
                val sos = resource.data.find { it.id == sosId }
                if (sos != null) bindSos(sos)
            }
        }

        viewModel.assignResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> binding.loadingOverlay.isVisible = true
                is Resource.Success -> {
                    binding.loadingOverlay.isVisible = false
                    showSnackbar(resource.data)
                    binding.btnAssignToMe.isEnabled = false
                    binding.btnAssignToMe.text = "Assigned"
                }
                is Resource.Error -> {
                    binding.loadingOverlay.isVisible = false
                    showSnackbar(resource.message)
                }
            }
        }
    }

    private fun bindSos(sos: SosLog) {
        currentSos = sos
        binding.tvSosType.text = sos.type.replace("_", " ").replaceFirstChar { it.uppercase() }

        // Status chip
        val (statusColor, statusLabel) = when (sos.status.uppercase()) {
            "ACTIVE" -> Pair(R.color.primary_red, "ACTIVE")
            "RESPONDED" -> Pair(R.color.accent_blue, "RESPONDED")
            "RESOLVED" -> Pair(R.color.accent_green, "RESOLVED")
            else -> Pair(R.color.accent_teal, sos.status.uppercase())
        }
        binding.chipStatus.text = statusLabel
        binding.chipStatus.setChipBackgroundColorResource(statusColor)

        // Time elapsed
        binding.tvTimeElapsed.text = getTimeElapsed(sos.createdAt)

        // Caller
        sos.triggeredBy?.let {
            binding.tvCallerName.text = it.name
            binding.tvCallerPhone.text = it.phone
            binding.btnCallCitizen.setOnClickListener { _ ->
                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${it.phone}") }
                startActivity(intent)
            }
        }

        // Location
        binding.tvLocation.text = sos.address.ifEmpty { "Location not available" }
        binding.tvCoordinates.text = "${sos.latitude}, ${sos.longitude}"

        // Responding officer
        sos.respondingOfficer?.let {
            binding.cardRespondingOfficer.isVisible = true
            binding.tvOfficerName.text = it.name
            binding.btnAssignToMe.isEnabled = false
            binding.btnAssignToMe.text = "Already Assigned"
        }
    }

    private fun getTimeElapsed(iso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(iso) ?: return iso
            val diffMs = System.currentTimeMillis() - date.time
            val mins = (diffMs / 60000).toInt()
            when {
                mins < 1 -> "Just now"
                mins < 60 -> "$mins min ago"
                mins < 1440 -> "${mins / 60} hr ago"
                else -> "${mins / 1440} day(s) ago"
            }
        } catch (_: Exception) { iso }
    }

    private fun showSnackbar(msg: String) = Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
