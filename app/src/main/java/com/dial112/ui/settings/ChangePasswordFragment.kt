package com.dial112.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dial112.databinding.FragmentChangePasswordBinding
import com.dial112.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnChangePassword.setOnClickListener { attemptChangePassword() }
    }

    private fun attemptChangePassword() {
        val current = binding.etCurrentPassword.text?.toString()?.trim() ?: ""
        val newPwd = binding.etNewPassword.text?.toString()?.trim() ?: ""
        val confirm = binding.etConfirmPassword.text?.toString()?.trim() ?: ""

        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        if (current.isEmpty()) { binding.tilCurrentPassword.error = "Required"; return }
        if (newPwd.length < 6) { binding.tilNewPassword.error = "Min 6 characters"; return }
        if (newPwd != confirm) { binding.tilConfirmPassword.error = "Passwords do not match"; return }

        binding.loadingOverlay.isVisible = true
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                val body = JSONObject().apply {
                    put("currentPassword", current)
                    put("newPassword", newPwd)
                }.toString().toRequestBody("application/json".toMediaType())

                val baseUrl = com.dial112.BuildConfig.BASE_URL.trimEnd('/')
                val request = Request.Builder()
                    .url("$baseUrl/api/auth/change-password")
                    .addHeader("Authorization", "Bearer $token")
                    .put(body)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                binding.loadingOverlay.isVisible = false
                if (response.isSuccessful) {
                    Snackbar.make(binding.root, "Password changed successfully", Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    val msg = try { JSONObject(response.body?.string() ?: "").optString("message", "Failed") } catch (_: Exception) { "Failed" }
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.loadingOverlay.isVisible = false
                Snackbar.make(binding.root, "Network error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
