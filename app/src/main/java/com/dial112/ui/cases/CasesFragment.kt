package com.dial112.ui.cases

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dial112.R
import com.dial112.databinding.FragmentCasesBinding
import com.dial112.domain.model.Case
import com.dial112.domain.model.Resource
import dagger.hilt.android.AndroidEntryPoint

/**
 * CasesFragment - Displays list of filed FIR cases
 */
@AndroidEntryPoint
class CasesFragment : Fragment() {

    private var _binding: FragmentCasesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CasesViewModel by viewModels()
    private lateinit var casesAdapter: CasesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        viewModel.refreshCases()
    }

    private fun setupRecyclerView() {
        casesAdapter = CasesAdapter { case ->
            val bundle = Bundle().apply { putString("caseId", case.id) }
            findNavController().navigate(R.id.action_casesFragment_to_caseDetailFragment, bundle)
        }
        binding.rvCases.apply {
            adapter = casesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshCases() }
    }

    private fun observeViewModel() {
        viewModel.cases.observe(viewLifecycleOwner) { cases ->
            casesAdapter.submitList(cases)
            binding.tvEmpty.isVisible = cases.isEmpty()
            binding.rvCases.isVisible = cases.isNotEmpty()
        }

        viewModel.refreshState.observe(viewLifecycleOwner) { resource ->
            binding.swipeRefresh.isRefreshing = resource is Resource.Loading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
