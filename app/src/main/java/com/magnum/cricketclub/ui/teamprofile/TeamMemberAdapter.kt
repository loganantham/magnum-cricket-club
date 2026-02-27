package com.magnum.cricketclub.ui.teamprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfile

class TeamMemberAdapter : ListAdapter<UserProfile, TeamMemberAdapter.TeamMemberViewHolder>(DiffCallback()) {

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team_member, parent, false)
        return TeamMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamMemberViewHolder, position: Int) {
        val profile = getItem(position)
        val isExpanded = position == expandedPosition
        holder.bind(profile, isExpanded) {
            // Toggle expansion
            val previousExpanded = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(previousExpanded)
            if (previousExpanded != position) {
                notifyItemChanged(position)
            }
        }
    }

    class TeamMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
        private val roleTextView: TextView = itemView.findViewById(R.id.roleTextView)
        private val mobileTextView: TextView = itemView.findViewById(R.id.mobileTextView)
        private val alternateMobileTextView: TextView = itemView.findViewById(R.id.alternateMobileTextView)
        private val alternateMobileLayout: LinearLayout = itemView.findViewById(R.id.alternateMobileLayout)
        private val detailsLayout: LinearLayout = itemView.findViewById(R.id.detailsLayout)
        private val cardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(profile: UserProfile, isExpanded: Boolean, onItemClick: () -> Unit) {
            // Set basic info
            nameTextView.text = profile.name ?: "-"
            roleTextView.text = profile.playerPreference ?: "-"
            
            // Set details
            emailTextView.text = profile.email
            mobileTextView.text = profile.mobileNumber ?: "-"
            
            // Handle alternate mobile number
            val alternateMobile = profile.alternateMobileNumber
            if (!alternateMobile.isNullOrBlank()) {
                alternateMobileTextView.text = alternateMobile
                alternateMobileLayout.visibility = View.VISIBLE
            } else {
                alternateMobileLayout.visibility = View.GONE
            }
            
            // Toggle details visibility
            detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Set click listener
            cardView.setOnClickListener {
                onItemClick()
            }
            
            // Add visual feedback for expanded state
            if (isExpanded) {
                // Light blue background for expanded items
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
            } else {
                cardView.setCardBackgroundColor(android.graphics.Color.WHITE)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem.email == newItem.email
        }

        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem == newItem
        }
    }
}
