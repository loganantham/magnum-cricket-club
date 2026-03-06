package com.magnum.cricketclub.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.expense.AddEditExpenseActivity
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expense.TransactionsActivity
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.ui.teamprofile.TeamLedgerActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var contributionLedgerRepository: ContributionLedgerRepository

    private lateinit var mainContentScrollView: View
    private lateinit var unauthorizedLayout: View
    private lateinit var totalBalanceTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var totalIncomesTextView: TextView
    private lateinit var fabAddExpense: FloatingActionButton

    private lateinit var overviewCard: CardView
    private lateinit var overviewHeader: View
    private lateinit var overviewContent: View
    private lateinit var overviewChevron: ImageView
    private lateinit var overviewYearSpinner: Spinner
    private lateinit var overviewMonthSpinner: Spinner
    private lateinit var btnExportPdf: MaterialButton

    private lateinit var expenseManagementCard: CardView
    private lateinit var expenseManagementHeader: View
    private lateinit var expenseManagementContent: View
    private lateinit var expenseManagementChevron: ImageView

    private lateinit var teamContributionCard: CardView
    private lateinit var teamContributionHeader: View
    private lateinit var teamContributionContent: View
    private lateinit var teamContributionChevron: ImageView

    private var currentUserProfile: UserProfile? = null
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    private val overviewYearState = MutableStateFlow(currentYear)
    private val overviewMonthState = MutableStateFlow(-1) // -1 for all year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.expenses)

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        userProfileRepository = UserProfileRepository(application)
        firestoreRepository = FirestoreRepository()
        val db = AppDatabase.getDatabase(application)
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

        setupOverviewSection()
        setupExpenseManagementSection()
        setupTeamContributionLedger()

        fabAddExpense.setOnClickListener {
            val intent = Intent(this, AddEditExpenseActivity::class.java)
            startActivity(intent)
        }

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

    private fun checkUserPermissions() {
        lifecycleScope.launch {
            val email = firestoreRepository.getCurrentUserEmail() ?: return@launch
            userProfileRepository.getUserProfile(email).collectLatest { profile ->
                currentUserProfile = profile
                updateUIBasedOnPermissions()
            }
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

    private fun setupOverviewSection() {
        setExpanded(overviewContent, overviewChevron, expanded = true)
        overviewHeader.setOnClickListener {
            val expand = overviewContent.visibility != View.VISIBLE
            setExpanded(overviewContent, overviewChevron, expand)
        }

        val years = (2024..currentYear).toList().map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        overviewYearSpinner.adapter = yearAdapter
        overviewYearSpinner.setSelection(years.indexOf(currentYear.toString()))
        overviewYearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                overviewYearState.value = years[pos].toInt()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val months = mutableListOf("All Year")
        months.addAll(DateFormatSymbols().months.filter { it.isNotEmpty() })
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        overviewMonthSpinner.adapter = monthAdapter
        overviewMonthSpinner.setSelection(0)
        overviewMonthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                overviewMonthState.value = pos - 1
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupExpenseManagementSection() {
        setExpanded(expenseManagementContent, expenseManagementChevron, expanded = false)
        expenseManagementHeader.setOnClickListener {
            val expand = expenseManagementContent.visibility != View.VISIBLE
            setExpanded(expenseManagementContent, expenseManagementChevron, expand)
        }
    }

    private fun setupTeamContributionLedger() {
        setExpanded(teamContributionContent, teamContributionChevron, expanded = true)
        teamContributionHeader.setOnClickListener {
            val expand = teamContributionContent.visibility != View.VISIBLE
            setExpanded(teamContributionContent, teamContributionChevron, expand)
        }
        
        teamContributionContent.findViewById<View>(R.id.btnOpenTeamLedger).setOnClickListener {
            startActivity(Intent(this, TeamLedgerActivity::class.java))
        }
    }

    private fun setExpanded(content: View, chevron: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                viewModel.allExpenses,
                overviewYearState,
                overviewMonthState
            ) { expenses, year, monthIdx ->
                val cal = Calendar.getInstance()
                expenses.filter { e ->
                    cal.timeInMillis = e.date
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH)
                    (y == year) && (monthIdx == -1 || m == monthIdx)
                }
            }.collect { filtered ->
                val incomeTotal = filtered.filter { it.isIncome }.sumOf { it.amount }
                val expenseTotal = filtered.filter { !it.isIncome }.sumOf { it.amount }
                val balance = incomeTotal - expenseTotal

                totalBalanceTextView.text = "₹${String.format("%.2f", balance)}"
                totalIncomesTextView.text = "₹${String.format("%.2f", incomeTotal)}"
                totalExpensesTextView.text = "₹${String.format("%.2f", expenseTotal)}"
                
                totalBalanceTextView.setTextColor(if (balance >= 0) Color.BLACK else Color.RED)
            }
        }
    }

    private fun exportReportToPdf() {
        val year = overviewYearState.value
        val monthIdx = overviewMonthState.value
        val monthName = if (monthIdx == -1) "Yearly" else DateFormatSymbols().months[monthIdx]
        Toast.makeText(this, "Generating PDF Report for $monthName $year...", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        checkUserPermissions()
        updateBottomNavigationSelection()
    }
}
