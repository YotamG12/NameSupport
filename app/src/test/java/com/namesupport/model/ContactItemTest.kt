package com.namesupport.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactItemTest {

    @Test
    fun `two items with same fields are equal`() {
        val a = ContactItem(1L, "יוסי כהן", "Yossi Cohen")
        val b = ContactItem(1L, "יוסי כהן", "Yossi Cohen")
        assertEquals(a, b)
    }

    @Test
    fun `items with different IDs are not equal`() {
        val a = ContactItem(1L, "יוסי כהן", "Yossi Cohen")
        val b = ContactItem(2L, "יוסי כהן", "Yossi Cohen")
        assertNotEquals(a, b)
    }

    @Test
    fun `copy with new suggestion produces different item`() {
        val original = ContactItem(1L, "יוסי", "Yossi")
        val modified = original.copy(suggestion = "Jossi")
        assertNotEquals(original, modified)
        assertEquals("Jossi", modified.suggestion)
        assertEquals(original.id, modified.id)
        assertEquals(original.displayName, modified.displayName)
    }

    @Test
    fun `toString contains all fields`() {
        val item = ContactItem(42L, "שרה", "Sarah")
        val str = item.toString()
        assertTrue(str.contains("42"))
        assertTrue(str.contains("שרה"))
        assertTrue(str.contains("Sarah"))
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = ContactItem(5L, "דוד", "David")
        val b = ContactItem(5L, "דוד", "David")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // Needed because JUnit is on the classpath, not kotlin-test
    private fun assertTrue(msg: String, value: Boolean) {
        if (!value) throw AssertionError(msg)
    }
}
