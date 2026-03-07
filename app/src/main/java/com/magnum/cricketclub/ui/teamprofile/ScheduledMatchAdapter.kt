package com.magnum.cricketclub.ui.teamprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UpcomingMatch
import java.text.SimpleDateFormat
import java.util.*

class ScheduledMatchAdapter(
    private val canEdit: Boolean,
    private val onEditClick: (UpcomingMatch) -> Unit,
    private val onDeleteClick: (UpcomingMatch) -> Unit
) : ListAdapter<UpcomingMatch, ScheduledMatchAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scheduled_match, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val matchDateText: TextView = itemView.findViewById(R.id.matchDateText)
        private val matchTypeText: TextView = itemView.findViewById(R.id.matchTypeText)
        private val teamsText: TextView = itemView.findViewById(R.id.teamsText)
        private val groundInfoText: TextView = itemView.findViewById(R.id.groundInfoText)
        private val oversText: TextView = itemView.findViewById(R.id.oversText)
        private val feesText: TextView = itemView.findViewById(R.id.feesText)
        private val btnEditMatch: ImageButton = itemView.findViewById(R.id.btnEditMatch)
        private val btnDeleteMatch: ImageButton = itemView.findViewById(R.id.btnDeleteMatch)

        fun bind(match: UpcomingMatch) {
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            matchDateText.text = formatter.format(Date(match.dateUtcMillis))
            matchTypeText.text = if (match.matchType == "MAGNUM_MATCH") "Magnum Match" else "Magnum Ground Match"
            teamsText.text = "${match.team1} vs ${match.team2}"
            groundInfoText.text = "${match.groundName}, ${match.groundLocation}"
            oversText.text = "${match.overs} Overs"
            feesText.text = "Fees: ₹${match.groundFees}"

            btnEditMatch.visibility = if (canEdit) View.VISIBLE else View.GONE
            btnDeleteMatch.visibility = if (canEdit) View.VISIBLE else View.GONE

            btnEditMatch.setOnClickListener { onEditClick(match) }
            btnDeleteMatch.setOnClickListener { onDeleteClick(match) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UpcomingMatch>() {
        override fun areItemsTheSame(oldItem: UpcomingMatch, newItem: UpcomingMatch): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UpcomingMatch, newItem: UpcomingMatch): Boolean = oldItem == newItem
    }
}
