package com.dial112.ui.police

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.R
import com.dial112.databinding.ItemCriminalBinding
import com.dial112.domain.model.Criminal

class CriminalAdapter(
    private val onClick: (Criminal) -> Unit
) : ListAdapter<Criminal, CriminalAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemCriminalBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClick(getItem(adapterPosition)) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemCriminalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = getItem(position)
        with(holder.binding) {
            tvCriminalName.text = c.name
            tvCriminalInitials.text = c.name.take(2).uppercase()
            tvCriminalAddress.text = c.lastKnownAddress?.let { "Last seen: $it" } ?: "Address unknown"
            tvCrimesCount.text = if (c.crimeHistory.isEmpty()) "No records" else "${c.crimeHistory.size} offense${if (c.crimeHistory.size > 1) "s" else ""}"

            // Danger level chip
            val (chipBg, chipLabel) = when (c.dangerLevel.uppercase()) {
                "CRITICAL" -> Pair(R.color.primary_red, "CRITICAL")
                "HIGH" -> Pair(R.color.accent_orange, "HIGH")
                "MEDIUM" -> Pair(R.color.accent_blue, "MEDIUM")
                else -> Pair(R.color.accent_teal, "LOW")
            }
            chipDangerLevel.text = chipLabel
            chipDangerLevel.setChipBackgroundColorResource(chipBg)

            // Warrant status
            tvWarrantStatus.text = if (c.warrantStatus) "WARRANT ACTIVE" else "NO WARRANT"
            tvWarrantStatus.setBackgroundResource(
                if (c.warrantStatus) R.drawable.bg_chip_red else R.drawable.bg_chip_neutral
            )
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Criminal>() {
            override fun areItemsTheSame(a: Criminal, b: Criminal) = a.id == b.id
            override fun areContentsTheSame(a: Criminal, b: Criminal) = a == b
        }
    }
}
