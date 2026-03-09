package com.dial112.ui.home

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
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
import com.dial112.databinding.FragmentHomeBinding
import com.dial112.domain.model.Resource
import com.dial112.utils.receivers.PowerButtonReceiver
import com.dial112.utils.services.SosEmergencyService
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/**
 * HomeFragment - Citizen Dashboard
 *
 * This is the main screen for citizens. Contains:
 * - Glassmorphism header with user greeting
 * - Animated SOS pulse button (primary CTA)
 * - Quick action cards (File FIR, Track Cases, Crime Map, AI Chat)
 * - Bottom navigation for citizen role
 * - Power button emergency detection integration
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    // Permission launcher for location (needed for SOS)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            triggerSos()
        } else {
            showSnackbar("Location permission required for SOS emergency")
        }
    }

    // Power button SOS broadcast receiver
    private val powerButtonSosReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == "com.dial112.ACTION_POWER_BUTTON_SOS") {
                handleSosButtonClick()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        startSosPulseAnimation()
        loadUserData()

        // Register power button receiver
        val filter = IntentFilter("com.dial112.ACTION_POWER_BUTTON_SOS")
        requireContext().registerReceiver(powerButtonSosReceiver, filter,
            android.content.Context.RECEIVER_NOT_EXPORTED)
    }

    private fun setupClickListeners() {
        // SOS Emergency Button - most prominent UI element
        binding.btnSos.setOnClickListener {
            handleSosButtonClick()
        }

        // Call 112 directly
        binding.btnCall112.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:112")
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent)
            } else {
                // Dial intent (user presses call)
                val dialIntent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:112") }
                startActivity(dialIntent)
            }
        }

        // File FIR
        binding.cardFileFir.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_fileFirFragment)
        }

        // Track Cases
        binding.cardTrackCases.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_casesFragment)
        }

        // Crime Map
        binding.cardCrimeMap.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_crimeMapFragment)
        }

        // AI Chat
        binding.cardAiChat.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatFragment)
        }

        // Vehicle Lookup
        binding.cardVehicle.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_vehicleFragment)
        }
        binding.cardSosHistory.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_sosHistoryFragment)
        }

        // Profile picture → Profile screen
        binding.ivProfilePic.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun handleSosButtonClick() {
        if (hasLocationPermission()) {
            triggerSos()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun triggerSos() {
        // Start foreground service for continuous tracking
        val serviceIntent = Intent(requireContext(), SosEmergencyService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)

        // Navigate to SOS active screen
        findNavController().navigate(R.id.action_homeFragment_to_sosFragment)
    }

    private fun observeViewModel() {
        homeViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvGreeting.text = "Hello, ${it.name.split(" ").first()}! 👋"
                binding.tvRole.text = it.role.value.replaceFirstChar { c -> c.uppercase() }
            }
        }

        homeViewModel.sosState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    findNavController().navigate(R.id.action_homeFragment_to_sosFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    showSnackbar("SOS Error: ${resource.message}")
                }
            }
        }
    }

    /** Pulsating animation for the SOS button - draws attention */
    private fun startSosPulseAnimation() {
        val pulseAnimation = android.animation.AnimatorSet().apply {
            val scaleXUp = android.animation.ObjectAnimator.ofFloat(
                binding.btnSos, "scaleX", 1f, 1.12f
            ).apply { duration = 700 }
            val scaleYUp = android.animation.ObjectAnimator.ofFloat(
                binding.btnSos, "scaleY", 1f, 1.12f
            ).apply { duration = 700 }
            val scaleXDown = android.animation.ObjectAnimator.ofFloat(
                binding.btnSos, "scaleX", 1.12f, 1f
            ).apply { duration = 700 }
            val scaleYDown = android.animation.ObjectAnimator.ofFloat(
                binding.btnSos, "scaleY", 1.12f, 1f
            ).apply { duration = 700 }

            play(scaleXUp).with(scaleYUp)
            play(scaleXDown).with(scaleYDown).after(scaleXUp)
        }

        val repeatPulse = android.animation.ValueAnimator.ofInt(0, 1).apply {
            duration = 1400
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { if (it.currentPlayTime == 0L) pulseAnimation.start() }
        }
        repeatPulse.start()
    }

    private fun loadUserData() {
        homeViewModel.loadCurrentUser()
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireContext().unregisterReceiver(powerButtonSosReceiver) } catch (e: Exception) {}
        _binding = null
    }
}
