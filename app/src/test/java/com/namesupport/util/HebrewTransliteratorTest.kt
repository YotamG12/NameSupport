package com.namesupport.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
