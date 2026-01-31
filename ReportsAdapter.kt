package com.safe_tap.app

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class EmergencyReport(
    val id: Int,
    val emergencyType: String,
    val location: String,
    val timestamp: String
)

class ReportsAdapter(
    private var reports: MutableList<EmergencyReport>,
    private val onDeleteClick: (EmergencyReport, Int) -> Unit
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmergencyType: TextView = view.findViewById(R.id.tvReportType)
        val tvLocation: TextView = view.findViewById(R.id.tvReportLocation)
        val tvTimestamp: TextView = view.findViewById(R.id.tvReportTimestamp)
        val cardReport: CardView = view.findViewById(R.id.cardReport)
        val btnDelete: CardView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.tvEmergencyType.text = report.emergencyType
        holder.tvLocation.text = "📍 ${report.location}"
        holder.tvTimestamp.text = "🕒 ${report.timestamp}"

        // Color code by emergency type
        val color = when {
            report.emergencyType.contains("Medical", ignoreCase = true) -> 0xFFE57373.toInt()
            report.emergencyType.contains("Harassment", ignoreCase = true) ||
                    report.emergencyType.contains("Crime", ignoreCase = true) -> 0xFFFFB74D.toInt()
            report.emergencyType.contains("Accident", ignoreCase = true) ||
                    report.emergencyType.contains("Disaster", ignoreCase = true) -> 0xFFFFF176.toInt()
            else -> 0xFF90CAF9.toInt()
        }

        holder.cardReport.setCardBackgroundColor(color)

        // Delete button click
        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context

            AlertDialog.Builder(context)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to delete this emergency report?")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteClick(report, position)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = reports.size

    fun removeItem(position: Int) {
        reports.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, reports.size)
    }
}