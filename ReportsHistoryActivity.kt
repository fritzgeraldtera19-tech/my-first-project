package com.safe_tap.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReportsHistoryActivity : ComponentActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var rvReports: RecyclerView
    private lateinit var tvNoReports: TextView
    private lateinit var tvTotalReports: TextView
    private lateinit var adapter: ReportsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports_history)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        rvReports = findViewById(R.id.rvReports)
        tvNoReports = findViewById(R.id.tvNoReports)
        tvTotalReports = findViewById(R.id.tvTotalReports)

        rvReports.layoutManager = LinearLayoutManager(this)

        loadReports()
    }

    private fun loadReports() {
        val userId = sessionManager.getUserId()
        val reports = dbHelper.getUserEmergencyReports(userId).toMutableList()

        if (reports.isEmpty()) {
            tvNoReports.visibility = android.view.View.VISIBLE
            rvReports.visibility = android.view.View.GONE
            tvTotalReports.text = "No emergency reports yet"
        } else {
            tvNoReports.visibility = android.view.View.GONE
            rvReports.visibility = android.view.View.VISIBLE
            tvTotalReports.text = "Total Reports: ${reports.size}"

            adapter = ReportsAdapter(reports) { report, position ->
                deleteReport(report, position)
            }
            rvReports.adapter = adapter
        }
    }

    private fun deleteReport(report: EmergencyReport, position: Int) {
        // Delete from database
        val deleted = dbHelper.deleteEmergencyReport(report.id)

        if (deleted) {
            // Remove from adapter
            adapter.removeItem(position)

            // Update total count
            val remainingCount = adapter.itemCount
            if (remainingCount == 0) {
                tvNoReports.visibility = android.view.View.VISIBLE
                rvReports.visibility = android.view.View.GONE
                tvTotalReports.text = "No emergency reports yet"
            } else {
                tvTotalReports.text = "Total Reports: $remainingCount"
            }

            Toast.makeText(this, "Report deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete report", Toast.LENGTH_SHORT).show()
        }
    }
}