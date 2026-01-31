package com.safe_tap.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

class AlertSentActivity : ComponentActivity() {
    private lateinit var tvEmergencyType: TextView
    private lateinit var tvTimeSent: TextView
    private lateinit var tvGPSLocation: TextView
    private lateinit var tvUserName: TextView
    private lateinit var btnViewOnMap: Button
    private lateinit var btnBackToDashboard: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_sent)

        tvEmergencyType = findViewById(R.id.tvEmergencyType)
        tvTimeSent = findViewById(R.id.tvTimeSent)
        tvGPSLocation = findViewById(R.id.tvGPSLocation)
        tvUserName = findViewById(R.id.tvUserName)
        btnViewOnMap = findViewById(R.id.btnViewOnMap)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)

        // Get real data from intent
        val emergencyType = intent.getStringExtra("EMERGENCY_TYPE") ?: "Unknown"
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        val timeSent = intent.getStringExtra("TIME_SENT") ?: "Unknown time"
        val userName = intent.getStringExtra("USER_NAME") ?: "Unknown user"

        // Display real data
        tvEmergencyType.text = emergencyType
        tvTimeSent.text = timeSent
        tvGPSLocation.text = "$latitude, $longitude"
        tvUserName.text = "Reported by: $userName"

        // View location on Google Maps
        btnViewOnMap.setOnClickListener {
            val mapUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            val intent = Intent(Intent.ACTION_VIEW, mapUri)
            intent.setPackage("com.google.android.apps.maps") // Open in Google Maps app if available

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to browser if Maps app not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, mapUri)
                startActivity(browserIntent)
            }
        }

        // Back to Dashboard
        btnBackToDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}