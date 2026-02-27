package com.magnum.cricketclub.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.expense.AddEditExpenseActivity
import com.magnum.cricketclub.ui.expense.ExpenseAdapter
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var totalBalanceTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var totalIncomesTextView: TextView
    private lateinit var emptyStateTextView: TextView
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var expenseAdapter: ExpenseAdapter
    private var expenseTypes: List<ExpenseType> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.expenses)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        totalIncomesTextView = findViewById(R.id.totalIncomesTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        
        // Setup card click listeners
        val expensesCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.expensesCard)
        val incomesCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.incomesCard)
        val expenseTypesCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.expenseTypesCard)
        val incomeTypesCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.incomeTypesCard)
        val chartsCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.chartsCard)
        
        expensesCard.setOnClickListener {
            // Scroll to expenses list or highlight it
            expensesRecyclerView.smoothScrollToPosition(0)
        }
        
        incomesCard.setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.income.IncomesActivity::class.java))
        }
        
        expenseTypesCard.setOnClickListener {
            startActivity(Intent(this, ExpenseTypesActivity::class.java))
        }
        
        incomeTypesCard.setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.incometype.IncomeTypesActivity::class.java))
        }
        
        chartsCard.setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.charts.ChartsActivity::class.java))
        }

        expenseAdapter = ExpenseAdapter(
            onEditClick = { expense ->
                val intent = Intent(this, AddEditExpenseActivity::class.java)
                intent.putExtra("expense_id", expense.id)
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                viewModel.deleteExpense(expense)
            }
        )

        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter

        fabAddExpense.setOnClickListener {
            val intent = Intent(this, AddEditExpenseActivity::class.java)
            startActivity(intent)
        }

        // Observe expenses (only expenses, not incomes)
        lifecycleScope.launch {
            viewModel.allExpensesOnly.collectLatest { expenses ->
                expenseAdapter.submitList(expenses)
                expenseAdapter.setExpenseTypes(expenseTypes)
                emptyStateTextView.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe expense types
        lifecycleScope.launch {
            viewModel.allExpenseTypes.collectLatest { types ->
                expenseTypes = types
                expenseAdapter.setExpenseTypes(types)
            }
        }

        // Observe income types (for backward compatibility)
        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { types ->
                expenseAdapter.setIncomeTypes(types)
            }
        }

        // Observe total balance
        lifecycleScope.launch {
            viewModel.totalBalance.collectLatest { balance ->
                val balanceText = "₹${String.format("%.2f", balance ?: 0.0)}"
                totalBalanceTextView.text = balanceText
                val color = if ((balance ?: 0.0) >= 0) 
                    getColor(R.color.green) 
                else 
                    getColor(R.color.red)
                totalBalanceTextView.setTextColor(color)
            }
        }
        
        // Calculate total expenses
        lifecycleScope.launch {
            viewModel.allExpensesOnly.collectLatest { expenses ->
                val total = expenses.sumOf { it.amount }
                totalExpensesTextView.text = "₹${String.format("%.2f", total)}"
            }
        }
        
        // Calculate total incomes
        lifecycleScope.launch {
            viewModel.allIncomes.collectLatest { incomes ->
                val total = incomes.sumOf { it.amount }
                totalIncomesTextView.text = "₹${String.format("%.2f", total)}"
            }
        }
        
        // Setup bottom navigation
        setupBottomNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_notifications -> {
                // TODO: Implement notifications screen
                android.widget.Toast.makeText(this, "Notifications coming soon", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, ConfigActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
