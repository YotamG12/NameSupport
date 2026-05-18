package com.namesupport.util

import android.content.Context

object ApiKeyManager {

    private const val PREFS = "api_key_prefs"
    private const val KEY   = "anthropic_api_key"

    fun getApiKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }

    fun setApiKey(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, key.trim())
            .apply()

    fun hasApiKey(context: Context) = getApiKey(context) != null
}
