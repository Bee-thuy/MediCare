package com.example.medicare.util

import android.content.Context
import android.content.pm.ApplicationInfo
import org.json.JSONObject

/**
 * Allows the app's debug build to demonstrate the ADMIN flow without a Firebase account.
 * This is deliberately disabled in release builds.
 */
object DebugMockAdminAccount {
    private const val FILE_NAME = "mock_admin_account.json"

    fun matches(context: Context, username: String, password: String): Boolean {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!isDebuggable) return false

        return runCatching {
            val account = context.assets.open(FILE_NAME).bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
            username == account.optString("username") && password == account.optString("password")
        }.getOrDefault(false)
    }
}
