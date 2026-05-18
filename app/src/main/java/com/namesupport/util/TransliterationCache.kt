package com.namesupport.util

import android.content.Context

/**
 * Persistent cache so each Hebrew word is only sent to the Claude API once.
 * Backed by SharedPreferences (key = bare Hebrew word, value = English result).
 */
object TransliterationCache {

    private const val PREFS = "transliteration_cache"

    fun get(context: Context, hebrewWord: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(hebrewWord, null)

    fun putAll(context: Context, results: Map<String, String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .apply { results.forEach { (k, v) -> putString(k, v) } }
            .apply()
    }

    fun clear(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
}
