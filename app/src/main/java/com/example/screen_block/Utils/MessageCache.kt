package com.example.screen_block.Utils

import android.content.Context

object MessageCache {
    private const val PREF_NAME = "screen_block_messages"
    private const val KEY_MESSAGE = "pending_toast_message"

    fun saveMessage(context: Context, message: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MESSAGE, message)
            .apply()
    }

    fun getAndClearMessage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val message = prefs.getString(KEY_MESSAGE, null)
        if (message != null) {
            prefs.edit().remove(KEY_MESSAGE).apply()
        }
        return message
    }
}