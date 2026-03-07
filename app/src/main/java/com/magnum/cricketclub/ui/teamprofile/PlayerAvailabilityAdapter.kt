package com.magnum.cricketclub.ui.teamprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.remote.FirestoreMatchAvailability

class PlayerAvailabilityAdapter(
    private val userNamesMap: Map<String, String>,
    private val onPlayerClick: (FirestoreMatchAvailability) -> Unit
) : ListAdapter<FirestoreMatchAvailability, PlayerAvailabilityAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerNameTextView: TextView = view.findViewById(R.id.playerNameTextView)
        val availabilityTextView: TextView = view.findViewById(R.id.availabilityTextView)
        val reasonTextView: TextView = view.findViewById(R.id.reasonTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_availability, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.playerNameTextView.text = userNamesMap[item.userEmail] ?: item.userEmail
        
        // Check if lastModified is 0 or very old, which we use to indicate "Not Confirmed"
        if (item.lastModified == 0L) {
            holder.availabilityTextView.text = "Not Confirmed"
            holder.availabilityTextView.setBackgroundResource(R.drawable.status_background_pending)
            holder.reasonTextView.visibility = View.GONE
        } else if (item.available) {
            holder.availabilityTextView.text = "Available"
            holder.availabilityTextView.setBackgroundResource(R.drawable.status_background_available)
            holder.reasonTextView.visibility = View.GONE
        } else {
            holder.availabilityTextView.text = "Not Available"
            holder.availabilityTextView.setBackgroundResource(R.drawable.status_background_unavailable)
            if (!item.reason.isNullOrBlank()) {
                holder.reasonTextView.text = "Reason: ${item.reason}"
                holder.reasonTextView.visibility = View.VISIBLE
            } else {
                holder.reasonTextView.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            onPlayerClick(item)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FirestoreMatchAvailability>() {
        override fun areItemsTheSame(oldItem: FirestoreMatchAvailability, newItem: FirestoreMatchAvailability): Boolean {
            return oldItem.userEmail == newItem.userEmail
        }

        override fun areContentsTheSame(oldItem: FirestoreMatchAvailability, newItem: FirestoreMatchAvailability): Boolean {
            return oldItem == newItem
        }
    }
}
