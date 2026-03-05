package com.magnum.cricketclub.ui.incometype

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.IncomeType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomeTypesActivity : AppCompatActivity() {
    private lateinit var viewModel: IncomeTypeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var fabAddIncomeType: FloatingActionButton
    private lateinit var adapter: IncomeTypeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_types)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.income_types)

        viewModel = ViewModelProvider(this)[IncomeTypeViewModel::class.java]

        recyclerView = findViewById(R.id.expenseTypesRecyclerView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        fabAddIncomeType = findViewById(R.id.fabAddExpenseType)

        adapter = IncomeTypeAdapter(
            onEditClick = { incomeType ->
                showAddEditDialog(incomeType)
            },
            onDeleteClick = { incomeType ->
                showDeleteDialog(incomeType)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAddIncomeType.setOnClickListener {
            showAddEditDialog(null)
        }

        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { types ->
                adapter.submitList(types)
                emptyStateTextView.visibility = if (types.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showAddEditDialog(incomeType: IncomeType?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_expense_type, null)
        val typeNameLabel = dialogView.findViewById<TextView>(R.id.typeNameLabelTextView)
        val typeNameHint = dialogView.findViewById<TextView>(R.id.typeNameHintTextView)
        val nameEditText = dialogView.findViewById<EditText>(R.id.expenseTypeNameEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.expenseTypeDescriptionEditText)
        val saveButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        typeNameLabel.text = getString(R.string.income_type)
        typeNameHint.text = getString(R.string.enter_income_type_name)

        if (incomeType != null) {
            nameEditText.setText(incomeType.name)
            descriptionEditText.setText(incomeType.description)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle(if (incomeType != null) getString(R.string.edit_income_type) else getString(R.string.add_income_type))
            .create()

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (name.isEmpty()) {
                nameEditText.error = getString(R.string.enter_income_type_name)
                return@setOnClickListener
            }
            nameEditText.error = null

            val updatedIncomeType = if (incomeType != null) {
                incomeType.copy(name = name, description = description)
            } else {
                IncomeType(name = name, description = description)
            }

            if (incomeType != null) {
                viewModel.updateIncomeType(updatedIncomeType)
            } else {
                viewModel.insertIncomeType(updatedIncomeType)
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog(incomeType: IncomeType) {
        AlertDialog.Builder(this)
            .setTitle("Delete Income Type")
            .setMessage("Are you sure you want to delete \"${incomeType.name}\"? This will not delete associated incomes.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteIncomeType(incomeType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
