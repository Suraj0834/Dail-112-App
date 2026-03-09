package com.dial112.ui.police

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dial112.R
import com.dial112.databinding.FragmentCriminalDetailBinding
import com.dial112.domain.model.Criminal
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CriminalDetailFragment : Fragment() {

    private var _binding: FragmentCriminalDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CriminalSearchViewModel by viewModels()
    private val criminalId: String by lazy { arguments?.getString("criminalId") ?: "" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCriminalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        observeViewModel()
        viewModel.loadCriminalDetail(criminalId)
    }

    private fun observeViewModel() {
        viewModel.criminalDetail.observe(viewLifecycleOwner) { resource ->
            binding.loadingOverlay.isVisible = resource is Resource.Loading
            when (resource) {
                is Resource.Success -> bindCriminal(resource.data)
                is Resource.Error -> Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun bindCriminal(c: Criminal) {
        binding.tvCriminalName.text = c.name
        binding.tvInitials.text = c.name.take(2).uppercase()
        binding.tvAge.text = c.age?.toString() ?: "Unknown"
        binding.tvGender.text = c.gender?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        binding.tvAddress.text = c.lastKnownAddress ?: "Unknown"

        // Danger level chip
        val (chipColor, label) = when (c.dangerLevel.uppercase()) {
            "CRITICAL" -> Pair(R.color.primary_red, "CRITICAL RISK")
            "HIGH" -> Pair(R.color.accent_orange, "HIGH RISK")
            "MEDIUM" -> Pair(R.color.accent_blue, "MEDIUM RISK")
            else -> Pair(R.color.accent_teal, "LOW RISK")
        }
        binding.chipDangerLevel.text = label
        binding.chipDangerLevel.setChipBackgroundColorResource(chipColor)

        // Warrant
        binding.chipWarrant.text = if (c.warrantStatus) "WARRANT ACTIVE" else "NO WARRANT"
        binding.chipWarrant.setChipBackgroundColorResource(
            if (c.warrantStatus) R.color.primary_red else R.color.accent_teal
        )

        // Description
        c.description?.let {
            binding.cardDescription.isVisible = true
            binding.tvDescription.text = it
        }

        // Crime history timeline
        binding.crimeHistoryContainer.removeAllViews()
        if (c.crimeHistory.isEmpty()) {
            binding.tvNoCrimes.isVisible = true
        } else {
            binding.tvNoCrimes.isVisible = false
            c.crimeHistory.forEachIndexed { index, entry ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_crime_history_entry, binding.crimeHistoryContainer, false
                )
                itemView.findViewById<TextView>(R.id.tvOffense).text = entry.offense
                itemView.findViewById<TextView>(R.id.tvCrimeDate).text = entry.date ?: "Date unknown"
                itemView.findViewById<TextView>(R.id.tvCrimeStatus).text = entry.status ?: "Unknown"
                // Hide timeline dot connector for last item
                itemView.findViewById<View>(R.id.timelineConnector).isVisible = index < c.crimeHistory.lastIndex
                binding.crimeHistoryContainer.addView(itemView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
