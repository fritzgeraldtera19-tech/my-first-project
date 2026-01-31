package com.safe_tap.app

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import kotlin.random.Random

class VerificationManager(private val context: Context) {

    companion object {
        private const val VERIFICATION_PREFIX = "SAFE-TAP Verification: "
    }

    private val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }

    /**
     * Generate 6-digit verification code
     */
    fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Send verification code via SMS
     */
    fun sendVerificationCode(
        mobileNumber: String,
        code: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val message = """
                $VERIFICATION_PREFIX$code
                
                Enter this code to verify your SAFE-TAP account.
                Valid for 10 minutes.
                
                Do not share this code.
            """.trimIndent()

            smsManager.sendTextMessage(
                mobileNumber,
                null,
                message,
                null,
                null
            )

            onSuccess()
        } catch (e: Exception) {
            onFailure(e.message ?: "Failed to send verification code")
        }
    }

    /**
     * Validate verification code
     */
    fun validateCode(enteredCode: String, actualCode: String): Boolean {
        return enteredCode.trim() == actualCode.trim()
    }
}