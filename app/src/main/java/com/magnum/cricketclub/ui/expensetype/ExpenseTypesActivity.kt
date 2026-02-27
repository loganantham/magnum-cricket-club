package com.magnum.cricketclub.ui.expensetype

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.ui.expensetype.ExpenseTypeAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpenseTypesActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseTypeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var fabAddExpenseType: FloatingActionButton
    private lateinit var adapter: ExpenseTypeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_types)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.expense_types)

        viewModel = ViewModelProvider(this)[ExpenseTypeViewModel::class.java]

        recyclerView = findViewById(R.id.expenseTypesRecyclerView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        fabAddExpenseType = findViewById(R.id.fabAddExpenseType)

        adapter = ExpenseTypeAdapter(
            onEditClick = { expenseType ->
                showAddEditDialog(expenseType)
            },
            onDeleteClick = { expenseType ->
                showDeleteDialog(expenseType)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAddExpenseType.setOnClickListener {
            showAddEditDialog(null)
        }

        lifecycleScope.launch {
            viewModel.allExpenseTypes.collectLatest { types ->
                adapter.submitList(types)
                emptyStateTextView.visibility = if (types.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showAddEditDialog(expenseType: ExpenseType?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_expense_type, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.expenseTypeNameEditText)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.expenseTypeDescriptionEditText)
        val saveButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        if (expenseType != null) {
            nameEditText.setText(expenseType.name)
            descriptionEditText.setText(expenseType.description)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle(if (expenseType != null) getString(R.string.edit_expense_type) else getString(R.string.add_expense_type))
            .create()

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (name.isEmpty()) {
                nameEditText.error = getString(R.string.enter_expense_type_name)
                return@setOnClickListener
            }

            val updatedExpenseType = if (expenseType != null) {
                expenseType.copy(name = name, description = description)
            } else {
                ExpenseType(name = name, description = description)
            }

            if (expenseType != null) {
                viewModel.updateExpenseType(updatedExpenseType)
            } else {
                viewModel.insertExpenseType(updatedExpenseType)
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(expenseType: ExpenseType) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense Type")
            .setMessage("Are you sure you want to delete \"${expenseType.name}\"? This will not delete associated expenses.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExpenseType(expenseType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
