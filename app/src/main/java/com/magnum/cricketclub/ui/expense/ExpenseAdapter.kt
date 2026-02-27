package com.magnum.cricketclub.ui.expense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.data.IncomeType
import com.magnum.cricketclub.utils.DateUtils
import com.google.android.material.button.MaterialButton

class ExpenseAdapter(
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    private var expenseTypes: Map<Long, ExpenseType> = emptyMap()
    private var incomeTypes: Map<Long, IncomeType> = emptyMap()

    fun setExpenseTypes(types: List<ExpenseType>) {
        expenseTypes = types.associateBy { it.id }
        notifyDataSetChanged()
    }

    fun setIncomeTypes(types: List<IncomeType>) {
        incomeTypes = types.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val expenseTypeTextView: TextView = itemView.findViewById(R.id.expenseTypeTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)

        fun bind(expense: Expense) {
            val typeName = if (expense.isIncome) {
                val incomeTypeId = expense.incomeTypeId ?: expense.expenseTypeId // Backward compatibility
                incomeTypes[incomeTypeId]?.name ?: "Unknown"
            } else {
                expenseTypes[expense.expenseTypeId]?.name ?: "Unknown"
            }
            expenseTypeTextView.text = typeName
            
            if (expense.description.isNotEmpty()) {
                descriptionTextView.text = expense.description
                descriptionTextView.visibility = View.VISIBLE
            } else {
                descriptionTextView.visibility = View.GONE
            }
            
            dateTextView.text = DateUtils.formatDateTime(expense.date)
            
            val sign = if (expense.isIncome) "+" else "-"
            val color = if (expense.isIncome) 
                itemView.context.getColor(R.color.green) 
            else 
                itemView.context.getColor(R.color.red)
            
            amountTextView.text = "$sign₹${String.format("%.2f", expense.amount)}"
            amountTextView.setTextColor(color)
            
            editButton.setOnClickListener { onEditClick(expense) }
            deleteButton.setOnClickListener { onDeleteClick(expense) }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
