package com.magnum.cricketclub.ui.teamprofile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UpcomingMatch
import java.text.SimpleDateFormat
import java.util.*

class GroundFeesMaintenanceAdapter(
    private val onStatusClick: (match: UpcomingMatch, teamIndex: Int) -> Unit,
    private val onDeleteMatch: (UpcomingMatch) -> Unit
) : ListAdapter<UpcomingMatch, GroundFeesMaintenanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ground_fee_maintenance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val matchTitleText: TextView = itemView.findViewById(R.id.matchTitleText)
        private val matchDateText: TextView = itemView.findViewById(R.id.matchDateText)
        private val team1NameText: TextView = itemView.findViewById(R.id.team1NameText)
        private val team1StatusText: TextView = itemView.findViewById(R.id.team1StatusText)
        private val team2NameText: TextView = itemView.findViewById(R.id.team2NameText)
        private val team2StatusText: TextView = itemView.findViewById(R.id.team2StatusText)
        private val btnDeleteMatch: MaterialButton = itemView.findViewById(R.id.btnDeleteMatch)

        fun bind(match: UpcomingMatch) {
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            matchTitleText.text = "${match.team1} vs ${match.team2}"
            matchDateText.text = formatter.format(Date(match.dateUtcMillis))
            
            team1NameText.text = match.team1
            team2NameText.text = match.team2

            bindStatus(team1StatusText, match.team1FeesStatus, match.team1PendingAmount)
            bindStatus(team2StatusText, match.team2FeesStatus, match.team2PendingAmount)

            team1StatusText.setOnClickListener { onStatusClick(match, 1) }
            team2StatusText.setOnClickListener { onStatusClick(match, 2) }

            // Show delete button only if BOTH teams are marked as DONE
            val isAllDone = match.team1FeesStatus == "DONE" && match.team2FeesStatus == "DONE"
            btnDeleteMatch.visibility = if (isAllDone) View.VISIBLE else View.GONE
            btnDeleteMatch.setOnClickListener { onDeleteMatch(match) }
        }

        private fun bindStatus(textView: TextView, status: String, pendingAmount: Double) {
            textView.text = when (status) {
                "DONE" -> "DONE"
                "PARTIAL" -> "PARTIAL (Pending: ₹$pendingAmount)"
                else -> "PENDING"
            }
            
            when (status) {
                "DONE" -> {
                    textView.setTextColor(Color.parseColor("#4CAF50"))
                    textView.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "PARTIAL" -> {
                    textView.setTextColor(Color.parseColor("#FF9800"))
                    textView.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                else -> {
                    textView.setTextColor(Color.parseColor("#F44336"))
                    textView.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UpcomingMatch>() {
        override fun areItemsTheSame(oldItem: UpcomingMatch, newItem: UpcomingMatch): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UpcomingMatch, newItem: UpcomingMatch): Boolean = oldItem == newItem
    }
}
