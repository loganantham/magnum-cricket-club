package com.magnum.cricketclub.ui.expensetype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.ExpenseType
import com.google.android.material.button.MaterialButton

class ExpenseTypeAdapter(
    private val onEditClick: (ExpenseType) -> Unit,
    private val onDeleteClick: (ExpenseType) -> Unit
) : ListAdapter<ExpenseType, ExpenseTypeAdapter.ExpenseTypeViewHolder>(ExpenseTypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_type, parent, false)
        return ExpenseTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.expenseTypeNameTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.expenseTypeDescriptionTextView)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(expenseType: ExpenseType) {
            nameTextView.text = expenseType.name
            
            if (expenseType.description.isNotEmpty()) {
                descriptionTextView.text = expenseType.description
                descriptionTextView.visibility = View.VISIBLE
            } else {
                descriptionTextView.visibility = View.GONE
            }
            
            editButton.setOnClickListener { onEditClick(expenseType) }
            deleteButton.setOnClickListener { onDeleteClick(expenseType) }
        }
    }

    class ExpenseTypeDiffCallback : DiffUtil.ItemCallback<ExpenseType>() {
        override fun areItemsTheSame(oldItem: ExpenseType, newItem: ExpenseType): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpenseType, newItem: ExpenseType): Boolean {
            return oldItem == newItem
        }
    }
}
