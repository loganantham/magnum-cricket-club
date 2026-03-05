package com.magnum.cricketclub.ui.income

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.expense.AddEditExpenseActivity
import com.magnum.cricketclub.ui.expense.ExpenseAdapter
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.utils.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomesActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var incomesRecyclerView: RecyclerView
    private lateinit var totalBalanceTextView: TextView
    private lateinit var emptyStateTextView: TextView
    private lateinit var fabAddIncome: FloatingActionButton
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var userProfileRepository: UserProfileRepository
    private val firestoreRepository = FirestoreRepository()
    private var incomeTypes: List<com.magnum.cricketclub.data.IncomeType> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incomes)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.incomes)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        userProfileRepository = UserProfileRepository(application)

        incomesRecyclerView = findViewById(R.id.incomesRecyclerView)
        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        fabAddIncome = findViewById(R.id.fabAddIncome)

        expenseAdapter = ExpenseAdapter(
            onEditClick = { expense ->
                val intent = Intent(this, AddEditExpenseActivity::class.java)
                intent.putExtra("expense_id", expense.id)
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                viewModel.deleteExpense(expense)
            },
            onShareClick = { expense, typeName ->
                shareOnWhatsApp(expense, typeName)
            },
            showActions = false // Default to false, check permissions
        )

        incomesRecyclerView.layoutManager = LinearLayoutManager(this)
        incomesRecyclerView.adapter = expenseAdapter

        fabAddIncome.setOnClickListener {
            val intent = Intent(this, AddEditExpenseActivity::class.java)
            intent.putExtra("is_income", true)
            startActivity(intent)
        }

        checkUserPermissions()

        // Observe incomes
        lifecycleScope.launch {
            viewModel.allIncomes.collectLatest { incomes ->
                expenseAdapter.submitList(incomes)
                expenseAdapter.setIncomeTypes(incomeTypes)
                emptyStateTextView.visibility = if (incomes.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe income types
        lifecycleScope.launch {
            viewModel.allIncomeTypes.collectLatest { types ->
                incomeTypes = types
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
            expenseAdapter.setShowActions(isMaintenance)
            fabAddIncome.visibility = if (isMaintenance) View.VISIBLE else View.GONE
        }
    }

    private fun shareOnWhatsApp(expense: com.magnum.cricketclub.data.Expense, typeName: String?) {
        lifecycleScope.launch {
            val groupId = viewModel.getWhatsAppGroupId()
            if (!groupId.isNullOrEmpty()) {
                WhatsAppHelper.sendExpenseUpdateToGroup(
                    this@IncomesActivity,
                    groupId,
                    expense,
                    typeName,
                    false
                )
            } else {
                Toast.makeText(this@IncomesActivity, "WhatsApp Group ID not configured in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
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
