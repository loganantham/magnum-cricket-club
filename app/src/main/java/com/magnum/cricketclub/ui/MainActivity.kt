package com.magnum.cricketclub.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.*
import com.magnum.cricketclub.data.remote.FirestoreRepository
import com.magnum.cricketclub.ui.expense.AddEditExpenseActivity
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import com.magnum.cricketclub.ui.expense.TransactionsActivity
import com.magnum.cricketclub.ui.expensetype.ExpenseTypesActivity
import com.magnum.cricketclub.ui.teamprofile.GroundFeesMaintenanceAdapter
import com.magnum.cricketclub.ui.teamprofile.TeamLedgerActivity
import com.magnum.cricketclub.data.sync.SyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var contributionLedgerRepository: ContributionLedgerRepository
    private lateinit var upcomingMatchDao: UpcomingMatchDao
    private lateinit var syncService: SyncService

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

    private lateinit var groundFeesMaintenanceCard: View
    private lateinit var groundFeesHeader: LinearLayout
    private lateinit var groundFeesContent: View
    private lateinit var groundFeesChevron: ImageView
    private lateinit var rvGroundFeesMaintenance: RecyclerView
    private lateinit var groundFeesMaintenanceAdapter: GroundFeesMaintenanceAdapter
    private lateinit var groundFeesSyncProgressBar: ProgressBar

    private var currentUserProfile: UserProfile? = null
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    private val overviewYearState = MutableStateFlow(currentYear)
    private val overviewMonthState = MutableStateFlow(currentMonth)

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
        upcomingMatchDao = db.upcomingMatchDao()
        syncService = SyncService(applicationContext)

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

        groundFeesMaintenanceCard = findViewById(R.id.groundFeesMaintenanceCard)
        groundFeesHeader = findViewById(R.id.groundFeesHeader)
        groundFeesContent = findViewById(R.id.groundFeesContent)
        groundFeesChevron = findViewById(R.id.groundFeesChevron)
        rvGroundFeesMaintenance = findViewById(R.id.rvGroundFeesMaintenance)
        groundFeesSyncProgressBar = findViewById(R.id.groundFeesSyncProgressBar)

        setupOverviewSection()
        setupExpenseManagementSection()
        setupTeamContributionLedger()
        setupGroundFeesMaintenanceSection()

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
        observeSyncStatus()
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
            groundFeesMaintenanceCard.visibility = if (isMaintenance) View.VISIBLE else View.GONE
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
        // Default to current month (index in spinner = currentMonth + 1)
        overviewMonthSpinner.setSelection(currentMonth + 1)
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
        setExpanded(teamContributionContent, teamContributionChevron, expanded = false)
        teamContributionHeader.setOnClickListener {
            val expand = teamContributionContent.visibility != View.VISIBLE
            setExpanded(teamContributionContent, teamContributionChevron, expand)
        }
        
        teamContributionContent.findViewById<View>(R.id.btnOpenTeamLedger).setOnClickListener {
            startActivity(Intent(this, TeamLedgerActivity::class.java))
        }
    }

    private fun setupGroundFeesMaintenanceSection() {
        setExpanded(groundFeesContent, groundFeesChevron, expanded = false)
        groundFeesHeader.setOnClickListener {
            val expand = groundFeesContent.visibility != View.VISIBLE
            setExpanded(groundFeesContent, groundFeesChevron, expand)
            if (expand) {
                lifecycleScope.launch {
                    syncService.syncFromFirestore()
                }
            }
        }

        rvGroundFeesMaintenance.layoutManager = LinearLayoutManager(this)
        groundFeesMaintenanceAdapter = GroundFeesMaintenanceAdapter(
            onStatusClick = { match, teamIndex -> showUpdateGroundFeeStatusDialog(match, teamIndex) },
            onDeleteMatch = { showDeleteMatchDialog(it) }
        )
        rvGroundFeesMaintenance.adapter = groundFeesMaintenanceAdapter

        lifecycleScope.launch {
            upcomingMatchDao.getAllMatches().collectLatest { matches ->
                groundFeesMaintenanceAdapter.submitList(matches)
            }
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
            ) { expenses: List<Expense>, year: Int, monthIdx: Int ->
                val cal = Calendar.getInstance()
                expenses.filter { e: Expense ->
                    cal.timeInMillis = e.date
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH)
                    (y == year) && (monthIdx == -1 || m == monthIdx)
                }
            }.collect { filtered: List<Expense> ->
                val incomeTotal = filtered.filter { it.isIncome }.sumOf { it.amount }
                val expenseTotal = filtered.filter { !it.isIncome }.sumOf { it.amount }
                val balance = incomeTotal - expenseTotal

                totalBalanceTextView.text = getString(R.string.currency_format, balance)
                totalIncomesTextView.text = getString(R.string.currency_format, incomeTotal)
                totalExpensesTextView.text = getString(R.string.currency_format, expenseTotal)
                
                totalBalanceTextView.setTextColor(if (balance >= 0) Color.BLACK else Color.RED)
            }
        }
    }

    private fun observeSyncStatus() {
        lifecycleScope.launch {
            syncService.syncStatus.collectLatest { status ->
                when (status) {
                    SyncService.SyncStatus.SYNCING -> {
                        groundFeesSyncProgressBar.visibility = View.VISIBLE
                        rvGroundFeesMaintenance.alpha = 0.5f
                    }
                    else -> {
                        groundFeesSyncProgressBar.visibility = View.GONE
                        rvGroundFeesMaintenance.alpha = 1.0f
                    }
                }
            }
        }
    }

    private fun showUpdateGroundFeeStatusDialog(match: UpcomingMatch, teamIndex: Int) {
        val options = arrayOf(getString(R.string.status_pending), getString(R.string.partial_payment), getString(R.string.status_done))
        val currentStatus = if (teamIndex == 1) match.team1FeesStatus else match.team2FeesStatus
        val teamName = if (teamIndex == 1) match.team1 else match.team2
        
        val checkedItem = when (currentStatus) {
            "DONE" -> 2
            "PARTIAL" -> 1
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("${getString(R.string.collection_status)}: $teamName")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                when (which) {
                    1 -> showPartialPaymentInput(match, teamIndex)
                    2 -> updateMatchStatus(match, teamIndex, "DONE", 0.0)
                    else -> updateMatchStatus(match, teamIndex, "PENDING", 0.0)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPartialPaymentInput(match: UpcomingMatch, teamIndex: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_partial_payment, null)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.amountEditText)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val pending = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                updateMatchStatus(match, teamIndex, "PARTIAL", pending)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateMatchStatus(match: UpcomingMatch, teamIndex: Int, status: String, pending: Double) {
        val updatedMatch = if (teamIndex == 1) {
            match.copy(team1FeesStatus = status, team1PendingAmount = pending)
        } else {
            match.copy(team2FeesStatus = status, team2PendingAmount = pending)
        }

        lifecycleScope.launch {
            upcomingMatchDao.updateMatch(updatedMatch)
            syncService.syncUpcomingMatch(updatedMatch)
            Toast.makeText(this@MainActivity, "Status updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteMatchDialog(match: UpcomingMatch) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("Are you sure you want to delete this match? This will remove it from the schedule and maintenance list.")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    upcomingMatchDao.deleteMatch(match)
                    syncService.deleteUpcomingMatchFromFirestore(match.id)
                    Toast.makeText(this@MainActivity, "Match deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun exportReportToPdf() {
        val year = overviewYearState.value
        val monthIdx = overviewMonthState.value
        val monthName = if (monthIdx == -1) "Yearly" else DateFormatSymbols().months[monthIdx]
        
        lifecycleScope.launch {
            val expenses = viewModel.allExpenses.first()
            val expenseTypes = viewModel.allExpenseTypes.first().associateBy { it.id }
            val incomeTypes = viewModel.allIncomeTypes.first().associateBy { it.id }

            val cal = Calendar.getInstance()
            val filtered = expenses.filter { e ->
                cal.timeInMillis = e.date
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                (y == year) && (monthIdx == -1 || m == monthIdx)
            }.sortedBy { it.date }

            if (filtered.isEmpty()) {
                Toast.makeText(this@MainActivity, "No data to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(this@MainActivity, "Generating PDF Report for $monthName $year...", Toast.LENGTH_LONG).show()
            generateAndSavePdf(filtered, "$monthName $year", expenseTypes, incomeTypes)
        }
    }

    private fun generateAndSavePdf(
        expenses: List<Expense>, 
        period: String,
        expenseTypes: Map<Long, ExpenseType>,
        incomeTypes: Map<Long, IncomeType>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        val textPaint = Paint().apply {
            textSize = 10f
        }

        var yPos = 40f
        canvas.drawText("Magnum Cricket Club - Expense Report", 40f, yPos, titlePaint)
        yPos += 30f
        canvas.drawText("Period: $period", 40f, yPos, headerPaint)
        yPos += 30f

        // Grouping logic for summary
        val summaryItems = mutableListOf<Pair<String, Double>>()
        val grouped = expenses.groupBy { 
            if (it.isIncome) {
                val incomeTypeId = it.incomeTypeId ?: it.expenseTypeId
                incomeTypes[incomeTypeId]?.name ?: "Unknown Income"
            } else {
                expenseTypes[it.expenseTypeId]?.name ?: "Unknown Expense"
            }
        }
        
        for ((name, items) in grouped) {
            val total = items.sumOf { if (it.isIncome) it.amount else -it.amount }
            summaryItems.add(name to total)
        }

        // Summary Section at the top
        canvas.drawText("Summary by Category", 40f, yPos, headerPaint)
        yPos += 20f
        for ((name, amount) in summaryItems) {
            canvas.drawText(name, 40f, yPos, textPaint)
            val amountStr = "₹${String.format(Locale.getDefault(), "%.2f", amount)}"
            canvas.drawText(amountStr, 200f, yPos, textPaint)
            yPos += 15f
        }
        yPos += 15f

        // Table headers
        canvas.drawText("Date", 40f, yPos, headerPaint)
        canvas.drawText("Category/Type", 110f, yPos, headerPaint)
        canvas.drawText("Description", 280f, yPos, headerPaint)
        canvas.drawText("I/E", 440f, yPos, headerPaint)
        canvas.drawText("Amount", 490f, yPos, headerPaint)
        yPos += 10f
        canvas.drawLine(40f, yPos, 550f, yPos, paint)
        yPos += 20f

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (expense in expenses) {
            if (yPos > 800) {
                break 
            }
            canvas.drawText(dateFormat.format(Date(expense.date)), 40f, yPos, textPaint)
            
            val typeName = if (expense.isIncome) {
                val incomeTypeId = expense.incomeTypeId ?: expense.expenseTypeId
                incomeTypes[incomeTypeId]?.name ?: "Unknown"
            } else {
                expenseTypes[expense.expenseTypeId]?.name ?: "Unknown"
            }
            val typeNameTruncated = if (typeName.length > 25) typeName.substring(0, 22) + "..." else typeName
            canvas.drawText(typeNameTruncated, 110f, yPos, textPaint)

            val desc = if (expense.description.length > 25) expense.description.substring(0, 22) + "..." else expense.description
            canvas.drawText(desc, 280f, yPos, textPaint)
            
            canvas.drawText(if (expense.isIncome) "Inc" else "Exp", 440f, yPos, textPaint)
            
            val amountStr = String.format(Locale.getDefault(), "%.2f", expense.amount)
            canvas.drawText(amountStr, 490f, yPos, textPaint)
            
            yPos += 20f
        }

        val totalIncome = expenses.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = expenses.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        yPos += 20f
        canvas.drawLine(40f, yPos, 550f, yPos, paint)
        yPos += 20f
        canvas.drawText("Total Income: ₹${String.format(Locale.getDefault(), "%.2f", totalIncome)}", 40f, yPos, headerPaint)
        yPos += 20f
        canvas.drawText("Total Expense: ₹${String.format(Locale.getDefault(), "%.2f", totalExpense)}", 40f, yPos, headerPaint)
        yPos += 20f
        canvas.drawText("Balance: ₹${String.format(Locale.getDefault(), "%.2f", balance)}", 40f, yPos, headerPaint)

        pdfDocument.finishPage(page)

        val fileName = "Magnum_Report_${period.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(this, "Report downloaded to Downloads folder", Toast.LENGTH_LONG).show()
                    openPdf(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                Toast.makeText(this, "Report downloaded to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                openPdf(Uri.fromFile(file))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "Open Report"))
        } catch (e: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        checkUserPermissions()
        updateBottomNavigationSelection()
    }
}
