package com.namesupport.util

import android.content.Context
import android.util.Log

/**
 * Transliteration pipeline with four layers (fastest → most accurate):
 *
 *  1. Local dictionary  — instant, offline, curated spellings
 *  2. Persistent cache  — instant, offline, previously API-resolved words
 *  3. Claude API        — accurate, batched, requires internet + API key
 *  4. Char-by-char      — always available, phonetic fallback
 *
 * All unique Hebrew words across a whole contact scan are resolved in a single
 * API call, minimising latency and token cost.
 */
class SmartTransliterator(private val context: Context) {

    private val claude = ClaudeTransliterator(context)

    /**
     * Transliterates a list of full display names (may include spaces and mixed
     * Hebrew/Latin words).  Returns a map of displayName → English suggestion.
     */
    suspend fun transliterateAll(names: List<String>): Map<String, String> {
        // 1. Collect every unique Hebrew word across all names
        val allHebrewWords = names
            .flatMap { it.trim().split("\\s+".toRegex()) }
            .filter { it.isNotBlank() && HebrewTransliterator.containsHebrew(it) }
            .map { HebrewTransliterator.stripNikud(it) }
            .distinct()

        // 2. Resolve them through the pipeline
        val wordMap = resolveWords(allHebrewWords)

        // 3. Reassemble full names
        return names.associateWith { name ->
            name.trim()
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    val stripped = HebrewTransliterator.stripNikud(word)
                    if (HebrewTransliterator.containsHebrew(stripped)) {
                        wordMap[stripped] ?: word
                    } else {
                        word // keep Latin tokens unchanged (e.g. "David" in "David כהן")
                    }
                }
        }
    }

    /** Convenience: transliterate a single display name. */
    suspend fun transliterate(name: String): String =
        transliterateAll(listOf(name))[name] ?: HebrewTransliterator.transliterate(name)

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun resolveWords(words: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Layer 1: local dictionary
        val missLocal = mutableListOf<String>()
        words.forEach { w ->
            val hit = HebrewTransliterator.dictionaryLookup(w)
            if (hit != null) result[w] = hit else missLocal.add(w)
        }

        // Layer 2: persistent cache
        val missCache = mutableListOf<String>()
        missLocal.forEach { w ->
            val hit = TransliterationCache.get(context, w)
            if (hit != null) result[w] = hit else missCache.add(w)
        }

        // Layer 3: Claude API (batched)
        if (missCache.isNotEmpty()) {
            val apiResults = claude.transliterateWords(missCache)
            if (apiResults.isNotEmpty()) {
                TransliterationCache.putAll(context, apiResults)
                result.putAll(apiResults)
                Log.d(TAG, "Claude resolved ${apiResults.size}/${missCache.size} words")
            }
        }

        // Layer 4: char-by-char fallback for anything still unresolved
        words.forEach { w ->
            if (w !in result) {
                result[w] = HebrewTransliterator.transliterateCharByChar(w)
            }
        }

        return result
    }

    companion object {
        private const val TAG = "SmartTransliterator"
    }
}
