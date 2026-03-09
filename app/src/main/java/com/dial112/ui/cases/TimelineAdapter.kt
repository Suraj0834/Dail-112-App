package com.dial112.ui.cases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dial112.databinding.ItemTimelineBinding
import com.dial112.domain.model.TimelineEntry

class TimelineAdapter : ListAdapter<TimelineEntry, TimelineAdapter.TimelineViewHolder>(TimelineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimelineViewHolder(private val binding: ItemTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: TimelineEntry) {
            binding.tvStatus.text = entry.status
            binding.tvNote.text = entry.note
            binding.tvTimestamp.text = entry.timestamp.take(16).replace("T", " ")
        }
    }

    class TimelineDiffCallback : DiffUtil.ItemCallback<TimelineEntry>() {
        override fun areItemsTheSame(oldItem: TimelineEntry, newItem: TimelineEntry) =
            oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: TimelineEntry, newItem: TimelineEntry) =
            oldItem == newItem
    }
}
