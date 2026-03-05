package com.magnum.cricketclub.ui.expense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
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
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddEditExpenseActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var entryTypeAutoComplete: AutoCompleteTextView
    private lateinit var typeAutoComplete: AutoCompleteTextView
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
    private var createdByEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_expense)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        expenseId = intent.getLongExtra("expense_id", -1).takeIf { it != -1L }
        val isIncomeFromIntent = intent.getBooleanExtra("is_income", false)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        entryTypeAutoComplete = findViewById(R.id.entryTypeAutoComplete)
        typeAutoComplete = findViewById(R.id.expenseTypeAutoComplete)
        typeLabelTextView = findViewById(R.id.typeLabelTextView)
        typeHintTextView = findViewById(R.id.typeHintTextView)
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        setupEntryTypeDropdown()

        // Entry Type UX
        entryTypeAutoComplete.setOnClickListener { entryTypeAutoComplete.showDropDown() }
        entryTypeAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) entryTypeAutoComplete.showDropDown()
        }
        findViewById<View>(R.id.entryTypeLayout)?.setOnClickListener {
            if (entryTypeAutoComplete.isEnabled) entryTypeAutoComplete.showDropDown()
        }

        // Autocomplete UX for category type
        typeAutoComplete.threshold = 0
        typeAutoComplete.setOnClickListener { typeAutoComplete.showDropDown() }
        typeAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) typeAutoComplete.showDropDown()
        }
        findViewById<View>(R.id.typeInputLayout)?.setOnClickListener {
            typeAutoComplete.showDropDown()
        }
        
        typeAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // If the text doesn't match any known type, reset selection
                val text = s.toString()
                if (isIncome) {
                    if (selectedIncomeType?.name != text) selectedIncomeType = null
                } else {
                    if (selectedExpenseType?.name != text) selectedExpenseType = null
                }
            }
        })

        if (expenseId != null) {
            supportActionBar?.title = getString(R.string.edit_expense)
            deleteButton.visibility = View.VISIBLE
            // Hide entry type selection when editing existing entry to prevent confusion
            entryTypeAutoComplete.isEnabled = false
            findViewById<View>(R.id.entryTypeLayout)?.alpha = 0.6f
        } else {
            isIncome = isIncomeFromIntent
            val initialEntryType = if (isIncome) "Income" else "Expense"
            entryTypeAutoComplete.setText(initialEntryType, false)
            updateCategoryUiLabels()
            deleteButton.visibility = View.GONE
        }

        // Load categories
        lifecycleScope.launch {
            viewModel.allExpenseTypes.collectLatest { types ->
                expenseTypes = types
                if (!isIncome) setupCategoryAutoComplete()
            }
        }

        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { types ->
                incomeTypes = types
                if (isIncome) setupCategoryAutoComplete()
            }
        }

        // Load existing data if editing
        if (expenseId != null) {
            lifecycleScope.launch {
                val expense = viewModel.getExpenseById(expenseId!!)
                expense?.let {
                    isIncome = it.isIncome
                    entryTypeAutoComplete.setText(if (isIncome) "Income" else "Expense", false)
                    amountEditText.setText(it.amount.toString())
                    descriptionEditText.setText(it.description)
                    createdByEmail = it.createdByEmail
                    
                    updateCategoryUiLabels()
                    
                    if (it.isIncome) {
                        val incomeTypeId = it.incomeTypeId ?: it.expenseTypeId
                        if (incomeTypeId != null) {
                            val type = viewModel.getIncomeTypeById(incomeTypeId)
                            type?.let { incomeType ->
                                selectedIncomeType = incomeType
                                typeAutoComplete.setText(incomeType.name, false)
                            }
                        }
                    } else {
                        val expenseTypeId = it.expenseTypeId
                        if (expenseTypeId != null) {
                            val type = viewModel.getExpenseTypeById(expenseTypeId)
                            type?.let { expenseType ->
                                selectedExpenseType = expenseType
                                typeAutoComplete.setText(expenseType.name, false)
                            }
                        }
                    }
                    setupCategoryAutoComplete()
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

    private fun setupEntryTypeDropdown() {
        val entryTypes = arrayOf("Expense", "Income")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, entryTypes)
        entryTypeAutoComplete.setAdapter(adapter)
        
        entryTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val newIsIncome = position == 1
            if (newIsIncome != isIncome) {
                isIncome = newIsIncome
                typeAutoComplete.setText("")
                selectedExpenseType = null
                selectedIncomeType = null
                updateCategoryUiLabels()
                setupCategoryAutoComplete()
            }
        }
    }

    private fun updateCategoryUiLabels() {
        if (isIncome) {
            supportActionBar?.title = if (expenseId == null) getString(R.string.add_income) else getString(R.string.edit_income)
            typeLabelTextView.text = getString(R.string.income_type)
            typeHintTextView.text = getString(R.string.select_income_type)
        } else {
            supportActionBar?.title = if (expenseId == null) getString(R.string.add_expense) else getString(R.string.edit_expense)
            typeLabelTextView.text = getString(R.string.expense_type)
            typeHintTextView.text = getString(R.string.select_expense_type)
        }
    }

    private fun setupCategoryAutoComplete() {
        val categories = if (isIncome) incomeTypes.map { it.name } else expenseTypes.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
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

        // Verify selection again from text if needed
        val currentTypeText = typeAutoComplete.text.toString()
        if (isIncome) {
            if (selectedIncomeType == null || selectedIncomeType?.name != currentTypeText) {
                selectedIncomeType = incomeTypes.find { it.name == currentTypeText }
            }
            if (selectedIncomeType == null) {
                Toast.makeText(this, getString(R.string.select_income_type), Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (selectedExpenseType == null || selectedExpenseType?.name != currentTypeText) {
                selectedExpenseType = expenseTypes.find { it.name == currentTypeText }
            }
            if (selectedExpenseType == null) {
                Toast.makeText(this, getString(R.string.select_expense_type), Toast.LENGTH_SHORT).show()
                return
            }
        }

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        lifecycleScope.launch {
            val expense = if (expenseId != null) {
                Expense(
                    id = expenseId!!,
                    expenseTypeId = if (!isIncome) selectedExpenseType!!.id else null,
                    incomeTypeId = if (isIncome) selectedIncomeType!!.id else null,
                    amount = amount,
                    description = description,
                    isIncome = isIncome,
                    createdByEmail = createdByEmail ?: currentUserEmail
                )
            } else {
                Expense(
                    expenseTypeId = if (!isIncome) selectedExpenseType!!.id else null,
                    incomeTypeId = if (isIncome) selectedIncomeType!!.id else null,
                    amount = amount,
                    description = description,
                    isIncome = isIncome,
                    createdByEmail = currentUserEmail
                )
            }

            if (expenseId != null) {
                viewModel.updateExpense(expense)
            } else {
                viewModel.insertExpense(expense)
            }

            // Send WhatsApp message
            val groupId = viewModel.getWhatsAppGroupId()
            if (groupId != null && groupId.isNotEmpty()) {
                val typeName = if (isIncome) selectedIncomeType?.name else selectedExpenseType?.name
                WhatsAppHelper.sendExpenseUpdateToGroup(
                    this@AddEditExpenseActivity,
                    groupId,
                    expense,
                    typeName, 
                    expenseId == null
                )
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
