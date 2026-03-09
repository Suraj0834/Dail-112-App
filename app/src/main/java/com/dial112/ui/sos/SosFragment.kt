package com.dial112.ui.sos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.databinding.FragmentSosBinding
import com.dial112.utils.services.SosEmergencyService
import dagger.hilt.android.AndroidEntryPoint

/**
 * SosFragment - Active SOS screen showing tracking status
 * Displays Lottie animation, officer dispatch info, cancel option
 */
@AndroidEntryPoint
class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start Lottie loading animation
        binding.lottieLoading.playAnimation()

        binding.btnCancelSos.setOnClickListener {
            // Stop the foreground SOS service
            val stopIntent = Intent(requireContext(), SosEmergencyService::class.java).apply {
                action = SosEmergencyService.ACTION_STOP_SOS
            }
            requireContext().startService(stopIntent)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
