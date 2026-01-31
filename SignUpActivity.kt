package com.safe_tap.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : ComponentActivity() {
    private lateinit var etFullName: TextInputEditText
    private lateinit var etMobileNumber: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbAgreePolicy: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvBackToLogin: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var verificationManager: VerificationManager

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "SMS permission is required to send verification code",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        dbHelper = DatabaseHelper(this)
        verificationManager = VerificationManager(this)

        etFullName = findViewById(R.id.etFullName)
        etMobileNumber = findViewById(R.id.etMobileNumber)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbAgreePolicy = findViewById(R.id.cbAgreePolicy)
        btnRegister = findViewById(R.id.btnRegister)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        checkSmsPermission()
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val mobile = etMobileNumber.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        when {
            fullName.isEmpty() -> {
                Toast.makeText(this, "Enter your full name", Toast.LENGTH_SHORT).show()
                etFullName.requestFocus()
            }
            mobile.isEmpty() -> {
                Toast.makeText(this, "Enter mobile number", Toast.LENGTH_SHORT).show()
                etMobileNumber.requestFocus()
            }
            mobile.length != 11 || !mobile.startsWith("09") -> {
                Toast.makeText(this, "Enter valid PH mobile number (09XXXXXXXXX)", Toast.LENGTH_SHORT).show()
                etMobileNumber.requestFocus()
            }
            address.isEmpty() -> {
                Toast.makeText(this, "Enter your address", Toast.LENGTH_SHORT).show()
                etAddress.requestFocus()
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                etPassword.requestFocus()
            }
            password.length < 6 -> {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                etPassword.requestFocus()
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                etConfirmPassword.requestFocus()
            }
            !cbAgreePolicy.isChecked -> {
                Toast.makeText(this, "Please agree to 3-Strike Policy", Toast.LENGTH_SHORT).show()
            }
            else -> {
                if (dbHelper.isMobileExists(mobile)) {
                    Toast.makeText(this, "Mobile number already registered", Toast.LENGTH_LONG).show()
                    return
                }

                val code = verificationManager.generateVerificationCode()
                sendVerificationSMS(fullName, mobile, address, password, code)
            }
        }
    }

    private fun sendVerificationSMS(fullName: String, mobile: String, address: String, password: String, code: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_LONG).show()
            checkSmsPermission()
            return
        }

        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show()
        btnRegister.isEnabled = false

        val internationalNumber = "+63${mobile.substring(1)}"

        verificationManager.sendVerificationCode(
            mobileNumber = internationalNumber,
            code = code,
            onSuccess = {
                runOnUiThread {
                    btnRegister.isEnabled = true
                    Toast.makeText(this, "Verification code sent to $mobile", Toast.LENGTH_SHORT).show()
                    navigateToVerification(fullName, mobile, address, password, code)
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    btnRegister.isEnabled = true
                    Toast.makeText(this, "Failed to send SMS: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun navigateToVerification(fullName: String, mobile: String, address: String, password: String, code: String) {
        val intent = Intent(this, VerificationActivity::class.java)
        intent.putExtra("fullName", fullName)
        intent.putExtra("mobile", mobile)
        intent.putExtra("address", address)
        intent.putExtra("password", password)
        intent.putExtra("code", code)
        startActivity(intent)
        finish()
    }
}