package com.dial112.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentCrimeMapBinding
import com.dial112.domain.model.Hotspot
import com.dial112.domain.model.Resource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrimeMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCrimeMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CrimeMapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var pendingHotspots: List<Hotspot>? = null

    // -------------------------------------------------------------------------
    // Location permission launcher
    // -------------------------------------------------------------------------
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableMyLocation()
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialise MapView lifecycle
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupToolbar()
        observeViewModel()
        viewModel.loadHotspots()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
        googleMap = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    // -------------------------------------------------------------------------
    // OnMapReadyCallback
    // -------------------------------------------------------------------------

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Apply dark map style
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
            )
        } catch (e: Exception) {
            // Fall back to default style if JSON is unavailable
        }

        // Move camera to India overview
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 5f)
        )

        // Enable traffic layer for context
        map.isTrafficEnabled = false

        // Enable My Location layer if permission is already granted
        requestOrEnableMyLocation()

        // Draw any hotspots that arrived before the map was ready
        pendingHotspots?.let { drawHotspots(it) }
        pendingHotspots = null
    }

    // -------------------------------------------------------------------------
    // Toolbar
    // -------------------------------------------------------------------------

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    // -------------------------------------------------------------------------
    // ViewModel observer
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        viewModel.hotspots.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { /* loading state handled by MapView itself */ }
                is Resource.Success -> {
                    val hotspots = resource.data
                    if (googleMap != null) {
                        drawHotspots(hotspots)
                    } else {
                        // Map not ready yet — cache and draw once onMapReady fires
                        pendingHotspots = hotspots
                    }
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Map drawing
    // -------------------------------------------------------------------------

    private fun drawHotspots(hotspots: List<Hotspot>) {
        val map = googleMap ?: return
        map.clear()

        for (hotspot in hotspots) {
            val center = LatLng(hotspot.latitude, hotspot.longitude)
            val (fillColor, strokeColor) = riskColors(hotspot.riskScore)

            map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(500.0)           // metres
                    .fillColor(fillColor)
                    .strokeColor(strokeColor)
                    .strokeWidth(2f)
            )
        }

        // If we have hotspots, zoom to the first one so the user sees data
        if (hotspots.isNotEmpty()) {
            val first = hotspots.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 10f)
            )
        }
    }

    /**
     * Returns (fillColor, strokeColor) pair based on riskScore (0–10 scale).
     */
    private fun riskColors(riskScore: Double): Pair<Int, Int> {
        return when {
            riskScore < 3.0 -> {
                // Low risk — green
                Pair(Color.argb(90, 76, 175, 80), Color.rgb(76, 175, 80))
            }
            riskScore < 6.0 -> {
                // Medium risk — orange
                Pair(Color.argb(90, 255, 152, 0), Color.rgb(255, 152, 0))
            }
            riskScore < 8.0 -> {
                // High risk — red
                Pair(Color.argb(90, 244, 67, 54), Color.rgb(244, 67, 54))
            }
            else -> {
                // Critical — dark red
                Pair(Color.argb(120, 183, 28, 28), Color.rgb(183, 28, 28))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Location permission helpers
    // -------------------------------------------------------------------------

    private fun requestOrEnableMyLocation() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Suppress("MissingPermission")
    private fun enableMyLocation() {
        googleMap?.isMyLocationEnabled = true
    }
}

