package com.dial112.ui.police

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dial112.R
import com.dial112.databinding.FragmentCriminalSearchBinding
import com.dial112.domain.model.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CriminalSearchFragment : Fragment() {

    private var _binding: FragmentCriminalSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CriminalSearchViewModel by viewModels()
    private lateinit var adapter: CriminalAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCriminalSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = CriminalAdapter { criminal ->
            val bundle = Bundle().apply { putString("criminalId", criminal.id) }
            findNavController().navigate(R.id.action_criminalSearchFragment_to_criminalDetailFragment, bundle)
        }
        binding.recyclerViewCriminals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCriminals.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchCriminals(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        observeViewModel()
        viewModel.searchCriminals("", debounce = false)
    }

    private fun observeViewModel() {
        viewModel.criminals.observe(viewLifecycleOwner) { resource ->
            binding.progressBar.isVisible = resource is Resource.Loading
            when (resource) {
                is Resource.Success -> {
                    adapter.submitList(resource.data)
                    binding.emptyState.isVisible = resource.data.isEmpty()
                    binding.recyclerViewCriminals.isVisible = resource.data.isNotEmpty()
                    if (resource.data.isEmpty()) {
                        binding.tvEmptyText.text = if (binding.etSearch.text.isNullOrBlank())
                            "No criminal records available"
                        else "No results for \"${binding.etSearch.text}\""
                    }
                }
                is Resource.Error -> {
                    binding.emptyState.isVisible = true
                    binding.tvEmptyText.text = resource.message
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
