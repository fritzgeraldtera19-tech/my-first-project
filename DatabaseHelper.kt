package com.safe_tap.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "SafeTapDB"
        private const val DATABASE_VERSION = 2

        // Users Table
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_MOBILE = "mobile_number"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_STRIKE_COUNT = "strike_count"
        private const val COLUMN_IS_BLOCKED = "is_blocked"
        private const val COLUMN_CREATED_AT = "created_at"

        // Emergency Reports Table
        private const val TABLE_REPORTS = "emergency_reports"
        private const val COLUMN_REPORT_ID = "report_id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_EMERGENCY_TYPE = "emergency_type"
        private const val COLUMN_GPS_LOCATION = "gps_location"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_IS_FALSE_REPORT = "is_false_report"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create Users Table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_MOBILE TEXT UNIQUE NOT NULL,
                $COLUMN_ADDRESS TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_STRIKE_COUNT INTEGER DEFAULT 0,
                $COLUMN_IS_BLOCKED INTEGER DEFAULT 0,
                $COLUMN_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        // Create Emergency Reports Table
        val createReportsTable = """
            CREATE TABLE $TABLE_REPORTS (
                $COLUMN_REPORT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_EMERGENCY_TYPE TEXT NOT NULL,
                $COLUMN_GPS_LOCATION TEXT NOT NULL,
                $COLUMN_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_IS_FALSE_REPORT INTEGER DEFAULT 0,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()

        db?.execSQL(createUsersTable)
        db?.execSQL(createReportsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
        onCreate(db)
    }

    fun registerUser(fullName: String, mobile: String, address: String, password: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FULL_NAME, fullName)
            put(COLUMN_MOBILE, mobile)
            put(COLUMN_ADDRESS, address)
            put(COLUMN_PASSWORD, password)
        }

        val result = db.insert(TABLE_USERS, null, values)
        Log.d("DatabaseHelper", "Registered user - Mobile: $mobile, Password: $password, Result: $result")
        return result
    }

    fun loginUser(mobile: String, password: String): Long {
        val db = readableDatabase

        Log.d("DatabaseHelper", "Login attempt - Mobile: $mobile, Password: $password")

        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID, COLUMN_IS_BLOCKED, COLUMN_PASSWORD),
            "$COLUMN_MOBILE = ?",
            arrayOf(mobile),
            null, null, null
        )

        var userId: Long = -1
        if (cursor.moveToFirst()) {
            val storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            val isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_BLOCKED))

            Log.d("DatabaseHelper", "Found user - Stored password: $storedPassword, Entered password: $password")

            if (password == storedPassword && isBlocked == 0) {
                userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                Log.d("DatabaseHelper", "Login successful - UserID: $userId")
            } else {
                Log.d("DatabaseHelper", "Login failed - Password match: ${password == storedPassword}, IsBlocked: $isBlocked")
            }
        } else {
            Log.d("DatabaseHelper", "No user found with mobile: $mobile")
        }
        cursor.close()
        return userId
    }

    fun isMobileExists(mobile: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_MOBILE = ?",
            arrayOf(mobile),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getUserDetails(userId: Long): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                mobile = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOBILE)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                strikeCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STRIKE_COUNT)),
                isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_BLOCKED)) == 1
            )
        }
        cursor.close()
        return user
    }

    fun saveEmergencyReport(userId: Long, emergencyType: String, gpsLocation: String): Long {
        val db = writableDatabase

        // Get current time in local timezone
        val currentTime = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_EMERGENCY_TYPE, emergencyType)
            put(COLUMN_GPS_LOCATION, gpsLocation)
            put(COLUMN_TIMESTAMP, currentTime)
        }
        return db.insert(TABLE_REPORTS, null, values)
    }

    // NEW FUNCTION: Get user's emergency reports
    fun getUserEmergencyReports(userId: Long): List<EmergencyReport> {
        val reports = mutableListOf<EmergencyReport>()
        val db = this.readableDatabase

        val cursor = db.query(
            TABLE_REPORTS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC" // Most recent first
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REPORT_ID))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_TYPE))
                val location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GPS_LOCATION))
                val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))

                reports.add(EmergencyReport(id, type, location, timestamp))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return reports
    }

    // NEW FUNCTION: Delete emergency report
    fun deleteEmergencyReport(reportId: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(
            TABLE_REPORTS,
            "$COLUMN_REPORT_ID = ?",
            arrayOf(reportId.toString())
        )
        return result > 0
    }
}

data class User(
    val id: Long,
    val fullName: String,
    val mobile: String,
    val address: String,
    val strikeCount: Int,
    val isBlocked: Boolean
)