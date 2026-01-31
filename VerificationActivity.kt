package com.safe_tap.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class VerificationActivity : ComponentActivity() {
    private lateinit var etCode1: EditText
    private lateinit var etCode2: EditText
    private lateinit var etCode3: EditText
    private lateinit var etCode4: EditText
    private lateinit var etCode5: EditText
    private lateinit var etCode6: EditText
    private lateinit var btnVerify: Button
    private lateinit var btnResend: Button
    private lateinit var tvTimer: TextView
    private lateinit var tvMobile: TextView

    private lateinit var verificationManager: VerificationManager
    private lateinit var dbHelper: DatabaseHelper
    private var countDownTimer: CountDownTimer? = null

    private var actualCode: String = ""
    private var fullName: String = ""
    private var mobileNumber: String = ""
    private var address: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        verificationManager = VerificationManager(this)
        dbHelper = DatabaseHelper(this)

        // Get data from intent
        fullName = intent.getStringExtra("fullName") ?: ""
        mobileNumber = intent.getStringExtra("mobile") ?: ""
        address = intent.getStringExtra("address") ?: ""
        password = intent.getStringExtra("password") ?: ""
        actualCode = intent.getStringExtra("code") ?: ""

        initViews()
        setupCodeInputs()
        startTimer()

        btnVerify.setOnClickListener {
            verifyCode()
        }

        btnResend.setOnClickListener {
            resendCode()
        }
    }

    private fun initViews() {
        etCode1 = findViewById(R.id.etCode1)
        etCode2 = findViewById(R.id.etCode2)
        etCode3 = findViewById(R.id.etCode3)
        etCode4 = findViewById(R.id.etCode4)
        etCode5 = findViewById(R.id.etCode5)
        etCode6 = findViewById(R.id.etCode6)
        btnVerify = findViewById(R.id.btnVerify)
        btnResend = findViewById(R.id.btnResend)
        tvTimer = findViewById(R.id.tvTimer)
        tvMobile = findViewById(R.id.tvMobile)

        tvMobile.text = "Code sent to $mobileNumber"
    }

    private fun setupCodeInputs() {
        val editTexts = listOf(etCode1, etCode2, etCode3, etCode4, etCode5, etCode6)

        editTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < editTexts.size - 1) {
                        editTexts[index + 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun startTimer() {
        btnResend.isEnabled = false

        countDownTimer = object : CountDownTimer(600000, 1000) { // 10 minutes
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                tvTimer.text = String.format("Code expires in %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "Code expired"
                btnResend.isEnabled = true
            }
        }.start()
    }

    private fun verifyCode() {
        val enteredCode = etCode1.text.toString() +
                etCode2.text.toString() +
                etCode3.text.toString() +
                etCode4.text.toString() +
                etCode5.text.toString() +
                etCode6.text.toString()

        if (enteredCode.length != 6) {
            Toast.makeText(this, "Enter complete code", Toast.LENGTH_SHORT).show()
            return
        }

        if (verificationManager.validateCode(enteredCode, actualCode)) {
            // Register user in database
            val userId = dbHelper.registerUser(fullName, mobileNumber, address, password)

            if (userId > 0) {
                Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("mobile", mobileNumber)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show()
            clearCodeInputs()
        }
    }

    private fun resendCode() {
        actualCode = verificationManager.generateVerificationCode()

        val internationalNumber = "+63${mobileNumber.substring(1)}"

        verificationManager.sendVerificationCode(
            mobileNumber = internationalNumber,
            code = actualCode,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "New verification code sent", Toast.LENGTH_SHORT).show()
                    countDownTimer?.cancel()
                    startTimer()
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Failed to send SMS: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun clearCodeInputs() {
        etCode1.text.clear()
        etCode2.text.clear()
        etCode3.text.clear()
        etCode4.text.clear()
        etCode5.text.clear()
        etCode6.text.clear()
        etCode1.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}