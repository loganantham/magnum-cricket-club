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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.magnum.cricketclub.ui.expense.ExpenseAdapter
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.utils.WhatsAppHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

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
    private lateinit var teamContributionHeader: LinearLayout
    private lateinit var teamContributionContent: LinearLayout
    private lateinit var teamContributionChevron: ImageView
    private lateinit var yearFilterSpinner: Spinner
    private lateinit var monthFilterSpinner: Spinner
    private lateinit var contributionTablesContainer: LinearLayout
    private lateinit var teamContributionEmptyStateTextView: TextView
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var contributionLedgerRepository: ContributionLedgerRepository
    private val firestoreRepository = FirestoreRepository()
    private var financeContributors: List<UserProfile> = emptyList()
    private var selectedContributionYear: Int = 2026
    private var selectedContributionMonthIndex: Int = -1 // -1 for All Months

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.expenses)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        userProfileRepository = UserProfileRepository(application)
        val db = com.magnum.cricketclub.data.AppDatabase.getDatabase(application)
        contributionLedgerRepository = ContributionLedgerRepository(db.contributionLedgerDao())

        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        totalIncomesTextView = findViewById(R.id.totalIncomesTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        teamContributionHeader = findViewById(R.id.teamContributionHeader)
        teamContributionContent = findViewById(R.id.teamContributionContent)
        teamContributionChevron = findViewById(R.id.teamContributionChevron)
        yearFilterSpinner = findViewById(R.id.yearFilterSpinner)
        contributionTablesContainer = findViewById(R.id.contributionTablesContainer)
        teamContributionEmptyStateTextView = findViewById(R.id.teamContributionEmptyStateTextView)
        
        // Dynamically add month spinner to the filters layout
        addMonthFilterSpinner()

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

        setupTeamContributionLedger()

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
        
        // Setup bottom navigation from BaseActivity
        setupBottomNavigation()

        // Load finance contributors for the ledger
        loadFinanceContributors()
    }

    private fun addMonthFilterSpinner() {
        val filterLayout = yearFilterSpinner.parent as LinearLayout
        
        val monthLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
            text = "Month:"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, theme))
        }
        
        monthFilterSpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
        }
        
        val monthNames = DateFormatSymbols().months.filter { it.isNotEmpty() }.toMutableList()
        monthNames.add(0, "All Months")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monthNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthFilterSpinner.adapter = adapter
        
        filterLayout.addView(monthLabel)
        filterLayout.addView(monthFilterSpinner)
        
        monthFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedContributionMonthIndex = if (position == 0) -1 else position - 1
                renderContributionTables()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload contributors when activity resumes to pick up changes from Me section
        loadFinanceContributors()
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

    private fun setupTeamContributionLedger() {
        setExpanded(teamContributionContent, teamContributionChevron, expanded = false)

        teamContributionHeader.setOnClickListener {
            val expand = teamContributionContent.visibility != View.VISIBLE
            setExpanded(teamContributionContent, teamContributionChevron, expand)
        }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val startYear = 2026
        val years = (startYear..currentYear).toList()

        if (years.isNotEmpty()) {
            selectedContributionYear = years.last()
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                years
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            yearFilterSpinner.adapter = adapter
            yearFilterSpinner.setSelection(years.indexOf(selectedContributionYear))

            yearFilterSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedContributionYear = years[position]
                    renderContributionTables()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    // No-op
                }
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
                val firebaseProfiles = try {
                    firestoreRepository.downloadAllUserProfiles()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error loading profiles from Firebase", e)
                    emptyList()
                }

                val localProfiles = try {
                    userProfileRepository.getAllUserProfiles()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error loading profiles from local DB", e)
                    emptyList()
                }

                val isFinanceContributor = { profile: UserProfile ->
                    profile.canManageFinance()
                }

                financeContributors = if (firebaseProfiles.isNotEmpty()) {
                    firebaseProfiles.filter { isFinanceContributor(it) }
                } else {
                    localProfiles.filter { isFinanceContributor(it) }
                }

                renderContributionTables()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading finance contributors", e)
            }
        }
    }

    private fun renderContributionTables() {
        contributionTablesContainer.removeAllViews()

        if (financeContributors.isEmpty()) {
            teamContributionEmptyStateTextView.visibility = View.VISIBLE
            contributionTablesContainer.visibility = View.GONE
            return
        }

        teamContributionEmptyStateTextView.visibility = View.GONE
        contributionTablesContainer.visibility = View.VISIBLE

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonthIndex = calendar.get(Calendar.MONTH) // 0-based
        val startYear = 2026

        if (selectedContributionYear < startYear || selectedContributionYear > currentYear) {
            return
        }

        val startMonthIndex = if (selectedContributionMonthIndex == -1) 0 else selectedContributionMonthIndex
        val endMonthIndex = if (selectedContributionMonthIndex == -1) {
            if (selectedContributionYear == currentYear) currentMonthIndex else 11
        } else {
            selectedContributionMonthIndex
        }

        val monthNames = DateFormatSymbols().months

        // Collect all data first for summary aggregation
        val allContributorData = financeContributors.map { contributor ->
            val existingEntries = kotlinx.coroutines.runBlocking {
                contributionLedgerRepository.getEntriesForContributorAndYear(
                    contributor.email,
                    selectedContributionYear
                )
            }
            contributor to existingEntries
        }

        // --- Overall Summary Card ---
        val summaryCard = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            setCardBackgroundColor(Color.WHITE)
            radius = 12f
            strokeWidth = 2
            strokeColor = resources.getColor(R.color.primary_color, theme)
        }

        val summaryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val summaryTitle = TextView(this).apply {
            text = "Pending Summary (${if (selectedContributionMonthIndex == -1) "All Months" else monthNames[selectedContributionMonthIndex]})"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, theme))
            setPadding(0, 0, 0, 12)
        }
        summaryLayout.addView(summaryTitle)

        val summaryTable = TableLayout(this).apply {
            isStretchAllColumns = true
        }

        // Summary Header
        val summaryHeader = TableRow(this).apply {
            setBackgroundColor(resources.getColor(R.color.primary_color, theme))
            setPadding(8, 8, 8, 8)
        }
        
        fun createHeaderCell(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setPadding(4, 4, 4, 4)
        }
        
        summaryHeader.addView(createHeaderCell("Name"))
        summaryHeader.addView(createHeaderCell("Total Pending"))
        summaryHeader.addView(createHeaderCell("Months"))
        summaryHeader.addView(createHeaderCell("Action"))
        summaryTable.addView(summaryHeader)

        var entriesFound = false
        allContributorData.forEach { (contributor, entries) ->
            var totalPending = 0.0
            val pendingMonthNames = mutableListOf<String>()

            for (m in startMonthIndex..endMonthIndex) {
                val entry = entries.firstOrNull { it.monthIndex == m }
                val status = entry?.status ?: getString(R.string.status_pending)
                if (status != getString(R.string.status_done)) {
                    totalPending += 1000.0 // Standard amount
                    pendingMonthNames.add(monthNames[m].take(3))
                }
            }

            if (totalPending > 0) {
                entriesFound = true
                val row = TableRow(this).apply { setPadding(8, 8, 8, 8) }
                
                row.addView(TextView(this).apply {
                    text = contributor.name ?: contributor.email
                    textSize = 11f
                })
                
                row.addView(TextView(this).apply {
                    text = "₹${String.format("%.2f", totalPending)}"
                    textSize = 11f
                    setTextColor(resources.getColor(R.color.red, theme))
                    setTypeface(null, Typeface.BOLD)
                })

                row.addView(TextView(this).apply {
                    text = pendingMonthNames.joinToString(",")
                    textSize = 10f
                    setTextColor(resources.getColor(R.color.text_hint, theme))
                })

                val sendBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = "Remind"
                    textSize = 10f
                    setPadding(4, 2, 4, 2)
                    minimumWidth = 0
                    minimumHeight = 0
                    val phone = contributor.mobileNumber ?: contributor.alternateMobileNumber
                    isEnabled = !phone.isNullOrBlank()
                    setOnClickListener {
                        val fullPendingMonthNames = mutableListOf<String>()
                        for (m in startMonthIndex..endMonthIndex) {
                            val entry = entries.firstOrNull { it.monthIndex == m }
                            val status = entry?.status ?: getString(R.string.status_pending)
                            if (status != getString(R.string.status_done)) fullPendingMonthNames.add(monthNames[m])
                        }
                        
                        val msg = """
                            🏏 Magnum Cricket Club Reminder
                            
                            Hi ${contributor.name ?: "Member"},
                            
                            Friendly reminder for your pending contribution for $selectedContributionYear.
                            
                            Total Pending: ₹${String.format("%.2f", totalPending)}
                            Months: ${fullPendingMonthNames.joinToString(", ")}
                            
                            Please clear the dues at your earliest convenience. 
                            If already paid, please ignore or update the app.
                            
                            Thank you!
                        """.trimIndent()
                        WhatsAppHelper.sendCustomMessage(this@MainActivity, phone!!, msg)
                    }
                }
                row.addView(sendBtn)
                summaryTable.addView(row)
            }
        }

        if (!entriesFound) {
            val emptyRow = TextView(this).apply {
                text = "All contributions completed for this period! 🎉"
                textSize = 14f
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
                setTextColor(resources.getColor(R.color.green, theme))
            }
            summaryLayout.addView(emptyRow)
        } else {
            summaryLayout.addView(summaryTable)
        }
        
        summaryCard.addView(summaryLayout)
        contributionTablesContainer.addView(summaryCard)

        // --- Detailed Per-Contributor Tables ---
        financeContributors.forEach { contributor ->
            val contributorEmail = contributor.email

            val existingEntries = kotlinx.coroutines.runBlocking {
                contributionLedgerRepository.getEntriesForContributorAndYear(
                    contributorEmail,
                    selectedContributionYear
                )
            }

            val contributorContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                orientation = LinearLayout.VERTICAL
            }

            val nameTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = contributor.name ?: contributor.email
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(R.color.text_primary, theme))
                setPadding(0, 16, 0, 8)
            }
            contributorContainer.addView(nameTextView)

            val tableCard = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                radius = 12f
            }

            val tableLayout = TableLayout(this).apply {
                layoutParams = TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                isStretchAllColumns = true
            }

            val headerRow = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(resources.getColor(R.color.primary_color, theme))
                setPadding(8, 8, 8, 8)
            }

            fun headerCell(text: String, weight: Float): TextView {
                return TextView(this).apply {
                    layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
                    this.text = text
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(8, 4, 8, 4)
                }
            }

            headerRow.addView(headerCell(getString(R.string.contribution_year), 1f))
            headerRow.addView(headerCell(getString(R.string.contribution_month), 1f))
            headerRow.addView(headerCell(getString(R.string.contribution_amount), 1f))
            headerRow.addView(headerCell(getString(R.string.contribution_pending_amount), 1f))
            headerRow.addView(headerCell(getString(R.string.contribution_status), 1f))
            headerRow.addView(headerCell(getString(R.string.send_reminder), 1f))

            tableLayout.addView(headerRow)

            val contributionAmount = 1000.0

            for (monthIndex in startMonthIndex..endMonthIndex) {
                val monthName = monthNames[monthIndex]

                val existingForMonth: ContributionLedgerEntry? =
                    existingEntries.firstOrNull { it.monthIndex == monthIndex }

                val dataRow = TableRow(this).apply {
                    layoutParams = TableRow.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(8, 8, 8, 8)
                }

                fun dataCell(text: String, weight: Float): TextView {
                    return TextView(this).apply {
                        layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
                        this.text = text
                        textSize = 12f
                        setPadding(8, 4, 8, 4)
                    }
                }

                val yearCell = dataCell(selectedContributionYear.toString(), 1f)
                val monthCell = dataCell(monthName, 1f)
                val amountCell = dataCell("₹${String.format("%.2f", contributionAmount)}", 1f)

                var currentPendingAmount = existingForMonth?.pendingAmount ?: 0.0
                val pendingAmountCell = dataCell(
                    "₹${String.format("%.2f", currentPendingAmount)}",
                    1f
                )

                pendingAmountCell.setOnClickListener {
                    val editText = android.widget.EditText(this).apply {
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        setText(String.format("%.2f", currentPendingAmount))
                        setSelection(text.length)
                    }

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.contribution_pending_amount))
                        .setView(editText)
                        .setPositiveButton(R.string.save) { _, _ ->
                            val textValue = editText.text?.toString()?.trim().orEmpty()
                            val newAmount = textValue.toDoubleOrNull()
                            if (newAmount != null) {
                                currentPendingAmount = newAmount
                                pendingAmountCell.text = "₹${String.format("%.2f", currentPendingAmount)}"

                                lifecycleScope.launch {
                                    contributionLedgerRepository.upsertEntry(
                                        email = contributorEmail,
                                        year = selectedContributionYear,
                                        monthIndex = monthIndex,
                                        status = existingForMonth?.status
                                            ?: getString(R.string.status_pending),
                                        pendingAmount = currentPendingAmount
                                    )
                                }
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }

                var currentStatus = existingForMonth?.status ?: getString(R.string.status_pending)

                val statusTextView = TextView(this).apply {
                    layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = currentStatus
                    textSize = 12f
                    setPadding(8, 4, 8, 4)
                    val colorRes = when (currentStatus) {
                        getString(R.string.status_done) -> R.color.green
                        getString(R.string.status_partial) -> R.color.status_partial_color
                        else -> R.color.status_pending_color
                    }
                    setTextColor(resources.getColor(colorRes, theme))
                }

                statusTextView.setOnClickListener {
                    val statuses = arrayOf(
                        getString(R.string.status_pending),
                        getString(R.string.status_done),
                        getString(R.string.status_partial)
                    )

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.contribution_status))
                        .setItems(statuses) { _, which ->
                            val selected = statuses[which]
                            currentStatus = selected
                            statusTextView.text = selected
                            val colorRes = when (selected) {
                                getString(R.string.status_done) -> R.color.green
                                getString(R.string.status_partial) -> R.color.status_partial_color
                                else -> R.color.status_pending_color
                            }
                            statusTextView.setTextColor(resources.getColor(colorRes, theme))

                            lifecycleScope.launch {
                                contributionLedgerRepository.upsertEntry(
                                    email = contributorEmail,
                                    year = selectedContributionYear,
                                    monthIndex = monthIndex,
                                    status = currentStatus,
                                    pendingAmount = currentPendingAmount
                                )
                                // Force table re-render to update reminder button state
                                renderContributionTables()
                            }
                        }
                        .show()
                }

                val reminderButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    text = getString(R.string.send_reminder)
                    textSize = 12f
                    minimumWidth = 0
                    setPadding(8, 4, 8, 4)
                }

                val phoneNumber = contributor.mobileNumber ?: contributor.alternateMobileNumber

                // Reminder button enabled for pending or partial status, and if phone number exists
                val isReminderEnabled = (currentStatus == getString(R.string.status_pending) || 
                                        currentStatus == getString(R.string.status_partial)) && 
                                       !phoneNumber.isNullOrBlank()
                
                reminderButton.isEnabled = isReminderEnabled
                
                if (isReminderEnabled) {
                    reminderButton.setOnClickListener {
                        WhatsAppHelper.sendContributionReminder(
                            context = this,
                            phoneNumber = phoneNumber!!,
                            contributorName = contributor.name ?: contributor.email,
                            year = selectedContributionYear,
                            monthName = monthName,
                            amount = contributionAmount,
                            status = currentStatus
                        )
                    }
                }

                dataRow.addView(yearCell)
                dataRow.addView(monthCell)
                dataRow.addView(amountCell)
                dataRow.addView(pendingAmountCell)
                dataRow.addView(statusTextView)
                dataRow.addView(reminderButton)

                tableLayout.addView(dataRow)
            }

            tableCard.addView(tableLayout)
            contributorContainer.addView(tableCard)
            contributionTablesContainer.addView(contributorContainer)
        }
    }
}
