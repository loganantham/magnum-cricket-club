package com.magnum.cricketclub.ui.teamprofile

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.magnum.cricketclub.R
import com.magnum.cricketclub.data.ContributionLedgerEntry
import com.magnum.cricketclub.data.UserProfile
import com.magnum.cricketclub.databinding.ActivityTeamLedgerBinding
import com.magnum.cricketclub.ui.BaseActivity
import com.magnum.cricketclub.utils.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

class TeamLedgerActivity : BaseActivity() {
    private lateinit var binding: ActivityTeamLedgerBinding
    private lateinit var viewModel: TeamLedgerViewModel
    private var selectedUser: UserProfile? = null // null means "All"
    private val monthNames = DateFormatSymbols().months

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamLedgerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Team Ledger"

        viewModel = ViewModelProvider(this)[TeamLedgerViewModel::class.java]

        setupFilters()
        setupRecyclerView()
        observeViewModel()
        
        binding.btnNotifyAll.setOnClickListener {
            sendGroupReminder()
        }
    }

    private fun setupFilters() {
        // Year Filter
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (2024..currentYear).toList().map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, years)
        binding.yearDropdown.setAdapter(yearAdapter)
        binding.yearDropdown.setText(currentYear.toString(), false)
        binding.yearDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.setSelectedYear(years[position].toInt())
        }

        // Month Filter
        val monthsList = mutableListOf("All Months")
        monthsList.addAll(monthNames.filter { it.isNotEmpty() })
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, monthsList)
        binding.monthDropdown.setAdapter(monthAdapter)
        binding.monthDropdown.setText("All Months", false)
        binding.monthDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.setSelectedMonth(position - 1) // -1 for All
            updateDetails()
        }

        // Contributor Filter
        lifecycleScope.launch {
            viewModel.contributors.collectLatest { contributors ->
                val names = mutableListOf("All Contributors")
                names.addAll(contributors.map { it.name ?: it.email })
                val userAdapter = ArrayAdapter(this@TeamLedgerActivity, android.R.layout.simple_dropdown_item_1line, names)
                binding.userDropdown.setAdapter(userAdapter)
                
                binding.userDropdown.setText("All Contributors", false)
                selectedUser = null
                updateDetails()
                
                binding.userDropdown.setOnItemClickListener { _, _, position, _ ->
                    selectedUser = if (position == 0) null else contributors[position - 1]
                    updateDetails()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.ledgerRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.ledgerEntries.collectLatest {
                updateDetails()
                updateSummary()
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateSummary() {
        binding.summaryContainer.removeAllViews()
        val year = viewModel.selectedYear.value
        val summary = viewModel.getPendingSummary(year)
        val contributors = viewModel.contributors.value

        if (summary.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "All clear! No pending contributions for $year 🎉"
                setTextColor(Color.GRAY)
                setPadding(0, 16, 0, 0)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            binding.summaryContainer.addView(emptyText)
            return
        }

        summary.forEach { (email, pendingMonths) ->
            val contributor = contributors.find { it.email == email }
            val name = contributor?.name ?: email
            val totalAmount = pendingMonths.size * 1000
            
            val summaryRow = layoutInflater.inflate(R.layout.item_ledger_summary_row, binding.summaryContainer, false)
            summaryRow.findViewById<TextView>(R.id.nameTextView).text = name
            summaryRow.findViewById<TextView>(R.id.pendingAmountTextView).text = "₹$totalAmount"
            
            summaryRow.findViewById<MaterialButton>(R.id.remindButton).setOnClickListener {
                sendIndividualReminder(contributor, pendingMonths)
            }
            
            binding.summaryContainer.addView(summaryRow)
        }
    }

    private fun updateDetails() {
        val year = viewModel.selectedYear.value
        val monthFilter = viewModel.selectedMonth.value
        val userFilter = selectedUser
        
        val entries = viewModel.ledgerEntries.value
        val contributors = viewModel.contributors.value
        
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYearNum = Calendar.getInstance().get(Calendar.YEAR)
        
        val adapterItems = mutableListOf<DetailItem>()
        
        val usersToProcess = if (userFilter != null) listOf(userFilter) else contributors
        
        usersToProcess.forEach { user ->
            val userEntries = entries.filter { it.contributorEmail == user.email && it.year == year }
            val maxMonth = if (year < currentYearNum) 11 else currentMonth
            
            val startM = if (monthFilter == -1) 0 else monthFilter
            val endM = if (monthFilter == -1) maxMonth else monthFilter
            
            for (m in startM..endM) {
                if (m > maxMonth) break
                val entry = userEntries.find { it.monthIndex == m }
                adapterItems.add(DetailItem(user, m, monthNames[m], entry))
            }
        }

        binding.ledgerRecyclerView.adapter = LedgerAdapter(adapterItems) { item, status ->
            viewModel.updateStatus(item.user.email, year, item.monthIndex, status)
        }
    }

    private fun sendIndividualReminder(user: UserProfile?, pendingMonths: List<Int>) {
        val phone = user?.mobileNumber ?: user?.alternateMobileNumber
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "No mobile number for ${user?.name ?: "this user"}", Toast.LENGTH_SHORT).show()
            return
        }
        
        val year = viewModel.selectedYear.value
        val totalAmount = pendingMonths.size * 1000
        val monthBreakdown = pendingMonths.joinToString("\n") { "• ${monthNames[it]}: ₹1000" }
        
        val message = """
            *Magnum CC Contribution Reminder* 🏏
            
            Hi ${user?.name ?: "Member"},
            You have pending contributions for *$year*:
            
            $monthBreakdown
            
            *Total Pending: ₹$totalAmount*
            
            Please clear it at your earliest via UPI. Ignore if already paid.
            Thank you!
        """.trimIndent()
        
        WhatsAppHelper.sendCustomMessage(this, phone, message)
    }

    private fun sendGroupReminder() {
        lifecycleScope.launch {
            val year = viewModel.selectedYear.value
            val summary = viewModel.getPendingSummary(year)
            val contributors = viewModel.contributors.value
            
            if (summary.isEmpty()) {
                Toast.makeText(this@TeamLedgerActivity, "No pending contributions to notify!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val currentMonthName = if (viewModel.selectedMonth.value == -1) 
                "Yearly" 
            else 
                monthNames[viewModel.selectedMonth.value]

            var grandTotal = 0
            val summaryText = summary.entries.mapIndexed { index, entry ->
                val contributor = contributors.find { it.email == entry.key }
                val name = contributor?.name ?: entry.key
                val amount = entry.value.size * 1000
                grandTotal += amount
                val months = entry.value.joinToString(", ") { monthNames[it].take(3) }
                "${index + 1}. *$name*: ₹$amount ($months)"
            }.joinToString("\n")

            val message = """
                *Magnum CC Contribution Summary* 🏏
                *Period:* $currentMonthName $year
                
                *Pending Contributors:*
                $summaryText
                
                *Grand Total Pending: ₹$grandTotal*
                
                Requesting everyone to clear the dues to help manage club expenses efficiently.
                
                Thank you,
                Magnum Management
            """.trimIndent()

            val groupId = viewModel.getWhatsAppGroupId()
            WhatsAppHelper.shareToWhatsApp(this@TeamLedgerActivity, message, groupId)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    data class DetailItem(val user: UserProfile, val monthIndex: Int, val monthName: String, val entry: ContributionLedgerEntry?)

    inner class LedgerAdapter(private val items: List<DetailItem>, private val onStatusChange: (DetailItem, String) -> Unit) :
        androidx.recyclerview.widget.RecyclerView.Adapter<LedgerAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val monthText: TextView = view.findViewById(R.id.monthTextView)
            val amountText: TextView = view.findViewById(R.id.amountTextView)
            val statusChip: Chip = view.findViewById(R.id.statusChip)
            val remindButton: View = view.findViewById(R.id.remindButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_ledger_month, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            // Show user name if "All" is selected
            val displayText = if (selectedUser == null) "${item.user.name ?: item.user.email} - ${item.monthName}" else item.monthName
            holder.monthText.text = displayText
            
            val status = item.entry?.status ?: "Pending"
            holder.statusChip.text = status
            
            if (status == "Done") {
                holder.statusChip.setChipBackgroundColorResource(android.R.color.holo_green_light)
                holder.statusChip.setTextColor(Color.WHITE)
                holder.remindButton.visibility = View.GONE
            } else {
                holder.statusChip.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                holder.statusChip.setTextColor(Color.BLACK)
                holder.remindButton.visibility = View.VISIBLE
            }

            holder.statusChip.setOnClickListener {
                val statuses = arrayOf("Pending", "Done")
                MaterialAlertDialogBuilder(this@TeamLedgerActivity)
                    .setTitle("Update Status for ${item.monthName}")
                    .setItems(statuses) { _, which ->
                        onStatusChange(item, statuses[which])
                    }
                    .show()
            }

            holder.remindButton.setOnClickListener {
                sendIndividualReminder(item.user, listOf(item.monthIndex))
            }
        }

        override fun getItemCount() = items.size
    }
}
