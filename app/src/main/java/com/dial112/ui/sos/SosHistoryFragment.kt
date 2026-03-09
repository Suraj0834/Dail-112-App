package com.dial112.ui.sos

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
import com.dial112.databinding.FragmentSosHistoryBinding
import com.dial112.domain.model.Resource
import com.dial112.domain.model.SosLog
import com.dial112.domain.model.UserRole
import com.dial112.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SosHistoryFragment : Fragment() {

    private var _binding: FragmentSosHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SosHistoryViewModel by viewModels()
    private lateinit var adapter: SosHistoryAdapter

    @Inject lateinit var sessionManager: SessionManager

    private var isPolice = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSosHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val role = runBlocking { sessionManager.getUserRole() }
        isPolice = role == "police"

        adapter = SosHistoryAdapter { sos ->
            if (isPolice) {
                val bundle = Bundle().apply { putString("sosId", sos.id) }
                findNavController().navigate(R.id.action_sosHistoryFragment_to_activeSosDetailFragment, bundle)
            }
        }
        binding.recyclerViewSos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSos.adapter = adapter

        if (isPolice) {
            binding.tabLayout.isVisible = true
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == 0) viewModel.loadActiveSos() else viewModel.loadHistory()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            viewModel.loadActiveSos()
        } else {
            viewModel.loadHistory()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.sosHistory.observe(viewLifecycleOwner) { showResult(it) }
        viewModel.activeSos.observe(viewLifecycleOwner) { showResult(it) }
    }

    private fun showResult(resource: Resource<List<SosLog>>) {
        binding.progressBar.isVisible = resource is Resource.Loading
        when (resource) {
            is Resource.Success -> {
                adapter.submitList(resource.data)
                binding.emptyState.isVisible = resource.data.isEmpty()
                binding.recyclerViewSos.isVisible = resource.data.isNotEmpty()
            }
            is Resource.Error -> {
                binding.emptyState.isVisible = true
                Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
