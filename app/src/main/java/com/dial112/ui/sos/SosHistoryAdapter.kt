package com.dial112.ui.sos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.R
import com.dial112.databinding.ItemSosHistoryBinding
import com.dial112.domain.model.SosLog
import java.text.SimpleDateFormat
import java.util.*

class SosHistoryAdapter(
    private val onClick: (SosLog) -> Unit = {}
) : ListAdapter<SosLog, SosHistoryAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemSosHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClick(getItem(adapterPosition)) } }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemSosHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val sos = getItem(position)
        with(holder.binding) {
            tvSosType.text = sos.type.replace("_", " ").replaceFirstChar { it.uppercase() }
            tvAddress.text = sos.address.ifEmpty { "Location unavailable" }
            tvDateTime.text = formatDate(sos.createdAt)

            // Status chip
            val (statusColor, statusLabel) = when (sos.status.uppercase()) {
                "ACTIVE" -> Pair(R.color.primary_red, "ACTIVE")
                "RESPONDED" -> Pair(R.color.accent_blue, "RESPONDED")
                "RESOLVED" -> Pair(R.color.accent_green, "RESOLVED")
                "FALSE_ALARM" -> Pair(R.color.accent_purple, "FALSE ALARM")
                else -> Pair(R.color.accent_teal, sos.status.uppercase())
            }
            chipStatus.text = statusLabel
            chipStatus.setChipBackgroundColorResource(statusColor)
            statusBar.setBackgroundResource(
                when (sos.status.uppercase()) {
                    "ACTIVE" -> R.drawable.bg_circle_red
                    "RESPONDED" -> R.color.accent_blue
                    "RESOLVED" -> R.color.accent_green
                    else -> R.color.accent_teal
                }
            )

            // Officer info
            sos.respondingOfficer?.let { officer ->
                dividerOfficer.isVisible = true
                tvOfficerName.isVisible = true
                tvOfficerName.text = "Ofc. ${officer.name}"
            } ?: run {
                dividerOfficer.isVisible = false
                tvOfficerName.isVisible = false
            }
        }
    }

    private fun formatDate(iso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val display = SimpleDateFormat("MMM d, yyyy \u2022 h:mm a", Locale.US)
            display.format(parser.parse(iso) ?: return iso)
        } catch (_: Exception) { iso }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SosLog>() {
            override fun areItemsTheSame(a: SosLog, b: SosLog) = a.id == b.id
            override fun areContentsTheSame(a: SosLog, b: SosLog) = a == b
        }
    }
}
