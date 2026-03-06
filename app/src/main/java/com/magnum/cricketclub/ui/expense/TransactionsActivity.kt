package com.magnum.cricketclub.ui.expense

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.utils.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionsActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()
    
    private var filterType: String = TYPE_ALL
    private var selectedYear: Int = -1
    private var selectedMonth: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        filterType = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_ALL
        selectedYear = intent.getIntExtra(EXTRA_YEAR, -1)
        selectedMonth = intent.getIntExtra(EXTRA_MONTH, -1)
        
        supportActionBar?.title = when(filterType) {
            TYPE_INCOME -> getString(R.string.incomes)
            TYPE_EXPENSE -> getString(R.string.expenses)
            else -> "Transactions"
        }

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        userProfileRepository = UserProfileRepository(application)
        recyclerView = findViewById(R.id.transactionsRecyclerView)
        
        adapter = ExpenseAdapter(
            onEditClick = { expense ->
                val intent = android.content.Intent(this, AddEditExpenseActivity::class.java)
                intent.putExtra("expense_id", expense.id)
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                viewModel.deleteExpense(expense)
            },
            onShareClick = { expense, typeName ->
                shareOnWhatsApp(expense, typeName)
            },
            showBalance = filterType == TYPE_ALL,
            showActions = false // Default to false, check permissions
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        checkUserPermissions()
        observeData()
    }

    private fun checkUserPermissions() {
        val userEmail = firestoreRepository.getCurrentUserEmail() ?: return
        lifecycleScope.launch {
            val profile = try {
                val profiles = firestoreRepository.downloadAllUserProfiles()
                profiles.find { it.email == userEmail }
            } catch (e: Exception) {
                userProfileRepository.getUserProfileSync(userEmail)
            }
            
            val isMaintenance = profile?.isFinanceMaintenance() == true
            adapter.setShowActions(isMaintenance)
        }
    }

    private fun shareOnWhatsApp(expense: Expense, typeName: String?) {
        lifecycleScope.launch {
            val groupId = viewModel.getWhatsAppGroupId()
            if (!groupId.isNullOrEmpty()) {
                val latestBalance = viewModel.getLatestTotalBalance()
                WhatsAppHelper.sendExpenseUpdateToGroup(
                    this@TransactionsActivity,
                    groupId,
                    expense,
                    typeName,
                    false, // It's an existing entry
                    latestBalance
                )
            } else {
                Toast.makeText(this@TransactionsActivity, "WhatsApp Group ID not configured in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.allExpenseTypes.collectLatest { adapter.setExpenseTypes(it) }
        }
        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { adapter.setIncomeTypes(it) }
        }
        
        lifecycleScope.launch {
            viewModel.allExpenses.collectLatest { allExpenses ->
                // Sort by date ascending to calculate running balance
                val sortedAll = allExpenses.sortedBy { it.date }
                
                var running = 0.0
                val balances = mutableMapOf<Long, Double>()
                sortedAll.forEach { 
                    if (it.isIncome) running += it.amount else running -= it.amount
                    balances[it.id] = running
                }
                adapter.setRunningBalances(balances)
                
                // Now filter for display
                val filtered = allExpenses.filter { expense ->
                    val matchesType = when(filterType) {
                        TYPE_INCOME -> expense.isIncome
                        TYPE_EXPENSE -> !expense.isIncome
                        else -> true
                    }
                    
                    val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
                    val yearMatch = if (selectedYear != -1) cal.get(Calendar.YEAR) == selectedYear else true
                    val monthMatch = if (selectedMonth != -1) cal.get(Calendar.MONTH) == selectedMonth else true
                    
                    matchesType && yearMatch && monthMatch
                }
                
                // Show most recent first in the list
                adapter.submitList(filtered.sortedByDescending { it.date })
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_YEAR = "extra_year"
        const val EXTRA_MONTH = "extra_month"
        
        const val TYPE_ALL = "all"
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}
