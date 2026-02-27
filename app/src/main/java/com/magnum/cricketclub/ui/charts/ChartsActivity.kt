package com.magnum.cricketclub.ui.charts

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.R
import com.magnum.cricketclub.ui.MainActivity
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.ui.home.HomeActivity
import com.magnum.cricketclub.ui.income.IncomesActivity
import com.magnum.cricketclub.ui.incometype.IncomeTypesActivity
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChartsActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.charts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        tabLayout = findViewById(R.id.tabLayout)

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.monthly_chart)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.yearly_chart)))

        // Load initial fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MonthlyChartFragment())
            .commit()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, MonthlyChartFragment())
                            .commit()
                    }
                    1 -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, YearlyChartFragment())
                            .commit()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
            R.id.menu_home -> {
                startActivity(Intent(this, HomeActivity::class.java))
                true
            }
            R.id.menu_expenses -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            R.id.menu_incomes -> {
                startActivity(Intent(this, IncomesActivity::class.java))
                true
            }
            R.id.menu_expense_types -> {
                startActivity(Intent(this, ExpenseTypesActivity::class.java))
                true
            }
            R.id.menu_income_types -> {
                startActivity(Intent(this, IncomeTypesActivity::class.java))
                true
            }
            R.id.menu_charts -> {
                // Already on charts screen
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
