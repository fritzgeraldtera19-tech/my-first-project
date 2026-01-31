package com.safe_tap.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ConfirmationActivity : ComponentActivity() {

    private lateinit var tvEmergencyType: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var btnCancel: Button

    private var countDownTimer: CountDownTimer? = null
    private var emergencyType: String = ""

    private lateinit var sessionManager: SessionManager
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var locationHandler: LocationHandler

    companion object {
        private const val TAG = "ConfirmationActivity"
        private const val COUNTDOWN_SECONDS = 5
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        Log.d(TAG, "ConfirmationActivity started")

        sessionManager = SessionManager(this)
        databaseHelper = DatabaseHelper(this)
        locationHandler = LocationHandler(this)

        emergencyType = intent.getStringExtra("EMERGENCY_TYPE") ?: "Unknown"

        tvEmergencyType = findViewById(R.id.tvEmergencyType)
        tvCountdown = findViewById(R.id.tvCountdown)
        btnCancel = findViewById(R.id.btnCancel)

        tvEmergencyType.text = getString(R.string.emergency_label, emergencyType)

        val hasSMS = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Permission check - SMS: $hasSMS, Location: $hasLocation")
        Log.d(TAG, "Emergency Type: $emergencyType")

        if (!hasSMS || !hasLocation) {
            val permissions = mutableListOf<String>()
            if (!hasSMS) permissions.add(Manifest.permission.SEND_SMS)
            if (!hasLocation) permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        startCountdown()

        btnCancel.setOnClickListener {
            cancelAlert()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                finish()
                return
            }

            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d(TAG, "Permissions granted")
                Toast.makeText(this, "Permissions granted! Starting alert...", Toast.LENGTH_SHORT).show()
                startCountdown()

                btnCancel.setOnClickListener {
                    cancelAlert()
                }
            } else {
                Log.e(TAG, "Permissions denied")
                Toast.makeText(
                    this,
                    "SMS and Location permissions are required.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun cancelAlert() {
        Log.d(TAG, "User cancelled emergency alert")
        countDownTimer?.cancel()
        Toast.makeText(this, "Alert cancelled", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun startCountdown() {
        Log.d(TAG, "Starting $COUNTDOWN_SECONDS second countdown...")

        countDownTimer = object : CountDownTimer(
            COUNTDOWN_SECONDS * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000L).toInt()
                tvCountdown.text = secondsRemaining.toString()
                Log.d(TAG, "Countdown: $secondsRemaining seconds")
            }

            override fun onFinish() {
                tvCountdown.text = "0"
                Log.d(TAG, "Countdown: 0 seconds")
                Log.d(TAG, "Countdown finished - sending emergency alert")
                sendEmergencyAlert()
            }
        }.start()
    }

    private fun sendEmergencyAlert() {
        val userId = sessionManager.getUserId()
        val userName = sessionManager.getUserName()

        if (userId <= 0 || userName.isEmpty()) {
            Log.e(TAG, "User not logged in!")
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Sending emergency alert for user: $userName (ID: $userId)")
        Toast.makeText(this, "Sending emergency alert...", Toast.LENGTH_SHORT).show()

        locationHandler.getCurrentLocation(
            onSuccess = { latitude, longitude ->
                handleLocationSuccess(userId, userName, latitude, longitude)
            },
            onFailure = { error ->
                handleLocationFailure(error)
            }
        )
    }

    private fun handleLocationSuccess(
        userId: Int,
        userName: String,
        latitude: Double,
        longitude: Double
    ) {
        Log.d(TAG, "Location obtained: $latitude, $longitude")

        val reportId = databaseHelper.insertEmergencyReport(
            userId = userId,
            emergencyType = emergencyType,
            latitude = latitude,
            longitude = longitude
        )

        if (reportId > 0) {
            Log.d(TAG, "Emergency report saved to DB with ID: $reportId")
        } else {
            Log.e(TAG, "Failed to save emergency report to DB")
        }

        val smsHandler = SMSHandler(this)
        smsHandler.sendEmergencySMS(
            userName = userName,
            emergencyType = emergencyType,
            latitude = latitude,
            longitude = longitude,
            onSuccess = {
                handleSMSSuccess(latitude, longitude)
            },
            onFailure = { error ->
                handleSMSFailure(error)
            }
        )
    }

    private fun handleLocationFailure(error: String) {
        Log.e(TAG, "Failed to get location: $error")
        runOnUiThread {
            Toast.makeText(
                this,
                "Failed to get location: $error",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun handleSMSSuccess(latitude: Double, longitude: Double) {
        Log.d(TAG, "SMS sent successfully!")
        runOnUiThread {
            val intent = Intent(this, AlertSentActivity::class.java)
            intent.putExtra("EMERGENCY_TYPE", emergencyType)
            intent.putExtra("LATITUDE", latitude)
            intent.putExtra("LONGITUDE", longitude)
            startActivity(intent)
            finish()
        }
    }

    private fun handleSMSFailure(error: String) {
        Log.e(TAG, "Failed to send SMS: $error")
        runOnUiThread {
            Toast.makeText(
                this,
                "Failed to send alert: $error",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        Log.d(TAG, "ConfirmationActivity destroyed")
    }
}