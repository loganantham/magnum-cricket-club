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
            val amountStr = "₹${String.format("%.2f", amount)}"
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
                // For simplicity, we limit to one page in this fix
                // In a full implementation, we would start a new page
                break 
            }
            canvas.drawText(dateFormat.format(Date(expense.date)), 40f, yPos, textPaint)
            
            // Category Type
            val typeName = if (expense.isIncome) {
                val incomeTypeId = expense.incomeTypeId ?: expense.expenseTypeId
                incomeTypes[incomeTypeId]?.name ?: "Unknown"
            } else {
                expenseTypes[expense.expenseTypeId]?.name ?: "Unknown"
            }
            val typeNameTruncated = if (typeName.length > 25) typeName.substring(0, 22) + "..." else typeName
            canvas.drawText(typeNameTruncated, 110f, yPos, textPaint)

            // Description
            val desc = if (expense.description.length > 25) expense.description.substring(0, 22) + "..." else expense.description
            canvas.drawText(desc, 280f, yPos, textPaint)
            
            // Income/Expense Indicator
            canvas.drawText(if (expense.isIncome) "Inc" else "Exp", 440f, yPos, textPaint)
            
            // Amount
            val amountStr = String.format("%.2f", expense.amount)
            canvas.drawText(amountStr, 490f, yPos, textPaint)
            
            yPos += 20f
        }

        val totalIncome = expenses.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = expenses.filter { !it.isIncome }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        yPos += 20f
        canvas.drawLine(40f, yPos, 550f, yPos, paint)
        yPos += 20f
        canvas.drawText("Total Income: ₹${String.format("%.2f", totalIncome)}", 40f, yPos, headerPaint)
        yPos += 20f
        canvas.drawText("Total Expense: ₹${String.format("%.2f", totalExpense)}", 40f, yPos, headerPaint)
        yPos += 20f
        canvas.drawText("Balance: ₹${String.format("%.2f", balance)}", 40f, yPos, headerPaint)

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
            // No app to open PDF
        }
    }

    override fun onResume() {
        super.onResume()
        checkUserPermissions()
        updateBottomNavigationSelection()
    }
}
