package com.magnum.cricketclub.ui.expense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.data.IncomeType
import com.magnum.cricketclub.utils.WhatsAppHelper
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddEditExpenseActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var typeAutoComplete: android.widget.AutoCompleteTextView
    private lateinit var typeLabelTextView: TextView
    private lateinit var typeHintTextView: TextView
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    
    private var expenseId: Long? = null
    private var isIncome: Boolean = false
    private var expenseTypes: List<ExpenseType> = emptyList()
    private var incomeTypes: List<IncomeType> = emptyList()
    private var selectedExpenseType: ExpenseType? = null
    private var selectedIncomeType: IncomeType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_expense)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        expenseId = intent.getLongExtra("expense_id", -1).takeIf { it != -1L }
        val isIncomeFromIntent = intent.getBooleanExtra("is_income", false)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        typeAutoComplete = findViewById(R.id.expenseTypeAutoComplete)
        typeLabelTextView = findViewById(R.id.typeLabelTextView)
        typeHintTextView = findViewById(R.id.typeHintTextView)
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        typeLabelTextView.text = if (isIncomeFromIntent) getString(R.string.income_type) else getString(R.string.expense_type)
        typeHintTextView.text = if (isIncomeFromIntent) getString(R.string.select_income_type) else getString(R.string.select_expense_type)

        // Autocomplete UX: allow typing, show dropdown on tap/focus.
        typeAutoComplete.threshold = 0
        typeAutoComplete.setOnClickListener { typeAutoComplete.showDropDown() }
        typeAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) typeAutoComplete.showDropDown()
        }
        typeAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // If user types/edits manually, force re-selection from dropdown.
                selectedIncomeType = null
                selectedExpenseType = null
            }
        })

        if (expenseId != null) {
            // Will be updated after loading the expense
            supportActionBar?.title = getString(R.string.edit_expense)
            deleteButton.visibility = android.view.View.VISIBLE
        } else {
            if (isIncomeFromIntent) {
                supportActionBar?.title = getString(R.string.add_income)
                isIncome = true
            } else {
                supportActionBar?.title = getString(R.string.add_expense)
                isIncome = false
            }
            deleteButton.visibility = android.view.View.GONE
        }

        // Load expense types and income types
        lifecycleScope.launch {
            viewModel.allExpenseTypes.collectLatest { types ->
                expenseTypes = types
                if (!isIncome) {
                    setupTypeAutoComplete()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { types ->
                incomeTypes = types
                if (isIncome) {
                    setupTypeAutoComplete()
                }
            }
        }

        // Load expense if editing
        if (expenseId != null) {
            lifecycleScope.launch {
                val expense = viewModel.getExpenseById(expenseId!!)
                expense?.let {
                    isIncome = it.isIncome
                    amountEditText.setText(it.amount.toString())
                    descriptionEditText.setText(it.description)
                    
                    if (it.isIncome) {
                        supportActionBar?.title = getString(R.string.edit_income)
                        typeLabelTextView.text = getString(R.string.income_type)
                        typeHintTextView.text = getString(R.string.select_income_type)
                        // Load income type
                        val incomeTypeId = it.incomeTypeId ?: it.expenseTypeId // Backward compatibility
                        if (incomeTypeId != null) {
                            val type = viewModel.getIncomeTypeById(incomeTypeId)
                            type?.let { incomeType ->
                                selectedIncomeType = incomeType
                                typeAutoComplete.setText(incomeType.name, false)
                            }
                        }
                    } else {
                        supportActionBar?.title = getString(R.string.edit_expense)
                        typeLabelTextView.text = getString(R.string.expense_type)
                        typeHintTextView.text = getString(R.string.select_expense_type)
                        // Load expense type
                        val expenseTypeId = it.expenseTypeId
                        if (expenseTypeId != null) {
                            val type = viewModel.getExpenseTypeById(expenseTypeId)
                            type?.let { expenseType ->
                                selectedExpenseType = expenseType
                                typeAutoComplete.setText(expenseType.name, false)
                            }
                        }
                    }
                    setupTypeAutoComplete()
                }
            }
        }

        typeAutoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as? String
            if (isIncome) {
                selectedIncomeType = selectedName?.let { name -> incomeTypes.firstOrNull { it.name == name } }
            } else {
                selectedExpenseType = selectedName?.let { name -> expenseTypes.firstOrNull { it.name == name } }
            }
        }

        saveButton.setOnClickListener {
            saveExpense()
        }

        deleteButton.setOnClickListener {
            deleteExpense()
        }
    }

    private fun setupTypeAutoComplete() {
        val adapter = if (isIncome) {
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                incomeTypes.map { it.name }
            )
        } else {
            android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                expenseTypes.map { it.name }
            )
        }
        typeAutoComplete.setAdapter(adapter)
    }

    private fun saveExpense() {
        val amountText = amountEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (amountText.isEmpty()) {
            amountEditText.error = getString(R.string.enter_amount)
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            amountEditText.error = "Invalid amount"
            return
        }

        if (isIncome) {
            if (selectedIncomeType == null) {
                Toast.makeText(this, getString(R.string.select_income_type), Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (selectedExpenseType == null) {
                Toast.makeText(this, getString(R.string.select_expense_type), Toast.LENGTH_SHORT).show()
                return
            }
        }

        lifecycleScope.launch {
            val expense = if (expenseId != null) {
                Expense(
                    id = expenseId!!,
                    expenseTypeId = if (!isIncome) selectedExpenseType!!.id else null,
                    incomeTypeId = if (isIncome) selectedIncomeType!!.id else null,
                    amount = amount,
                    description = description,
                    isIncome = isIncome
                )
            } else {
                Expense(
                    expenseTypeId = if (!isIncome) selectedExpenseType!!.id else null,
                    incomeTypeId = if (isIncome) selectedIncomeType!!.id else null,
                    amount = amount,
                    description = description,
                    isIncome = isIncome
                )
            }

            if (expenseId != null) {
                viewModel.updateExpense(expense)
            } else {
                viewModel.insertExpense(expense)
            }

            // Send WhatsApp message if enabled
            val isWhatsAppEnabled = viewModel.isWhatsAppEnabled()
            if (isWhatsAppEnabled) {
                val groupId = viewModel.getWhatsAppGroupId()
                val typeName = if (isIncome) selectedIncomeType?.name else selectedExpenseType?.name
                if (groupId != null && groupId.isNotEmpty()) {
                    WhatsAppHelper.sendExpenseUpdateToGroup(
                        this@AddEditExpenseActivity,
                        groupId,
                        expense,
                        null, // This needs to be updated to handle both types
                        expenseId == null
                    )
                } else {
                    WhatsAppHelper.sendExpenseUpdate(
                        this@AddEditExpenseActivity,
                        expense,
                        null, // This needs to be updated to handle both types
                        expenseId == null
                    )
                }
            }

            finish()
        }
    }

    private fun deleteExpense() {
        if (expenseId == null) return

        AlertDialog.Builder(this)
            .setTitle("Delete ${if (isIncome) "Income" else "Expense"}")
            .setMessage("Are you sure you want to delete this ${if (isIncome) "income" else "expense"}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val expense = viewModel.getExpenseById(expenseId!!)
                    expense?.let {
                        viewModel.deleteExpense(it)
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
