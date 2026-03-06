package com.magnum.cricketclub.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.magnum.cricketclub.data.Expense
import java.text.SimpleDateFormat
import java.util.*

object WhatsAppHelper {
    
    private fun getExpenseMessage(expense: Expense, categoryName: String?, isNew: Boolean, currentBalance: Double?): String {
        val action = if (isNew) "Added" else "Updated"
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(expense.date))
        val type = if (expense.isIncome) "Income" else "Expense"
        val sign = if (expense.isIncome) "+" else "-"
        
        return """
            Magnum Expense/Income Update 

            ${action}: ${type}
            Category: ${categoryName ?: "Unknown"}
            Amount: ${sign}₹${String.format("%.2f", expense.amount)}
            ${if (expense.description.isNotEmpty()) "Description: ${expense.description}\n" else ""}
            Date: ${dateStr}
            By: ${expense.createdByEmail ?: "Unknown"}
            ${if (currentBalance != null) "\nUpdated Balance: ₹${String.format("%.2f", currentBalance)}" else ""}
        """.trimIndent()
    }

    fun shareExpenseViaWhatsApp(context: Context, expense: Expense, categoryName: String?, isNew: Boolean, currentBalance: Double?) {
        val message = getExpenseMessage(expense, categoryName, isNew, currentBalance)
        shareToWhatsApp(context, message)
    }

    fun sendExpenseUpdateToGroup(context: Context, groupId: String, expense: Expense, categoryName: String?, isNew: Boolean, currentBalance: Double?) {
        val message = getExpenseMessage(expense, categoryName, isNew, currentBalance)
        shareToWhatsApp(context, message, groupId)
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
                ${messageTitle}

                Hi ${greetingName},

                This is a gentle reminder for your team contribution for ${monthName} ${year}.

                Amount: ₹${String.format("%.2f", amount)}
                Status: ${messageStatus}

                Please complete the payment and update the status once done.

                Thank you for supporting Magnum Cricket Club! 🏏
            """.trimIndent()

            sendCustomMessage(context, phoneNumber, message)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendCustomMessage(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.trim()
            if (cleanNumber.isEmpty()) {
                shareToWhatsApp(context, message)
                return
            }
            
            // Standard WhatsApp direct message URL
            val uri = Uri.parse("https://wa.me/${cleanNumber}?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Try without setPackage (might open in browser or other app)
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToWhatsApp(context: Context, message: String, groupId: String? = null) {
        try {
            // If group ID is present, try to open the group first (to help user find it)
            if (!groupId.isNullOrBlank()) {
                try {
                    val inviteLink = if (groupId.startsWith("http")) groupId else "https://chat.whatsapp.com/${groupId}"
                    val groupIntent = Intent(Intent.ACTION_VIEW, Uri.parse(inviteLink))
                    groupIntent.setPackage("com.whatsapp")
                    context.startActivity(groupIntent)
                    Toast.makeText(context, "Opening Group. Please select WhatsApp to send the message next.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Ignore group open failure and proceed to share
                }
            }
            
            // Prepare the share intent targeting WhatsApp's "Send to..." screen
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_TEXT, message)
            sendIntent.setPackage("com.whatsapp")
            
            try {
                context.startActivity(sendIntent)
            } catch (ex: android.content.ActivityNotFoundException) {
                // Try WhatsApp Business package if standard is not found
                try {
                    sendIntent.setPackage("com.whatsapp.w4b")
                    context.startActivity(sendIntent)
                } catch (ex2: android.content.ActivityNotFoundException) {
                    // Final fallback: Use generic chooser
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, message)
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share to WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
