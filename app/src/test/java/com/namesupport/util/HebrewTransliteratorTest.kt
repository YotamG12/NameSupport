package com.namesupport.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewTransliteratorTest {

    // --- Dictionary hits ---

    @Test
    fun `dictionary hit returns preferred spelling`() {
        assertEquals("Moshe", HebrewTransliterator.transliterate("משה"))
    }

    @Test
    fun `dictionary hit for female name`() {
        assertEquals("Sarah", HebrewTransliterator.transliterate("שרה"))
    }

    @Test
    fun `dictionary hit for family name`() {
        assertEquals("Cohen", HebrewTransliterator.transliterate("כהן"))
    }

    @Test
    fun `multi-word name with two dictionary hits`() {
        assertEquals("Yosef Mizrachi", HebrewTransliterator.transliterate("יוסף מזרחי"))
    }

    // --- User-reported examples (offline fallback) ---

    @Test
    fun `ori spelled correctly not uri`() {
        assertEquals("Ori", HebrewTransliterator.transliterate("אורי"))
    }

    @Test
    fun `abba translates to dad`() {
        assertEquals("Dad", HebrewTransliterator.transliterate("אבא"))
    }

    @Test
    fun `ima translates to mom`() {
        assertEquals("Mom", HebrewTransliterator.transliterate("אמא"))
    }

    @Test
    fun `shel translates to of`() {
        assertEquals("of", HebrewTransliterator.transliterate("של"))
    }

    @Test
    fun `malka is in dictionary`() {
        assertEquals("Malka", HebrewTransliterator.transliterate("מלכה"))
    }

    @Test
    fun `mangisto is in dictionary`() {
        assertEquals("Mangisto", HebrewTransliterator.transliterate("מנגיסטו"))
    }

    @Test
    fun `miluim is in dictionary`() {
        assertEquals("Miluim", HebrewTransliterator.transliterate("מילואים"))
    }

    @Test
    fun `avi malka full name`() {
        assertEquals("Avi Malka", HebrewTransliterator.transliterate("אבי מלכה"))
    }

    @Test
    fun `avi mangisto miluim full name`() {
        assertEquals("Avi Mangisto Miluim", HebrewTransliterator.transliterate("אבי מנגיסטו מילואים"))
    }

    @Test
    fun `abba shel ori multi-word relational name`() {
        assertEquals("Dad of Ori", HebrewTransliterator.transliterate("אבא של אורי"))
    }

    // --- Character-by-character fallback ---

    @Test
    fun `initial bet is b not v`() {
        // "בכי" (crying) — not in dictionary; starts with ב which should be b
        val result = HebrewTransliterator.transliterate("בכי")
        assertTrue("Expected result to start with 'B'", result.startsWith("B"))
    }

    @Test
    fun `medial bet is v`() {
        val result = HebrewTransliterator.transliterate("אבג")
        assertTrue("Expected 'v' for medial ב", result.contains("v"))
    }

    @Test
    fun `shin maps to sh`() {
        val result = HebrewTransliterator.transliterate("שן")
        assertTrue(result.startsWith("Sh"))
    }

    @Test
    fun `ayin maps to empty string`() {
        val result = HebrewTransliterator.transliterate("עם")
        assertFalse("ע should produce no character", result.contains("'"))
    }

    @Test
    fun `result is capitalized`() {
        val result = HebrewTransliterator.transliterate("דלת")
        assertTrue("First char should be uppercase", result[0].isUpperCase())
    }

    // --- Nikud stripping ---

    @Test
    fun `nikud is stripped before transliteration`() {
        val withNikud = "דָּוִד"
        assertEquals("David", HebrewTransliterator.transliterate(withNikud))
    }

    // --- Non-Hebrew passthrough ---

    @Test
    fun `latin word in mixed name is unchanged`() {
        val result = HebrewTransliterator.transliterate("David כהן")
        assertTrue("Latin word should be unchanged", result.contains("David"))
        assertTrue("Hebrew word should be transliterated", result.contains("Cohen"))
    }

    // --- Edge cases ---

    @Test
    fun `empty string returns empty`() {
        assertEquals("", HebrewTransliterator.transliterate(""))
    }

    @Test
    fun `contains hebrew detection`() {
        assertTrue(HebrewTransliterator.containsHebrew("שלום"))
        assertFalse(HebrewTransliterator.containsHebrew("Hello"))
        assertTrue(HebrewTransliterator.containsHebrew("Hello שלום"))
    }

    // --- Fallback rules: success cases (rules added in v5.0) ---

    @Test
    fun `fallback nora via vav-as-vowel and final he`() {
        // ו between two consonants → "o"; final ה → "a"
        assertEquals("Nora", HebrewTransliterator.transliterate("נורה"))
    }

    @Test
    fun `fallback shira via yod-as-vowel and final he`() {
        assertEquals("Shira", HebrewTransliterator.transliterate("שירה"))
    }

    @Test
    fun `fallback hila via yod-as-vowel and final he`() {
        assertEquals("Hila", HebrewTransliterator.transliterate("הילה"))
    }

    @Test
    fun `fallback bita via initial bet and yod-as-vowel`() {
        assertEquals("Bita", HebrewTransliterator.transliterate("ביתה"))
    }

    @Test
    fun `vav at word start is v not o`() {
        // ו at position 0 has no previous Hebrew char → charMap gives "v", not the vowel "o"
        val result = HebrewTransliterator.transliterate("ולי")
        assertTrue("ו at word start must map to V", result.startsWith("V"))
        assertTrue("final י must map to i", result.endsWith("i"))
    }

    @Test
    fun `yod at word start is y not i`() {
        // י at position 0 has no previous Hebrew char → charMap gives "y", not the vowel "i"
        val result = HebrewTransliterator.transliterate("יסוד")
        assertTrue("Initial י must map to Y", result.startsWith("Y"))
    }

    // --- New dictionary entries smoke tests ---

    @Test
    fun `kfir is in dictionary`() {
        assertEquals("Kfir", HebrewTransliterator.transliterate("כפיר"))
    }

    @Test
    fun `hadar is in dictionary`() {
        assertEquals("Hadar", HebrewTransliterator.transliterate("הדר"))
    }

    @Test
    fun `linoy is in dictionary`() {
        assertEquals("Linoy", HebrewTransliterator.transliterate("לינוי"))
    }

    @Test
    fun `harel is in dictionary`() {
        assertEquals("Harel", HebrewTransliterator.transliterate("הראל"))
    }

    @Test
    fun `sharon is in dictionary`() {
        assertEquals("Sharon", HebrewTransliterator.transliterate("שרון"))
    }

    @Test
    fun `multi-word with new dictionary entries`() {
        assertEquals("Kfir Cohen", HebrewTransliterator.transliterate("כפיר כהן"))
    }

    // --- Known-gap tests: document cases where fallback is imperfect ---

    @Test
    fun `geresh name is not Jackie (known gap)`() {
        // ג׳ (gimel + geresh) represents the J sound but the fallback cannot handle it.
        // This test documents the known limitation — it should NOT return "Jackie".
        val result = HebrewTransliterator.transliterate("ג'קי")
        assertFalse("Result must not be empty", result.isEmpty())
        assertTrue("Result must start with uppercase", result[0].isUpperCase())
        assertNotEquals("Known gap: geresh names cannot produce correct output", "Jackie", result)
    }

    @Test
    fun `long foreign name fallback produces non-empty capitalised result`() {
        val result = HebrewTransliterator.transliterate("פרנקנשטיין")
        assertFalse("Result must not be empty", result.isEmpty())
        assertTrue("Result must start with uppercase", result[0].isUpperCase())
    }

    @Test
    fun `all-ayin word returns original via ifEmpty fallback`() {
        // ע maps to empty string; when the entire result is empty, ifEmpty returns the original
        assertEquals("עע", HebrewTransliterator.transliterate("עע"))
    }

    // --- Edge-case robustness ---

    @Test
    fun `digits in mixed name pass through unchanged`() {
        val result = HebrewTransliterator.transliterate("חדר 5")
        assertTrue("Digit token must be preserved as-is", result.contains("5"))
    }
}
