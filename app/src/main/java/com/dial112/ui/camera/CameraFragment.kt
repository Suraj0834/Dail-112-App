package com.dial112.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentCameraBinding
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentMode = CameraMode.FACE

    enum class CameraMode { FACE, WEAPON, ANPR }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showMessage("Camera permission required")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupToolbar()
        setupModeFromArgs()
        setupClickListeners()
        observeViewModel()
        checkCameraPermission()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupModeFromArgs() {
        val modeArg = arguments?.getString("mode") ?: "FACE"
        currentMode = try { CameraMode.valueOf(modeArg) } catch (_: Exception) { CameraMode.FACE }
        updateModeUI()
    }

    private fun updateModeUI() {
        binding.tvMode.text = when (currentMode) {
            CameraMode.FACE -> "Face Recognition"
            CameraMode.WEAPON -> "Weapon Detection"
            CameraMode.ANPR -> "Number Plate Recognition"
        }
    }

    private fun setupClickListeners() {
        binding.btnCapture.setOnClickListener { captureImage() }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                showMessage("Camera init failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        binding.progressBar.isVisible = true

        val photoFile = File(requireContext().cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bytes = photoFile.readBytes()
                    when (currentMode) {
                        CameraMode.FACE -> viewModel.recognizeFace(bytes)
                        CameraMode.WEAPON -> viewModel.detectWeapon(bytes)
                        CameraMode.ANPR -> viewModel.detectPlate(bytes)
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    binding.progressBar.isVisible = false
                    showMessage("Capture failed: ${exception.message}")
                }
            })
    }

    private fun observeViewModel() {
        viewModel.faceResult.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading
            when (resource) {
                is Resource.Success -> {
                    val r = resource.data
                    if (r.matched) {
                        binding.tvResultLabel.text = "Match Found"
                        binding.tvResultDetail.text = "${r.name} (${(r.confidence?.times(100))?.toInt()}%)"
                        binding.cardResult.isVisible = true
                        // Navigate to criminal detail if we have a criminal ID
                        r.criminalId?.let { criminalId ->
                            val bundle = Bundle().apply { putString("criminalId", criminalId) }
                            findNavController().navigate(R.id.action_cameraFragment_to_criminalDetailFragment, bundle)
                        }
                    } else {
                        binding.tvResultLabel.text = "No Match"
                        binding.tvResultDetail.text = "No match found in database"
                        binding.cardResult.isVisible = true
                    }
                }
                is Resource.Error -> showMessage(resource.message)
                else -> {}
            }
        }

        viewModel.weaponResult.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading
            when (resource) {
                is Resource.Success -> {
                    val r = resource.data
                    if (r.weaponDetected) {
                        binding.tvResultLabel.text = "Weapon Detected"
                        binding.tvResultDetail.text = r.detections.joinToString { it.label }
                    } else {
                        binding.tvResultLabel.text = "Clear"
                        binding.tvResultDetail.text = "No weapons detected"
                    }
                    binding.cardResult.isVisible = true
                }
                is Resource.Error -> showMessage(resource.message)
                else -> {}
            }
        }

        viewModel.anprResult.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading
            when (resource) {
                is Resource.Success -> {
                    binding.tvResultLabel.text = "Plate Recognized"
                    binding.tvResultDetail.text = resource.data
                    binding.cardResult.isVisible = true
                }
                is Resource.Error -> showMessage(resource.message)
                else -> {}
            }
        }
    }

    private fun showMessage(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
