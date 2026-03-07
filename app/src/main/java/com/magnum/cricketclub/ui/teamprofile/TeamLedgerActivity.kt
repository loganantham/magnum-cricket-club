package com.magnum.cricketclub.ui.teamprofile

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
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
        val allEntries = viewModel.ledgerEntries.value

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
            
            // Calculate total pending amount accurately including partial payments
            var totalAmount = 0.0
            val userEntries = allEntries.filter { it.contributorEmail == email && it.year == year }
            pendingMonths.forEach { m ->
                val entry = userEntries.find { it.monthIndex == m }
                totalAmount += entry?.pendingAmount ?: 1000.0
            }
            
            val summaryRow = layoutInflater.inflate(R.layout.item_ledger_summary_row, binding.summaryContainer, false)
            summaryRow.findViewById<TextView>(R.id.nameTextView).text = name
            summaryRow.findViewById<TextView>(R.id.pendingAmountTextView).text = "₹${totalAmount.toInt()}"
            
            summaryRow.findViewById<MaterialButton>(R.id.remindButton).setOnClickListener {
                sendIndividualReminder(contributor, pendingMonths, totalAmount.toInt())
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

        binding.ledgerRecyclerView.adapter = LedgerAdapter(adapterItems) { item, status, amount ->
            viewModel.updateStatus(item.user.email, year, item.monthIndex, status, amount)
        }
    }

    private fun sendIndividualReminder(user: UserProfile?, pendingMonths: List<Int>, totalAmount: Int? = null) {
        val phone = user?.mobileNumber ?: user?.alternateMobileNumber
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "No mobile number for ${user?.name ?: "this user"}", Toast.LENGTH_SHORT).show()
            return
        }
        
        val year = viewModel.selectedYear.value
        val finalAmount = totalAmount ?: (pendingMonths.size * 1000)
        
        val entries = viewModel.ledgerEntries.value.filter { it.contributorEmail == user?.email && it.year == year }
        val monthBreakdown = pendingMonths.joinToString("\n") { m ->
            val entry = entries.find { it.monthIndex == m }
            val amount = entry?.pendingAmount ?: 1000.0
            "• ${monthNames[m]}: ₹${amount.toInt()}"
        }
        
        val message = """
            *Magnum CC Contribution Reminder* 🏏
            
            Hi ${user?.name ?: "Member"},
            You have pending contributions for *$year*:
            
            $monthBreakdown
            
            *Total Pending: ₹$finalAmount*
            
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
            val allEntries = viewModel.ledgerEntries.value
            
            if (summary.isEmpty()) {
                Toast.makeText(this@TeamLedgerActivity, "No pending contributions to notify!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val currentMonthName = if (viewModel.selectedMonth.value == -1) 
                "Yearly" 
            else 
                monthNames[viewModel.selectedMonth.value]

            var grandTotal = 0.0
            val summaryText = summary.entries.mapIndexed { index, entry ->
                val contributor = contributors.find { it.email == entry.key }
                val name = contributor?.name ?: entry.key
                
                var userTotal = 0.0
                val userEntries = allEntries.filter { it.contributorEmail == entry.key && it.year == year }
                entry.value.forEach { m ->
                    val ledgerEntry = userEntries.find { it.monthIndex == m }
                    userTotal += ledgerEntry?.pendingAmount ?: 1000.0
                }
                
                grandTotal += userTotal
                val months = entry.value.joinToString(", ") { monthNames[it].take(3) }
                "${index + 1}. *$name*: ₹${userTotal.toInt()} ($months)"
            }.joinToString("\n")

            val message = """
                *Magnum CC Contribution Summary* 🏏
                *Period:* $currentMonthName $year
                
                *Pending Contributors:*
                $summaryText
                
                *Grand Total Pending: ₹${grandTotal.toInt()}*
                
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

    inner class LedgerAdapter(private val items: List<DetailItem>, private val onStatusChange: (DetailItem, String, Double?) -> Unit) :
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
            val pendingAmount = item.entry?.pendingAmount ?: 1000.0
            holder.statusChip.text = status
            holder.amountText.text = "₹${pendingAmount.toInt()}"
            
            when (status) {
                "Done" -> {
                    holder.statusChip.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    holder.statusChip.setTextColor(Color.WHITE)
                    holder.remindButton.visibility = View.GONE
                }
                "Partially Paid" -> {
                    holder.statusChip.setChipBackgroundColorResource(android.R.color.holo_blue_light)
                    holder.statusChip.setTextColor(Color.WHITE)
                    holder.remindButton.visibility = View.VISIBLE
                }
                else -> {
                    holder.statusChip.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    holder.statusChip.setTextColor(Color.BLACK)
                    holder.remindButton.visibility = View.VISIBLE
                }
            }

            holder.statusChip.setOnClickListener {
                val statuses = arrayOf("Pending", "Partially Paid", "Done")
                MaterialAlertDialogBuilder(this@TeamLedgerActivity)
                    .setTitle("Update Status for ${item.monthName}")
                    .setItems(statuses) { _, which ->
                        val selectedStatus = statuses[which]
                        if (selectedStatus == "Partially Paid") {
                            showAmountInputDialog(item)
                        } else {
                            onStatusChange(item, selectedStatus, null)
                        }
                    }
                    .show()
            }

            holder.remindButton.setOnClickListener {
                sendIndividualReminder(item.user, listOf(item.monthIndex), pendingAmount.toInt())
            }
        }

        private fun showAmountInputDialog(item: DetailItem) {
            val input = EditText(this@TeamLedgerActivity)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = "Enter balance amount"
            
            val container = FrameLayout(this@TeamLedgerActivity)
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(48, 16, 48, 16)
            input.layoutParams = params
            container.addView(input)

            MaterialAlertDialogBuilder(this@TeamLedgerActivity)
                .setTitle("Balance Amount")
                .setMessage("Enter the remaining amount to be paid for ${item.monthName}")
                .setView(container)
                .setPositiveButton("Update") { _, _ ->
                    val amountStr = input.text.toString()
                    if (amountStr.isNotEmpty()) {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount != null) {
                            onStatusChange(item, "Partially Paid", amount)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        override fun getItemCount() = items.size
    }
}
