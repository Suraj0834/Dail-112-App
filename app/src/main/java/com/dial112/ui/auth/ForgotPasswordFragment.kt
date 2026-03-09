package com.dial112.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentForgotPasswordBinding
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    /** Email entered in state 1 — kept to pass into resetPassword call */
    private var userEmail = ""

    private var resendTimer: CountDownTimer? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupOtpAutoAdvance()
        observeViewModel()
    }

    override fun onDestroyView() {
        resendTimer?.cancel()
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Click listeners
    // -------------------------------------------------------------------------

    private fun setupClickListeners() {
        // Back button — returns to previous screen (Login)
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // State 1: Send OTP
        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            if (email.isBlank()) {
                showSnackbar("Please enter your email address")
                return@setOnClickListener
            }
            userEmail = email
            viewModel.forgotPassword(email)
        }

        // State 2: Reset password
        binding.btnResetPassword.setOnClickListener {
            val otp = collectOtp()
            val newPass = binding.etNewPassword.text?.toString() ?: ""
            val confirmPass = binding.etConfirmPassword.text?.toString() ?: ""

            if (otp.length != 6) {
                showSnackbar("Please enter the complete 6-digit OTP")
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                showSnackbar("Passwords do not match")
                return@setOnClickListener
            }
            viewModel.resetPassword(userEmail, otp, newPass)
        }

        // Resend OTP link
        binding.tvResendOtp.setOnClickListener {
            if (userEmail.isNotBlank()) {
                viewModel.forgotPassword(userEmail)
            }
        }

        // Back to login link (in state 1)
        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    // -------------------------------------------------------------------------
    // OTP 6-box auto-advance
    // -------------------------------------------------------------------------

    private fun setupOtpAutoAdvance() {
        val otpFields = listOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4,
            binding.etOtp5,
            binding.etOtp6
        )

        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                    // Clear next box when deleting (backspace support)
                    if (s?.isEmpty() == true && i > 0) {
                        otpFields[i - 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun collectOtp(): String {
        return listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        ).joinToString("") { it.text?.toString() ?: "" }
    }

    // -------------------------------------------------------------------------
    // ViewModel observers
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        viewModel.forgotPasswordState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnSendOtp.isEnabled = false
                    binding.progressBarEmail.isVisible = true
                }
                is Resource.Success -> {
                    binding.btnSendOtp.isEnabled = true
                    binding.progressBarEmail.isVisible = false
                    // Transition to OTP state
                    switchToOtpState()
                    showSnackbar("OTP sent to $userEmail")
                    startResendCountdown()
                }
                is Resource.Error -> {
                    binding.btnSendOtp.isEnabled = true
                    binding.progressBarEmail.isVisible = false
                    showSnackbar(resource.message)
                }
            }
        }

        viewModel.resetPasswordState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnResetPassword.isEnabled = false
                    binding.progressBarOtp.isVisible = true
                }
                is Resource.Success -> {
                    binding.btnResetPassword.isEnabled = true
                    binding.progressBarOtp.isVisible = false
                    showSnackbar("Password reset successful! Please sign in.")
                    // Navigate back to login
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
                }
                is Resource.Error -> {
                    binding.btnResetPassword.isEnabled = true
                    binding.progressBarOtp.isVisible = false
                    showSnackbar(resource.message)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // UI state helpers
    // -------------------------------------------------------------------------

    private fun switchToOtpState() {
        binding.groupEmailEntry.visibility = View.GONE
        binding.groupOtpEntry.visibility = View.VISIBLE
        binding.tvOtpSentTo.text = "Enter the 6-digit OTP sent to $userEmail"
        binding.etOtp1.requestFocus()
    }

    /**
     * Start a 60-second countdown on the Resend OTP link so users can't spam it.
     */
    private fun startResendCountdown() {
        binding.tvResendOtp.isClickable = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = millisUntilFinished / 1000
                binding.tvResendOtp.text = "Resend OTP ($secs s)"
                binding.tvResendOtp.alpha = 0.5f
            }
            override fun onFinish() {
                binding.tvResendOtp.text = "Resend OTP"
                binding.tvResendOtp.alpha = 1f
                binding.tvResendOtp.isClickable = true
            }
        }.start()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
