package com.safe_tap.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class DashboardActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var btnMedical: LinearLayout
    private lateinit var btnHarassment: LinearLayout
    private lateinit var btnAccident: LinearLayout
    private lateinit var btnViewProfile: Button
    private lateinit var btnPermissions: LinearLayout
    private lateinit var btnReportsHistory: LinearLayout

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Some permissions were denied. The app may not work properly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_dashboard)

        // Initialize views

        initializeViews()

        // Request permissions on first launch
        checkAndRequestPermissions()

        // Set click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        btnMedical = findViewById(R.id.btnMedical)
        btnHarassment = findViewById(R.id.btnHarassment)
        btnAccident = findViewById(R.id.btnAccident)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnReportsHistory = findViewById(R.id.btnReportsHistory)
    }

    private fun setupClickListeners() {
        btnMedical.setOnClickListener {
            sendEmergencyAlert("Medical")
        }

        btnHarassment.setOnClickListener {
            sendEmergencyAlert("Harassment/Crime")
        }

        btnAccident.setOnClickListener {
            sendEmergencyAlert("Accident/Disaster")
        }

        btnViewProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnPermissions.setOnClickListener {
            startActivity(Intent(this, TestPermissionsActivity::class.java))
        }

        btnReportsHistory.setOnClickListener {
            startActivity(Intent(this, ReportsHistoryActivity::class.java))
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        // Check Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Request permissions if needed
        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun sendEmergencyAlert(emergencyType: String) {
        // Check if permissions are granted
        val hasSMS = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // If permissions not granted, request them
        if (!hasSMS || !hasLocation) {
            Toast.makeText(
                this,
                "Please grant SMS and Location permissions to send emergency alerts",
                Toast.LENGTH_LONG
            ).show()
            checkAndRequestPermissions()
            return
        }

        // Start confirmation activity with emergency type
        val intent = Intent(this, ConfirmationActivity::class.java)
        intent.putExtra("EMERGENCY_TYPE", emergencyType)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Check session on resume
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}