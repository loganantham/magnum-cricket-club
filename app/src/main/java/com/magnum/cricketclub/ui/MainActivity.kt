package com.magnum.cricketclub.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import com.magnum.cricketclub.data.ContributionLedgerEntry
import com.magnum.cricketclub.data.ContributionLedgerRepository
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.data.UserProfileRepository
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.config.ConfigActivity
import com.magnum.cricketclub.ui.expense.AddEditExpenseActivity
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expense.TransactionsActivity
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.utils.WhatsAppHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

class MainActivity : BaseActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var totalBalanceTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var totalIncomesTextView: TextView
    private lateinit var fabAddExpense: FloatingActionButton
    
    private lateinit var overviewHeader: LinearLayout
    private lateinit var overviewContent: LinearLayout
    private lateinit var overviewChevron: ImageView
    private lateinit var overviewCard: View
    private lateinit var overviewYearSpinner: Spinner
    private lateinit var overviewMonthSpinner: Spinner
    private lateinit var btnExportPdf: MaterialButton
    
    private lateinit var expenseManagementHeader: LinearLayout
    private lateinit var expenseManagementContent: LinearLayout
    private lateinit var expenseManagementChevron: ImageView
    private lateinit var expenseManagementCard: View
    
    private lateinit var teamContributionHeader: LinearLayout
    private lateinit var teamContributionContent: LinearLayout
    private lateinit var teamContributionChevron: ImageView
    private lateinit var teamContributionCard: View
    
    private lateinit var yearFilterSpinner: Spinner
    private lateinit var contributionTablesContainer: LinearLayout
    private lateinit var teamContributionEmptyStateTextView: TextView
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var contributionLedgerRepository: ContributionLedgerRepository
    private val firestoreRepository = FirestoreRepository()
    private var financeContributors: List<UserProfile> = emptyList()
    
    private val currentCal = Calendar.getInstance()
    private val currentYear = currentCal.get(Calendar.YEAR)
    private val currentMonth = currentCal.get(Calendar.MONTH)
    
    private var selectedContributionYear: Int = currentYear
    
    private val overviewYearState = MutableStateFlow(currentYear)
    private val overviewMonthState = MutableStateFlow(currentMonth)
    
    private lateinit var mainContentScrollView: View
    private lateinit var unauthorizedLayout: View
    private var currentUserProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.expenses)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        userProfileRepository = UserProfileRepository(application)
        val db = com.magnum.cricketclub.data.AppDatabase.getDatabase(application)
        contributionLedgerRepository = ContributionLedgerRepository(db.contributionLedgerDao())

        mainContentScrollView = findViewById(R.id.mainContentScrollView)
        unauthorizedLayout = findViewById(R.id.unauthorizedLayout)
        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        totalIncomesTextView = findViewById(R.id.totalIncomesTextView)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        
        overviewCard = findViewById(R.id.overviewCard)
        overviewHeader = findViewById(R.id.overviewHeader)
        overviewContent = findViewById(R.id.overviewContent)
        overviewChevron = findViewById(R.id.overviewChevron)
        overviewYearSpinner = findViewById(R.id.overviewYearSpinner)
        overviewMonthSpinner = findViewById(R.id.overviewMonthSpinner)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        
        expenseManagementCard = findViewById(R.id.expenseManagementCard)
        expenseManagementHeader = findViewById(R.id.expenseManagementHeader)
        expenseManagementContent = findViewById(R.id.expenseManagementContent)
        expenseManagementChevron = findViewById(R.id.expenseManagementChevron)
        
        teamContributionCard = findViewById(R.id.teamContributionCard)
        teamContributionHeader = findViewById(R.id.teamContributionHeader)
        teamContributionContent = findViewById(R.id.teamContributionContent)
        teamContributionChevron = findViewById(R.id.teamContributionChevron)
        
        yearFilterSpinner = findViewById(R.id.yearFilterSpinner)
        contributionTablesContainer = findViewById(R.id.contributionTablesContainer)
        teamContributionEmptyStateTextView = findViewById(R.id.teamContributionEmptyStateTextView)
        
        setupOverviewSection()
        setupExpenseManagementSection()
        setupTeamContributionLedger()

        fabAddExpense.setOnClickListener {
            val intent = Intent(this, AddEditExpenseActivity::class.java)
            startActivity(intent)
        }

        // Overview click listeners - Updated to open TransactionsActivity
        findViewById<View>(R.id.balanceCard).setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra(TransactionsActivity.EXTRA_TYPE, TransactionsActivity.TYPE_ALL)
            intent.putExtra(TransactionsActivity.EXTRA_YEAR, overviewYearState.value)
            intent.putExtra(TransactionsActivity.EXTRA_MONTH, overviewMonthState.value)
            startActivity(intent)
        }
        
        findViewById<View>(R.id.expensesCard).setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra(TransactionsActivity.EXTRA_TYPE, TransactionsActivity.TYPE_EXPENSE)
            intent.putExtra(TransactionsActivity.EXTRA_YEAR, overviewYearState.value)
            intent.putExtra(TransactionsActivity.EXTRA_MONTH, overviewMonthState.value)
            startActivity(intent)
        }
        
        findViewById<View>(R.id.incomesCard).setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra(TransactionsActivity.EXTRA_TYPE, TransactionsActivity.TYPE_INCOME)
            intent.putExtra(TransactionsActivity.EXTRA_YEAR, overviewYearState.value)
            intent.putExtra(TransactionsActivity.EXTRA_MONTH, overviewMonthState.value)
            startActivity(intent)
        }

        findViewById<View>(R.id.chartsCard).setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.charts.ChartsActivity::class.java))
        }
        btnExportPdf.setOnClickListener {
            exportReportToPdf()
        }

        // Expense Management sub-items
        findViewById<View>(R.id.expenseTypesCard).setOnClickListener {
            startActivity(Intent(this, ExpenseTypesActivity::class.java))
        }
        findViewById<View>(R.id.incomeTypesCard).setOnClickListener {
            startActivity(Intent(this, com.magnum.cricketclub.ui.incometype.IncomeTypesActivity::class.java))
        }

        observeData()
        setupBottomNavigation()
        checkUserPermissions()
    }

    private fun exportReportToPdf() {
        val year = overviewYearState.value
        val monthIdx = overviewMonthState.value
        val monthName = if (monthIdx == -1) "Yearly" else DateFormatSymbols().months[monthIdx]
        Toast.makeText(this, "Generating PDF Report for $monthName $year...", Toast.LENGTH_LONG).show()
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                viewModel.allExpenses,
                overviewYearState,
                overviewMonthState
            ) { expenses, year, monthIdx ->
                val filtered = expenses.filter { expense ->
                    val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
                    val yearMatch = cal.get(Calendar.YEAR) == year
                    val monthMatch = monthIdx == -1 || cal.get(Calendar.MONTH) == monthIdx
                    yearMatch && monthMatch
                }
                val exp = filtered.filter { !it.isIncome }.sumOf { it.amount }
                val inc = filtered.filter { it.isIncome }.sumOf { it.amount }
                Pair(exp, inc)
            }.collectLatest { (totalExp, totalInc) ->
                totalExpensesTextView.text = "₹${String.format("%.2f", totalExp)}"
                totalIncomesTextView.text = "₹${String.format("%.2f", totalInc)}"
                val balance = totalInc - totalExp
                totalBalanceTextView.text = "₹${String.format("%.2f", balance)}"
                totalBalanceTextView.setTextColor(if (balance >= 0) getColor(R.color.green) else getColor(R.color.red))
            }
        }
    }

    private fun setupOverviewSection() {
        setExpanded(overviewContent, overviewChevron, expanded = true)
        overviewHeader.setOnClickListener {
            val expand = overviewContent.visibility != View.VISIBLE
            setExpanded(overviewContent, overviewChevron, expand)
        }
        
        val startYear = 2024
        val years = (startYear..currentYear).toList()
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        overviewYearSpinner.adapter = yearAdapter
        overviewYearSpinner.setSelection(years.indexOf(overviewYearState.value).coerceAtLeast(0))
        
        overviewYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = years[position]
                overviewYearState.value = selectedYear
                updateOverviewMonthSpinner(selectedYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        updateOverviewMonthSpinner(overviewYearState.value)
    }

    private fun updateOverviewMonthSpinner(selectedYear: Int) {
        val monthNames = DateFormatSymbols().months.filter { it.isNotEmpty() }
        val availableMonths = if (selectedYear >= currentYear) {
            monthNames.take(currentMonth + 1)
        } else {
            monthNames
        }
        
        val monthsWithAll = mutableListOf("All Months")
        monthsWithAll.addAll(availableMonths)
        
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monthsWithAll)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        overviewMonthSpinner.adapter = monthAdapter
        
        // Find index for current state
        val currentStateMonth = overviewMonthState.value
        val selectionIndex = if (currentStateMonth == -1) {
            0
        } else {
            val mName = if (currentStateMonth in 0..11) monthNames[currentStateMonth] else ""
            val idx = monthsWithAll.indexOf(mName)
            if (idx != -1) idx else {
                overviewMonthState.value = availableMonths.size - 1
                availableMonths.size
            }
        }
        overviewMonthSpinner.setSelection(selectionIndex)
        
        overviewMonthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                overviewMonthState.value = if (position == 0) -1 else {
                    val selectedMonthName = monthsWithAll[position]
                    monthNames.indexOf(selectedMonthName)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            currentUserProfile = profile
            updateUIBasedOnPermissions()
            loadFinanceContributors()
        }
    }

    private fun updateUIBasedOnPermissions() {
        val profile = currentUserProfile ?: return
        val isMaintenance = profile.isFinanceMaintenance()
        val isContributor = profile.isFinanceContributor()

        if (!isMaintenance && !isContributor) {
            mainContentScrollView.visibility = View.GONE
            unauthorizedLayout.visibility = View.VISIBLE
            fabAddExpense.visibility = View.GONE
        } else {
            mainContentScrollView.visibility = View.VISIBLE
            unauthorizedLayout.visibility = View.GONE
            expenseManagementCard.visibility = if (isMaintenance) View.VISIBLE else View.GONE
            teamContributionCard.visibility = if (isMaintenance) View.VISIBLE else View.GONE
            fabAddExpense.visibility = if (isMaintenance) View.VISIBLE else View.GONE
            overviewCard.visibility = View.VISIBLE
        }
    }

    private fun setupExpenseManagementSection() {
        setExpanded(expenseManagementContent, expenseManagementChevron, expanded = false)
        expenseManagementHeader.setOnClickListener {
            val expand = expenseManagementContent.visibility != View.VISIBLE
            setExpanded(expenseManagementContent, expenseManagementChevron, expand)
        }
    }

    override fun onResume() {
        super.onResume()
        checkUserPermissions()
    }

    private fun setupTeamContributionLedger() {
        setExpanded(teamContributionContent, teamContributionChevron, expanded = false)
        teamContributionHeader.setOnClickListener {
            val expand = teamContributionContent.visibility != View.VISIBLE
            setExpanded(teamContributionContent, teamContributionChevron, expand)
        }
        
        val startYear = 2024
        val years = (startYear..currentYear).toList()
        if (years.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            yearFilterSpinner.adapter = adapter
            yearFilterSpinner.setSelection(years.indexOf(selectedContributionYear).coerceAtLeast(0))
            yearFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedContributionYear = years[pos]
                    renderContributionTables()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun loadFinanceContributors() {
        lifecycleScope.launch {
            try {
                val firebaseProfiles = try { firestoreRepository.downloadAllUserProfiles() } catch (e: Exception) { emptyList() }
                val localProfiles = try { userProfileRepository.getAllUserProfiles() } catch (e: Exception) { emptyList() }
                financeContributors = (if (firebaseProfiles.isNotEmpty()) firebaseProfiles else localProfiles).filter { it.canManageFinance() }
                renderContributionTables()
            } catch (e: Exception) { /* log */ }
        }
    }

    private fun renderContributionTables() {
        contributionTablesContainer.removeAllViews()
        if (financeContributors.isEmpty()) {
            teamContributionEmptyStateTextView.visibility = View.VISIBLE
            return
        }
        teamContributionEmptyStateTextView.visibility = View.GONE
        val monthNames = DateFormatSymbols().months

        val data = financeContributors.map { c ->
            c to kotlinx.coroutines.runBlocking { contributionLedgerRepository.getEntriesForContributorAndYear(c.email, selectedContributionYear) }
        }

        val summaryCard = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 }
            radius = 12f; strokeWidth = 2; strokeColor = getColor(R.color.primary_color)
        }
        val summaryLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16) }
        summaryLayout.addView(TextView(this).apply { text = "Pending Summary"; textStyle(16f, true) })
        val summaryTable = TableLayout(this).apply { isStretchAllColumns = true }
        val header = TableRow(this).apply { setBackgroundColor(getColor(R.color.primary_color)) }
        fun h(t: String) = TextView(this).apply { text = t; setTextColor(Color.WHITE); textStyle(12f, true); setPadding(4, 4, 4, 4) }
        header.addView(h("Name")); header.addView(h("Total")); header.addView(h("Months")); header.addView(h("Action"))
        summaryTable.addView(header)

        val monthsToDisplay = if (selectedContributionYear >= currentYear) currentMonth else 11

        var found = false
        data.forEach { (c, e) ->
            var tp = 0.0; val pm = mutableListOf<String>()
            for (m in 0..monthsToDisplay) {
                if ((e.firstOrNull { it.monthIndex == m }?.status ?: getString(R.string.status_pending)) != getString(R.string.status_done)) { tp += 1000.0; pm.add(monthNames[m].take(3)) }
            }
            if (tp > 0) {
                found = true
                val row = TableRow(this).apply { setPadding(8, 8, 8, 8) }
                row.addView(TextView(this).apply { text = c.name ?: c.email; textSize = 11f })
                row.addView(TextView(this).apply { text = "₹$tp"; textStyle(11f, true); setTextColor(getColor(R.color.red)) })
                row.addView(TextView(this).apply { text = pm.joinToString(","); textSize = 10f })
                row.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "Remind"; textSize = 10f; setPadding(4, 2, 4, 2); minWidth = 0; minHeight = 0
                    setOnClickListener {
                        val p = c.mobileNumber ?: c.alternateMobileNumber
                        if (!p.isNullOrBlank()) {
                            WhatsAppHelper.sendCustomMessage(this@MainActivity, p, "Magnum reminder: ₹${String.format("%.2f", tp)} pending for $selectedContributionYear")
                        }
                    }
                })
                summaryTable.addView(row)
            }
        }
        if (!found) summaryLayout.addView(TextView(this).apply { text = "All clear! 🎉"; setTextColor(getColor(R.color.green)); gravity = android.view.Gravity.CENTER; setPadding(16,16,16,16) })
        else summaryLayout.addView(summaryTable)
        summaryCard.addView(summaryLayout); contributionTablesContainer.addView(summaryCard)

        data.forEach { (c, e) ->
            val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 0) }
            container.addView(TextView(this).apply { text = c.name ?: c.email; textStyle(16f, true) })
            val tc = com.google.android.material.card.MaterialCardView(this).apply { radius = 12f }
            val tl = TableLayout(this).apply { isStretchAllColumns = true }
            for (m in 0..monthsToDisplay) {
                val row = TableRow(this).apply { setPadding(4, 8, 4, 8) }
                row.addView(TextView(this).apply { text = monthNames[m]; textSize = 11f })
                row.addView(TextView(this).apply { text = "₹1000"; textSize = 11f })
                val s = e.firstOrNull { it.monthIndex == m }?.status ?: "Pending"
                row.addView(TextView(this).apply { text = s; textSize = 11f; setTextColor(getColor(if (s == "Done") R.color.green else R.color.status_pending_color)) })
                tl.addView(row)
            }
            tc.addView(tl); container.addView(tc); contributionTablesContainer.addView(container)
        }
    }

    private fun TextView.textStyle(size: Float, bold: Boolean) {
        textSize = size
        if (bold) setTypeface(null, Typeface.BOLD)
    }
}
