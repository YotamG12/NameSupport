package com.namesupport.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Calls the Anthropic Messages API (claude-haiku-4-5) to transliterate a batch
 * of Hebrew words into English phonetic spellings in a single network round-trip.
 *
 * Returns an empty map (and logs a warning) on any error, so callers fall back
 * to the local char-by-char engine gracefully.
 */
class ClaudeTransliterator(private val context: Context) {

    companion object {
        private const val TAG = "ClaudeTransliterator"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 1024
        private const val TIMEOUT_MS = 30_000
    }

    private val systemPrompt = """
        You are a Hebrew-to-English phonetic transliteration service for a mobile contacts app.
        Given a JSON array of Hebrew words (names or relationship labels), return a JSON object
        mapping each Hebrew word to its standard English phonetic spelling.

        Rules:
        - Use the common Israeli English phonetic form, not biblical/religious transliterations.
          Examples: יוסי → Yossi (not Joseph), משה → Moshe (not Moses), דוד → David
        - Relationship labels: אבא → Abba, אמא → Ima, סבא → Saba, סבתא → Savta
        - When ו is a vowel (not a consonant): שלום → Shalom, יובל → Yuval, דוד → David
        - Family names: כהן → Cohen, לוי → Levi, מזרחי → Mizrachi
        - Return ONLY the JSON object — no explanation, no markdown, no code blocks.
    """.trimIndent()

    suspend fun transliterateWords(words: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val apiKey = ApiKeyManager.getApiKey(context)
                ?: return@withContext emptyMap<String, String>().also {
                    Log.d(TAG, "No API key — skipping Claude call")
                }

            try {
                val requestBody = buildRequestBody(words)
                val responseText = post(apiKey, requestBody)
                parseResponse(responseText)
            } catch (e: Exception) {
                Log.w(TAG, "transliterateWords failed: ${e.message}")
                emptyMap()
            }
        }

    private fun buildRequestBody(words: List<String>): String {
        val wordsArray = JSONArray().apply { words.forEach { put(it) } }
        return JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", MAX_TOKENS)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Transliterate these Hebrew words: $wordsArray")
                })
            })
        }.toString()
    }

    private fun post(apiKey: String, body: String): String {
        val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
        }

        conn.outputStream.bufferedWriter().use { it.write(body) }

        val code = conn.responseCode
        return if (code == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw Exception("HTTP $code: $err")
        }
    }

    private fun parseResponse(responseText: String): Map<String, String> {
        val text = JSONObject(responseText)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            // Strip markdown code fences if Claude wraps the JSON
            .trimIndent()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(text)
        return obj.keys().asSequence().associateWith { obj.getString(it) }
    }
}
