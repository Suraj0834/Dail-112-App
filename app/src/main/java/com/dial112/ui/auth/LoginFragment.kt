package com.dial112.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentLoginBinding
import com.dial112.domain.model.Resource
import com.dial112.domain.model.UserRole
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/**
 * LoginFragment - Material 3 login screen with glassmorphism design
 *
 * Features:
 * - Glassmorphism blur card
 * - Button scale animation on press
 * - Loading state with progress indicator
 * - Role-based navigation after login (Citizen → Home, Police → PoliceHome)
 * - Error Snackbar feedback
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        playEntranceAnimation()
    }

    private fun setupClickListeners() {
        // Login button with scale animation
        binding.btnLogin.setOnClickListener {
            animateButtonPress(binding.btnLogin)
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""
            viewModel.login(email, password)
        }

        // Navigate to register
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Navigate to forgot password
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = ""
                }
                is Resource.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)

                    // Navigate based on user role
                    val action = when (resource.data.role) {
                        UserRole.POLICE ->
                            R.id.action_loginFragment_to_policeHomeFragment
                        UserRole.CITIZEN ->
                            R.id.action_loginFragment_to_homeFragment
                    }
                    findNavController().navigate(action)
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                    showError(resource.message)
                }
            }
        }
    }

    /** Entry animation - fade + slide up the bottom auth card */
    private fun playEntranceAnimation() {
        binding.scrollLogin.alpha = 0f
        binding.scrollLogin.translationY = 80f
        binding.scrollLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(200)
            .start()

        binding.lottieLogo.alpha = 0f
        binding.lottieLogo.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(100)
            .start()
    }

    /** Button press scale animation for tactile feel */
    private fun animateButtonPress(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            .start()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error_red))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
