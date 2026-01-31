package com.safe_tap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class TestPermissionsActivity : ComponentActivity() {

    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvLocationServices: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var tvUserInfo: TextView
    private lateinit var tvReadyStatus: TextView
    private lateinit var layoutReadyStatus: LinearLayout
    private lateinit var btnRefreshPermissions: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_permissions)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        initializeViews()
        checkAllPermissionsAndServices()

        btnRefreshPermissions.setOnClickListener {
            checkAllPermissionsAndServices()
        }
    }

    private fun initializeViews() {
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        tvLocationServices = findViewById(R.id.tvLocationServices)
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo)
        tvUserInfo = findViewById(R.id.tvUserInfo)
        tvReadyStatus = findViewById(R.id.tvReadyStatus)
        layoutReadyStatus = findViewById(R.id.layoutReadyStatus)
        btnRefreshPermissions = findViewById(R.id.btnRefreshPermissions)
    }

    private fun checkAllPermissionsAndServices() {
        checkPermissions()
        checkLocationServices()
        checkDeviceInfo()
        checkUserInfo()
        updateReadyStatus()
    }

    private fun checkPermissions() {
        val smsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        tvPermissionStatus.text = buildString {
            append("SMS Permission\n")
            append(if (smsGranted) "✓ Granted" else "✗ Denied")
            append("\n\nFine Location\n")
            append(if (fineLocationGranted) "✓ Granted" else "✗ Denied")
            append("\n\nCoarse Location\n")
            append(if (coarseLocationGranted) "✓ Granted" else "✗ Denied")
        }
    }

    private fun checkLocationServices() {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        tvLocationServices.text = buildString {
            append("GPS Provider\n")
            append(if (gpsEnabled) "✓ Enabled" else "✗ Disabled")
            append("\n\nNetwork Provider\n")
            append(if (networkEnabled) "✓ Enabled" else "✗ Disabled")
        }
    }

    private fun checkDeviceInfo() {
        tvDeviceInfo.text = buildString {
            append("Device: ${android.os.Build.MODEL}\n")
            append("Android: ${android.os.Build.VERSION.RELEASE}\n")
            append("SDK: ${android.os.Build.VERSION.SDK_INT}\n")
            append("Emulator: ${if (isEmulator()) "Yes" else "No"}")
        }
    }

    private fun checkUserInfo() {
        val userName = sessionManager.getUserName() ?: "Not logged in"
        val userId = sessionManager.getUserId()

        tvUserInfo.text = buildString {
            append("Name: $userName\n")
            append("User ID: $userId")
        }
    }

    private fun updateReadyStatus() {
        val smsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val locationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val allReady = smsGranted && locationGranted

        if (allReady) {
            tvReadyStatus.text = "✓ All Systems Ready\nEmergency alerts can be sent"
            layoutReadyStatus.setBackgroundResource(R.drawable.card_background_success)
        } else {
            tvReadyStatus.text = "⚠ Permissions Required\nPlease grant all permissions to continue"
            layoutReadyStatus.setBackgroundColor(0xFFFF9800.toInt()) // Orange color
        }
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator"))
    }
}