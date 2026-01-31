package com.safe_tap.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class SMSHandler(private val context: Context) {

    companion object {
        const val MDRRMO_NUMBER = "+639099434217"
        private const val TAG = "SMSHandler"
        private const val SMS_SENT = "SMS_SENT_ACTION"
    }

    private var sentReceiver: BroadcastReceiver? = null

    fun sendEmergencySMS(
        userName: String,
        emergencyType: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val message = buildEmergencyMessage(userName, emergencyType, latitude, longitude)

        Log.d(TAG, "=== ATTEMPTING TO SEND SMS ===")
        Log.d(TAG, "To: $MDRRMO_NUMBER")
        Log.d(TAG, "Message: $message")
        Log.d(TAG, "Message length: ${message.length} chars")

        try {
            // Clean up any previous receiver
            cleanupReceiver()

            // Create sent receiver with success/failure handling
            sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (resultCode) {
                        android.app.Activity.RESULT_OK -> {
                            Log.d(TAG, "✅ SMS SENT SUCCESSFULLY!")
                            cleanupReceiver()
                            onSuccess()
                        }
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                            Log.e(TAG, "❌ Generic failure")
                            cleanupReceiver()
                            onFailure("Network error. Check your signal and SMS credits.")
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
                            Log.e(TAG, "❌ No service")
                            cleanupReceiver()
                            onFailure("No cellular service.")
                        }
                        SmsManager.RESULT_ERROR_RADIO_OFF -> {
                            Log.e(TAG, "❌ Radio off")
                            cleanupReceiver()
                            onFailure("Airplane mode is on.")
                        }
                        SmsManager.RESULT_ERROR_NULL_PDU -> {
                            Log.e(TAG, "❌ Null PDU")
                            cleanupReceiver()
                            onFailure("Message format error.")
                        }
                        else -> {
                            Log.e(TAG, "❌ Unknown error: $resultCode")
                            cleanupReceiver()
                            onFailure("Failed to send SMS (code: $resultCode)")
                        }
                    }
                }
            }

            // Register receiver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    sentReceiver,
                    IntentFilter(SMS_SENT),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(sentReceiver, IntentFilter(SMS_SENT))
            }

            Log.d(TAG, "Broadcast receiver registered")

            // Create pending intent
            val sentIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(SMS_SENT),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.FLAG_MUTABLE
                else
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

            Log.d(TAG, "PendingIntent created")

            // Get SmsManager
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            Log.d(TAG, "SmsManager obtained")

            // Split and send
            val parts = smsManager.divideMessage(message)

            if (parts.size > 1) {
                Log.d(TAG, "Sending multipart SMS: ${parts.size} parts")
                val sentIntents = ArrayList<PendingIntent>()
                repeat(parts.size) { sentIntents.add(sentIntent) }

                smsManager.sendMultipartTextMessage(
                    MDRRMO_NUMBER,
                    null,
                    parts,
                    sentIntents,
                    null
                )
            } else {
                Log.d(TAG, "Sending single SMS")
                smsManager.sendTextMessage(
                    MDRRMO_NUMBER,
                    null,
                    message,
                    sentIntent,
                    null
                )
            }

            Log.d(TAG, "SMS command executed, waiting for broadcast result...")

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security Exception", e)
            cleanupReceiver()
            onFailure("SMS permission denied: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ Illegal Argument", e)
            cleanupReceiver()
            onFailure("Invalid phone number or message: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while sending SMS", e)
            cleanupReceiver()
            onFailure("Error: ${e.message}")
        }
    }

    private fun cleanupReceiver() {
        try {
            sentReceiver?.let {
                context.unregisterReceiver(it)
                sentReceiver = null
                Log.d(TAG, "Receiver cleaned up")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Receiver already unregistered")
        }
    }

    private fun buildEmergencyMessage(
        userName: String,
        emergencyType: String,
        latitude: Double,
        longitude: Double
    ): String {
        val time = SimpleDateFormat("hh:mma", Locale.getDefault()).format(Date())
        val url = "https://maps.google.com/?q=$latitude,$longitude"
        return "🚨$emergencyType\n$userName\n$time\n$url"
    }
}