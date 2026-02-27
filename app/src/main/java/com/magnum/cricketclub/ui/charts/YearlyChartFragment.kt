package com.magnum.cricketclub.ui.charts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.magnum.cricketclub.R
import com.magnum.cricketclub.ui.expense.ExpenseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class YearlyChartFragment : Fragment() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var chartTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[ExpenseViewModel::class.java]
        chartTextView = view.findViewById(R.id.chartTextView)

        lifecycleScope.launch {
            viewModel.allExpenses.collectLatest { expenses ->
                val yearlyData = expenses.groupBy {
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it.date))
                }.mapValues { (_, expenseList) ->
                    expenseList.sumOf { if (it.isIncome) it.amount else -it.amount }
                }

                val chartText = buildString {
                    append("Yearly Expense Report\n\n")
                    yearlyData.toSortedMap().forEach { (year, amount) ->
                        append("$year: ₹${String.format("%.2f", amount)}\n")
                    }
                }
                chartTextView.text = chartText
            }
        }
    }
}
