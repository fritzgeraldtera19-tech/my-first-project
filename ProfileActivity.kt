package com.safe_tap.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

class ProfileActivity : ComponentActivity() {
    private lateinit var tvName: TextView
    private lateinit var tvMobile: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvStrikeCount: TextView
    private lateinit var btnLogout: Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        tvName = findViewById(R.id.tvName)
        tvMobile = findViewById(R.id.tvMobile)
        tvAddress = findViewById(R.id.tvAddress)
        tvStrikeCount = findViewById(R.id.tvStrikeCount)
        btnLogout = findViewById(R.id.btnLogout)

        // Load user data
        loadUserData()

        btnLogout.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        val userId = sessionManager.getUserId()
        val user = dbHelper.getUserDetails(userId)

        if (user != null) {
            tvName.text = user.fullName
            tvMobile.text = user.mobile
            tvAddress.text = user.address
            tvStrikeCount.text = "${user.strikeCount} of 3"
        }
    }
}