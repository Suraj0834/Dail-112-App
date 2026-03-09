package com.dial112.ui.police

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.databinding.ItemSosAlertBinding
import com.dial112.domain.model.SosEmergency

class SosAlertsAdapter(
    private val onItemClick: (SosEmergency) -> Unit
) : ListAdapter<SosEmergency, SosAlertsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemSosAlertBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alert: SosEmergency) {
            binding.tvAlertType.text = alert.type.replace("_", " ").uppercase()
            binding.tvAlertAddress.text = alert.address.ifBlank { "${alert.latitude}, ${alert.longitude}" }
            binding.root.setOnClickListener { onItemClick(alert) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemSosAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SosEmergency>() {
            override fun areItemsTheSame(a: SosEmergency, b: SosEmergency) = a.id == b.id
            override fun areContentsTheSame(a: SosEmergency, b: SosEmergency) = a == b
        }
    }
}
