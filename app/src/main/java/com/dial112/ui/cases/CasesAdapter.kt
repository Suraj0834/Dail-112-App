package com.dial112.ui.cases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.R
import com.dial112.databinding.ItemCaseBinding
import com.dial112.domain.model.Case
import com.dial112.domain.model.CaseStatus

/**
 * CasesAdapter - RecyclerView adapter for case list items
 */
class CasesAdapter(
    private val onCaseClick: (Case) -> Unit
) : ListAdapter<Case, CasesAdapter.CaseViewHolder>(CaseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaseViewHolder {
        val binding = ItemCaseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CaseViewHolder(
        private val binding: ItemCaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(case: Case) {
            binding.tvTitle.text = case.title
            binding.tvCategory.text = case.category.displayName
            binding.tvDate.text = case.createdAt.take(10) // YYYY-MM-DD
            binding.chipStatus.text = case.status.displayName

            // Color the status chip based on case status
            val chipColor = when (case.status) {
                CaseStatus.PENDING -> R.color.status_pending
                CaseStatus.INVESTIGATING -> R.color.status_investigating
                CaseStatus.RESOLVED -> R.color.status_resolved
                CaseStatus.CLOSED -> R.color.status_closed
            }
            binding.chipStatus.setChipBackgroundColorResource(chipColor)

            binding.root.setOnClickListener { onCaseClick(case) }
        }
    }

    class CaseDiffCallback : DiffUtil.ItemCallback<Case>() {
        override fun areItemsTheSame(oldItem: Case, newItem: Case) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Case, newItem: Case) = oldItem == newItem
    }
}
