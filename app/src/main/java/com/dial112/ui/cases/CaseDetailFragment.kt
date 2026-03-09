package com.dial112.ui.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.dial112.R
import com.dial112.databinding.FragmentCaseDetailBinding
import com.dial112.domain.model.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CaseDetailFragment : Fragment() {

    private var _binding: FragmentCaseDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CasesViewModel by viewModels()
    private lateinit var timelineAdapter: TimelineAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCaseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val caseId = arguments?.getString("caseId") ?: return

        setupToolbar()
        setupTimeline()
        viewModel.getCaseDetail(caseId)
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupTimeline() {
        timelineAdapter = TimelineAdapter()
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.caseDetail.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> { /* loading state */ }
                is Resource.Success -> {
                    val case = resource.data
                    binding.tvTitle.text = case.title
                    binding.tvDescription.text = case.description
                    binding.chipStatus.text = case.status.displayName
                    case.assignedOfficer?.let { officer ->
                        binding.tvOfficerName.text = officer.name
                        binding.tvOfficerBadge.text = "Badge: ${officer.badgeId}"
                        binding.cardOfficer.isVisible = true
                    }
                    timelineAdapter.submitList(case.timeline)
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
