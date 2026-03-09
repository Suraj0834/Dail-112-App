package com.dial112.ui.police

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dial112.R
import com.dial112.databinding.FragmentPoliceHomeBinding
import com.dial112.domain.model.Resource
import com.dial112.utils.SocketManager
import com.dial112.utils.SosAssignmentEvent
import com.dial112.utils.services.OfficerDutyService
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PoliceHomeFragment : Fragment() {

    private var _binding: FragmentPoliceHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PoliceHomeViewModel by viewModels()

    @Inject lateinit var socketManager: SocketManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPoliceHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSosAlerts()
        setupCardClickListeners()
        observeViewModel()
        startOfficerDutyService()
        collectSosAssignments()
    }

    private lateinit var sosAlertsAdapter: SosAlertsAdapter

    private fun setupSosAlerts() {
        sosAlertsAdapter = SosAlertsAdapter { alert ->
            val bundle = Bundle().apply { putString("sosId", alert.id) }
            findNavController().navigate(R.id.action_policeHomeFragment_to_activeSosDetailFragment, bundle)
        }
        binding.rvSosAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSosAlerts.adapter = sosAlertsAdapter
        binding.tvViewAllSos.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_sosHistoryFragment)
        }
    }

    private fun setupCardClickListeners() {
        binding.cardFaceRecognition.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_cameraFragment)
        }
        binding.cardWeaponDetection.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_cameraFragment)
        }
        binding.cardAnpr.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_cameraFragment)
        }
        binding.cardVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_vehicleFragment)
        }
        binding.cardCaseManagement.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_casesFragment)
        }
        binding.cardCrimeMap.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_crimeMapFragment)
        }
        binding.cardPcrVan.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_pcrVanFragment)
        }
        binding.cardCriminalSearch.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_criminalSearchFragment)
        }
        binding.ivProfilePic.setOnClickListener {
            findNavController().navigate(R.id.action_policeHomeFragment_to_profileFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvGreeting.text = "Hello, ${it.name}! \uD83D\uDC4B"
                binding.tvBadge.text = "Badge #${it.badgeId ?: "N/A"}"
            }
        }

        viewModel.sosAlerts.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { /* loading */ }
                is Resource.Success -> {
                    val alerts = resource.data
                    sosAlertsAdapter.submitList(alerts)
                    binding.tvNoAlerts.isVisible = alerts.isEmpty()
                    binding.rvSosAlerts.isVisible = alerts.isNotEmpty()
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Start OfficerDutyService so live location sharing begins when officer opens dashboard */
    private fun startOfficerDutyService() {
        val intent = Intent(requireContext(), OfficerDutyService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    /** Collect SOS assignments emitted by SocketManager and show a dialog */
    private fun collectSosAssignments() {
        viewLifecycleOwner.lifecycleScope.launch {
            socketManager.sosAssigned.collect { event ->
                if (isAdded) showSosAssignedDialog(event)
            }
        }
    }

    private fun showSosAssignedDialog(event: SosAssignmentEvent) {
        AlertDialog.Builder(requireContext())
            .setTitle("🚨 SOS Assigned to You")
            .setMessage(
                "Caller: ${event.callerName}\n" +
                "Phone: ${event.callerPhone}\n" +
                "Location: ${event.address.ifBlank { "${event.latitude}, ${event.longitude}" }}"
            )
            .setPositiveButton("Navigate") { _, _ ->
                openGoogleMapsNavigation(event.latitude, event.longitude)
            }
            .setNegativeButton("Dismiss", null)
            .setCancelable(false)
            .show()
    }

    private fun openGoogleMapsNavigation(lat: Double, lng: Double) {
        val mapsUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapsIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            // Fallback: open in browser
            val webUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"
            )
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

