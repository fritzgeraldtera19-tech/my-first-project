package com.safe_tap.app

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("SafeTapSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_MOBILE = "user_mobile"
    }

    fun saveLoginSession(userId: Long, userName: String, mobile: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_MOBILE, mobile)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserMobile(): String? {
        return prefs.getString(KEY_USER_MOBILE, null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}