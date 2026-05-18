package com.namesupport.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewTransliteratorTest {

    // ── containsHebrew ────────────────────────────────────────────────────────

    @Test
    fun `containsHebrew returns true for pure Hebrew`() {
        assertTrue(HebrewTransliterator.containsHebrew("יוסי"))
    }

    @Test
    fun `containsHebrew returns true for mixed Hebrew-Latin`() {
        assertTrue(HebrewTransliterator.containsHebrew("David כהן"))
    }

    @Test
    fun `containsHebrew returns false for pure Latin`() {
        assertFalse(HebrewTransliterator.containsHebrew("John Smith"))
    }

    @Test
    fun `containsHebrew returns false for empty string`() {
        assertFalse(HebrewTransliterator.containsHebrew(""))
    }

    @Test
    fun `containsHebrew returns false for digits and symbols`() {
        assertFalse(HebrewTransliterator.containsHebrew("123 !@#"))
    }

    // ── Dictionary lookups ────────────────────────────────────────────────────

    @Test
    fun `dictionary - Yossi`() {
        assertEquals("Yossi", HebrewTransliterator.transliterate("יוסי"))
    }

    @Test
    fun `dictionary - Cohen`() {
        assertEquals("Cohen", HebrewTransliterator.transliterate("כהן"))
    }

    @Test
    fun `dictionary - Moshe`() {
        assertEquals("Moshe", HebrewTransliterator.transliterate("משה"))
    }

    @Test
    fun `dictionary - Sarah`() {
        assertEquals("Sarah", HebrewTransliterator.transliterate("שרה"))
    }

    @Test
    fun `dictionary - David`() {
        assertEquals("David", HebrewTransliterator.transliterate("דוד"))
    }

    @Test
    fun `dictionary - Noa`() {
        assertEquals("Noa", HebrewTransliterator.transliterate("נועה"))
    }

    // ── Full name (first + last) ───────────────────────────────────────────────

    @Test
    fun `full name Yossi Cohen`() {
        assertEquals("Yossi Cohen", HebrewTransliterator.transliterate("יוסי כהן"))
    }

    @Test
    fun `full name Moshe Shapira`() {
        assertEquals("Moshe Shapira", HebrewTransliterator.transliterate("משה שפירא"))
    }

    @Test
    fun `full name Sarah Levi`() {
        assertEquals("Sarah Levi", HebrewTransliterator.transliterate("שרה לוי"))
    }

    @Test
    fun `full name Rachel Peretz`() {
        assertEquals("Rachel Peretz", HebrewTransliterator.transliterate("רחל פרץ"))
    }

    // ── Mixed Hebrew + Latin ─────────────────────────────────────────────────

    @Test
    fun `latin word is preserved unchanged`() {
        val result = HebrewTransliterator.transliterate("David כהן")
        assertEquals("David Cohen", result)
    }

    @Test
    fun `latin first name with Hebrew last name`() {
        val result = HebrewTransliterator.transliterate("John לוי")
        assertEquals("John Levi", result)
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `empty string returns empty`() {
        assertEquals("", HebrewTransliterator.transliterate(""))
    }

    @Test
    fun `whitespace-only string returns empty`() {
        assertEquals("", HebrewTransliterator.transliterate("   "))
    }

    @Test
    fun `extra spaces between words are collapsed`() {
        val result = HebrewTransliterator.transliterate("יוסי  כהן")
        assertEquals("Yossi Cohen", result)
    }

    // ── Character-level fallback rules ────────────────────────────────────────

    @Test
    fun `bet (ב) is B at word start`() {
        // בית = house, not in dictionary → tests the initial-position rule
        val result = HebrewTransliterator.transliterate("בית")
        assertTrue("Expected word to start with 'B', got: $result", result.startsWith("B"))
    }

    @Test
    fun `kaf (כ) is K at word start`() {
        // כלב = dog, not in dictionary
        val result = HebrewTransliterator.transliterate("כלב")
        assertTrue("Expected word to start with 'K', got: $result", result.startsWith("K"))
    }

    @Test
    fun `peh (פ) is P at word start`() {
        // פרח = flower, not in dictionary
        val result = HebrewTransliterator.transliterate("פרח")
        assertTrue("Expected word to start with 'P', got: $result", result.startsWith("P"))
    }

    @Test
    fun `shin (ש) maps to Sh`() {
        // שמש = sun, not in dictionary → tests ש→sh
        val result = HebrewTransliterator.transliterate("שמש")
        assertTrue("Expected 'Sh' at start, got: $result", result.startsWith("Sh"))
    }

    @Test
    fun `chet (ח) maps to ch`() {
        // חם = hot, not in dictionary → tests ח→ch
        val result = HebrewTransliterator.transliterate("חם")
        assertTrue("Expected 'Ch' at start, got: $result", result.startsWith("Ch"))
    }

    @Test
    fun `result is always capitalised`() {
        val result = HebrewTransliterator.transliterate("בית")
        assertTrue("First letter should be uppercase", result[0].isUpperCase())
    }

    // ── Nikud stripping ───────────────────────────────────────────────────────

    @Test
    fun `nikud is stripped before lookup`() {
        // שָׁלוֹם with full nikud should still match שלום in dictionary
        val result = HebrewTransliterator.transliterate("שָׁלוֹם")
        assertEquals("Shalom", result)
    }

    // ── Relationship words (v3) ───────────────────────────────────────────────

    @Test
    fun `dictionary - Abba (father)`() {
        assertEquals("Abba", HebrewTransliterator.transliterate("אבא"))
    }

    @Test
    fun `dictionary - Ima (mother)`() {
        assertEquals("Ima", HebrewTransliterator.transliterate("אמא"))
    }

    @Test
    fun `dictionary - Saba (grandfather)`() {
        assertEquals("Saba", HebrewTransliterator.transliterate("סבא"))
    }

    @Test
    fun `dictionary - Savta (grandmother)`() {
        assertEquals("Savta", HebrewTransliterator.transliterate("סבתא"))
    }

    // ── dictionaryLookup public API ───────────────────────────────────────────

    @Test
    fun `dictionaryLookup returns known word`() {
        assertEquals("Sarah", HebrewTransliterator.dictionaryLookup("שרה"))
    }

    @Test
    fun `dictionaryLookup returns null for unknown word`() {
        assertNull(HebrewTransliterator.dictionaryLookup("בלבל"))
    }

    @Test
    fun `dictionaryLookup strips nikud before lookup`() {
        assertEquals("Shalom", HebrewTransliterator.dictionaryLookup("שָׁלוֹם"))
    }
}
