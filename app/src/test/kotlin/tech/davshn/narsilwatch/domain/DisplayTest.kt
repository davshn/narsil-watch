package tech.davshn.narsilwatch.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTest {
    private val sessions = listOf(
        Session("a", "waiting", "TE NECESITA"),
        Session("b", "error", "ERROR"),
        Session("c", "working", "TRABAJANDO"),
        Session("d", "waiting", "TE NECESITA"),
    )

    @Test
    fun `counts the conversations that need you`() {
        assertEquals(2, needsYou(sessions))
        assertEquals(0, needsYou(emptyList()))
    }

    @Test
    fun `says how old the state is`() {
        assertEquals("ahora", ageLabel(at = 1000, nowSeconds = 1030))
        assertEquals("hace 12 min", ageLabel(at = 1000, nowSeconds = 1000 + 12 * 60))
        assertEquals("hace 3 h", ageLabel(at = 1000, nowSeconds = 1000 + 3 * 3600 + 59))
        assertEquals("hace 2 d", ageLabel(at = 1000, nowSeconds = 1000 + 2 * 86400))
    }

    @Test
    fun `says when a limit resets`() {
        assertEquals("", resetLabel(null, nowSeconds = 0))
        assertEquals("reinicia en 45 min", resetLabel(45 * 60L, nowSeconds = 0))
        assertEquals("reinicia en 2 h 05 min", resetLabel(2 * 3600L + 5 * 60, nowSeconds = 0))
        assertEquals("reinicia en 3 d 4 h", resetLabel(3 * 86400L + 4 * 3600, nowSeconds = 0))
        assertEquals("reiniciado", resetLabel(10, nowSeconds = 20))
    }

    @Test
    fun `colors a status as Narsil does`() {
        assertEquals(0xFFF5B400.toInt(), statusColor("waiting"))
        assertEquals(0xFFF0435A.toInt(), statusColor("error"))
        assertEquals(0xFF8A90A6.toInt(), statusColor("unknown"))
    }

    @Test
    fun `reads a level color, and falls back to dim on noise`() {
        assertEquals(0xFF22B573.toInt(), parseColor("#22B573"))
        assertEquals(0xFF9A98AD.toInt(), parseColor("rojo"))
    }
}
