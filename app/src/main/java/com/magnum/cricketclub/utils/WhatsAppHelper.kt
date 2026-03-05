package com.magnum.cricketclub.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.magnum.cricketclub.data.Expense
import com.magnum.cricketclub.data.ExpenseType
import java.text.SimpleDateFormat
import java.util.*

object WhatsAppHelper {
    fun sendExpenseUpdate(context: Context, expense: Expense, expenseType: ExpenseType?, isNew: Boolean) {
        try {
            val action = if (isNew) "Added" else "Updated"
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(expense.date))
            val type = if (expense.isIncome) "Income" else "Expense"
            val sign = if (expense.isIncome) "+" else "-"
            
            val message = """
                🏏 Cricket Team Expense Update
                
                $action: $type
                Type: ${expenseType?.name ?: "Unknown"}
                Amount: $sign₹${String.format("%.2f", expense.amount)}
                ${if (expense.description.isNotEmpty()) "Description: ${expense.description}\n" else ""}
                Date: $dateStr
            """.trimIndent()
            
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.setPackage("com.whatsapp")
            intent.putExtra(Intent.EXTRA_TEXT, message)
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: Try to open WhatsApp with the message
                val uri = Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun sendExpenseUpdateToGroup(context: Context, groupId: String, expense: Expense, expenseType: ExpenseType?, isNew: Boolean) {
        try {
            val action = if (isNew) "Added" else "Updated"
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(expense.date))
            val type = if (expense.isIncome) "Income" else "Expense"
            val sign = if (expense.isIncome) "+" else "-"
            
            val message = """
                🏏 Cricket Team Expense Update
                
                $action: $type
                Type: ${expenseType?.name ?: "Unknown"}
                Amount: $sign₹${String.format("%.2f", expense.amount)}
                ${if (expense.description.isNotEmpty()) "Description: ${expense.description}\n" else ""}
                Date: $dateStr
            """.trimIndent()
            
            // Try to open WhatsApp with group ID
            val uri = Uri.parse("https://chat.whatsapp.com/$groupId")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.putExtra(Intent.EXTRA_TEXT, message)
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: Open WhatsApp and let user select group
                val fallbackIntent = Intent(Intent.ACTION_SEND)
                fallbackIntent.type = "text/plain"
                fallbackIntent.setPackage("com.whatsapp")
                fallbackIntent.putExtra(Intent.EXTRA_TEXT, message)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendContributionReminder(
        context: Context,
        phoneNumber: String,
        contributorName: String?,
        year: Int,
        monthName: String,
        amount: Double,
        status: String
    ) {
        try {
            val greetingName = contributorName?.takeIf { it.isNotBlank() } ?: "there"
            val messageTitle = context.getString(com.magnum.cricketclub.R.string.contribution_reminder_title)
            val messageStatus = status.ifBlank { context.getString(com.magnum.cricketclub.R.string.status_pending) }

            val message = """
                $messageTitle

                Hi $greetingName,

                This is a gentle reminder for your team contribution for $monthName $year.

                Amount: ₹${String.format("%.2f", amount)}
                Status: $messageStatus

                Please complete the payment and update the status once done.

                Thank you for supporting Magnum Cricket Club! 🏏
            """.trimIndent()

            val cleanNumber = phoneNumber.trim()
            if (cleanNumber.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(com.magnum.cricketclub.R.string.no_mobile_for_contributor),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    context.getString(com.magnum.cricketclub.R.string.whatsapp_not_installed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(com.magnum.cricketclub.R.string.failed_to_open_whatsapp, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun sendCustomMessage(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.trim()
            if (cleanNumber.isEmpty()) {
                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = Uri.parse("https://wa.me/$cleanNumber?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
