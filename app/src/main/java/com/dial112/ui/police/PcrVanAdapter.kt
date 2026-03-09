package com.dial112.ui.police

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.R
import com.dial112.databinding.ItemPcrVanBinding
import com.dial112.domain.model.PcrVan

class PcrVanAdapter(
    private val onItemClick: (PcrVan) -> Unit
) : ListAdapter<PcrVan, PcrVanAdapter.PcrVanViewHolder>(PcrVanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PcrVanViewHolder {
        val binding = ItemPcrVanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PcrVanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PcrVanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PcrVanViewHolder(
        private val binding: ItemPcrVanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(van: PcrVan) {
            binding.tvVanName.text = van.vehicleName.ifBlank { "PCR Van" }
            binding.tvPlateNo.text = van.plateNo

            binding.chipStatus.text = van.status
            val chipColor = when (van.status.lowercase()) {
                "available"   -> ContextCompat.getColor(binding.root.context, R.color.accent_green)
                "busy"        -> ContextCompat.getColor(binding.root.context, R.color.primary_red)
                "off-duty",
                "off_duty"    -> Color.parseColor("#78909C")
                "maintenance" -> ContextCompat.getColor(binding.root.context, R.color.accent_orange)
                else          -> ContextCompat.getColor(binding.root.context, R.color.accent_blue)
            }
            binding.chipStatus.setChipBackgroundColorResource(0) // clear resource first
            binding.chipStatus.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(chipColor)

            binding.tvOfficerName.text = van.assignedOfficer?.name?.let { "Officer: $it" }
                ?: "Unassigned"

            binding.tvLocation.text = if (van.latitude != 0.0 && van.longitude != 0.0) {
                van.address.ifBlank { "%.5f, %.5f".format(van.latitude, van.longitude) }
            } else {
                "Location unavailable"
            }

            binding.tvLastSeen.text = van.lastSeen?.let { "Last seen: $it" } ?: ""

            binding.root.setOnClickListener { onItemClick(van) }
        }
    }

    class PcrVanDiffCallback : DiffUtil.ItemCallback<PcrVan>() {
        override fun areItemsTheSame(oldItem: PcrVan, newItem: PcrVan) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PcrVan, newItem: PcrVan) = oldItem == newItem
    }
}
