package com.magnum.cricketclub.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.data.sync.SyncService
import com.magnum.cricketclub.ui.BaseActivity
import com.magnum.cricketclub.ui.MainActivity
import com.magnum.cricketclub.ui.auth.AuthActivity
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.ui.income.IncomesActivity
import com.magnum.cricketclub.ui.incometype.IncomeTypesActivity
import com.magnum.cricketclub.ui.charts.ChartsActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var totalBalanceTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var totalIncomesTextView: TextView
    private var auth: FirebaseAuth? = null
    private val firestoreRepo = FirestoreRepository()
    private lateinit var syncService: SyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth only if available
        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            // Firebase not configured - continue in offline mode
            android.util.Log.d("HomeActivity", "Firebase not configured, working offline")
            null
        }
        
        // Check authentication only if Firebase is configured
        // If Firebase is not configured, skip auth and work in offline mode
        try {
            if (!firestoreRepo.isUserSignedIn()) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
                return
            }
        } catch (e: Exception) {
            // Firebase not configured - continue in offline mode
            android.util.Log.d("HomeActivity", "Firebase not configured, working offline")
        }
        
        setContentView(R.layout.activity_home)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.home_screen)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        
        // Initialize SyncService after context is available
        syncService = SyncService(this)
        
        // Sync data on app start
        lifecycleScope.launch {
            syncService.fullSync()
        }

        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        totalIncomesTextView = findViewById(R.id.totalIncomesTextView)

        val expensesCard: MaterialCardView = findViewById(R.id.expensesCard)
        val incomesCard: MaterialCardView = findViewById(R.id.incomesCard)
        val chartsCard: MaterialCardView = findViewById(R.id.chartsCard)

        expensesCard.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        incomesCard.setOnClickListener {
            startActivity(Intent(this, IncomesActivity::class.java))
        }

        chartsCard.setOnClickListener {
            startActivity(Intent(this, ChartsActivity::class.java))
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

        // Calculate total expenses and incomes
        lifecycleScope.launch {
            viewModel.allExpensesOnly.collectLatest { expenses ->
                val total = expenses.sumOf { it.amount }
                totalExpensesTextView.text = "₹${String.format("%.2f", total)}"
            }
        }

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
    
    fun signOut() {
        auth?.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
