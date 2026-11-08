package com.vibra.wanderai.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanSessionTest {

    @Test
    fun `tapping the same place twice creates two focus requests`() {
        val session = PlanSession()
        session.focusPlace("catedral")
        val first = session.mapFocus.value!!
        session.focusPlace("catedral")
        val second = session.mapFocus.value!!
        assertEquals("catedral", second.placeId)
        assertNotEquals(first, second)
    }

    @Test
    fun `consuming an old request keeps the newer one`() {
        val session = PlanSession()
        session.focusPlace("a")
        val old = session.mapFocus.value!!
        session.focusPlace("b")
        session.consumeFocus(old)
        assertEquals("b", session.mapFocus.value!!.placeId)
        session.consumeFocus(session.mapFocus.value!!)
        assertNull(session.mapFocus.value)
    }

    @Test
    fun `filters are shared and updated in place`() {
        val session = PlanSession()
        session.updateFilters { it.copy(budgetCop = 42_000) }
        assertEquals(42_000, session.filters.value.budgetCop)
    }
}
