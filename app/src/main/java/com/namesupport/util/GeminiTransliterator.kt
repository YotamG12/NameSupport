package com.namesupport.util

import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONArray

class GeminiTransliterator(apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
    )

    suspend fun transliterateAll(names: List<String>): List<String> {
        if (names.isEmpty()) return emptyList()
        return try {
            val response = model.generateContent(buildPrompt(names))
            parseJsonArray(response.text ?: "", names.size)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildPrompt(names: List<String>): String {
        val jsonInput = buildString {
            append('[')
            names.forEachIndexed { i, name ->
                if (i > 0) append(", ")
                append('"')
                append(name.replace("\\", "\\\\").replace("\"", "\\\""))
                append('"')
            }
            append(']')
        }
        return """
            You are transliterating Hebrew contact names to English for Android voice commands.
            Rules:
            - Proper Hebrew names: use standard phonetic English spelling (e.g. מנגיסטו→Mangisto, מלכה→Malka, כהן→Cohen, אבי→Avi)
            - Common Hebrew words: translate to English meaning (אבא→Dad, אמא→Mom, של→of, סבא→Grandpa, סבתא→Grandma, עבודה→Work, בית→Home, מילואים→Miluim)
            - Multi-word names: handle each word independently using the rules above
            - Return ONLY a JSON array of strings in the same order as the input, no explanation or extra text

            Input: $jsonInput
        """.trimIndent()
    }

    private fun parseJsonArray(text: String, expectedSize: Int): List<String> {
        return try {
            val start = text.indexOf('[')
            val end = text.lastIndexOf(']')
            if (start == -1 || end == -1 || end <= start) return emptyList()
            val array = JSONArray(text.substring(start, end + 1))
            if (array.length() != expectedSize) return emptyList()
            (0 until array.length()).map { array.getString(it).trim() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
