package com.magnum.cricketclub.ui.incometype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.IncomeType
import com.google.android.material.button.MaterialButton

class IncomeTypeAdapter(
    private val onEditClick: (IncomeType) -> Unit,
    private val onDeleteClick: (IncomeType) -> Unit
) : ListAdapter<IncomeType, IncomeTypeAdapter.IncomeTypeViewHolder>(IncomeTypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_type, parent, false)
        return IncomeTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomeTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncomeTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.expenseTypeNameTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.expenseTypeDescriptionTextView)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(incomeType: IncomeType) {
            nameTextView.text = incomeType.name
            
            if (incomeType.description.isNotEmpty()) {
                descriptionTextView.text = incomeType.description
                descriptionTextView.visibility = View.VISIBLE
            } else {
                descriptionTextView.visibility = View.GONE
            }
            
            editButton.setOnClickListener { onEditClick(incomeType) }
            deleteButton.setOnClickListener { onDeleteClick(incomeType) }
        }
    }

    class IncomeTypeDiffCallback : DiffUtil.ItemCallback<IncomeType>() {
        override fun areItemsTheSame(oldItem: IncomeType, newItem: IncomeType): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncomeType, newItem: IncomeType): Boolean {
            return oldItem == newItem
        }
    }
}
