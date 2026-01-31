package com.safe_tap.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : ComponentActivity() {
    private lateinit var etMobileNumber: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        // Initialize views
        etMobileNumber = findViewById(R.id.etMobileNumber)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        // Pre-fill mobile if coming from registration
        val preFillMobile = intent.getStringExtra("mobile")
        if (preFillMobile != null) {
            etMobileNumber.setText(preFillMobile)
            etPassword.requestFocus()
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Please contact MDRRMO for password reset", Toast.LENGTH_LONG).show()
        }
    }

    private fun loginUser() {
        val mobile = etMobileNumber.text.toString().trim()
        val password = etPassword.text.toString()

        when {
            mobile.isEmpty() -> {
                Toast.makeText(this, "Enter mobile number", Toast.LENGTH_SHORT).show()
                etMobileNumber.requestFocus()
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
                etPassword.requestFocus()
            }
            else -> {
                val userId = dbHelper.loginUser(mobile, password)

                if (userId > 0) {
                    // Get user details
                    val user = dbHelper.getUserDetails(userId)

                    if (user != null) {
                        if (user.isBlocked) {
                            Toast.makeText(
                                this,
                                "Account blocked due to 3 false reports. Contact MDRRMO.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }

                        // Save session
                        sessionManager.saveLoginSession(userId, user.fullName, user.mobile)

                        Toast.makeText(this, "Welcome, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    }
                } else {
                    Toast.makeText(this, "Invalid mobile number or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
