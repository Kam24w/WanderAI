package com.vibra.wanderai.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberInputTest {

    @Test
    fun `cop accepts grouped and decorated input`() {
        assertEquals(80_000, NumberInput.parseCop("80000"))
        assertEquals(80_000, NumberInput.parseCop("80.000"))
        assertEquals(83_500, NumberInput.parseCop("$83.500 COP"))
        assertNull(NumberInput.parseCop(""))
        assertNull(NumberInput.parseCop("COP"))
    }

    @Test
    fun `km accepts dot or comma decimals and unit`() {
        assertEquals(2.5f, NumberInput.parseKm("2,5")!!, 0.0001f)
        assertEquals(2.35f, NumberInput.parseKm("2.35 km")!!, 0.0001f)
        assertEquals(3f, NumberInput.parseKm("3")!!, 0.0001f)
        assertNull(NumberInput.parseKm("2.3.4"))
        assertNull(NumberInput.parseKm("abc"))
        assertNull(NumberInput.parseKm(""))
    }

    @Test
    fun `sanitizers keep only what the keyboard field accepts`() {
        assertEquals("80000", NumberInput.sanitizeCop("80.000"))
        assertEquals("2.35", NumberInput.sanitizeKm("2,357"))
        assertEquals("0.5", NumberInput.sanitizeKm(".5"))
        assertEquals("12.51", NumberInput.sanitizeKm("12.5.1"))
    }

    @Test
    fun `km labels show hundredths only when typed`() {
        assertEquals("2.0 km", NumberInput.formatKm(2f))
        assertEquals("2.5 km", NumberInput.formatKm(2.5f))
        assertEquals("2.35 km", NumberInput.formatKm(2.35f))
        assertEquals("2", NumberInput.editableKm(2f))
        assertEquals("2.5", NumberInput.editableKm(2.5f))
        assertEquals("2.35", NumberInput.editableKm(2.35f))
    }
}
