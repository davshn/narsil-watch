package tech.davshn.narsilwatch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `is day from 6 to 18, as the other Narsil apps`() {
        assertEquals(false, isDay(5))
        assertEquals(true, isDay(6))
        assertEquals(true, isDay(17))
        assertEquals(false, isDay(18))
        assertEquals(false, isDay(0))
    }

    @Test
    fun `wears the night palette by night and the day one by day`() {
        assertEquals(Palette.NIGHT, Palette.at(hour = 22))
        assertEquals(Palette.DAY, Palette.at(hour = 12))
        assertEquals(0xFF16182A.toInt(), Palette.NIGHT.bg)
    }

    @Test
    fun `the day palette is a little lighter than the night, and still dark`() {
        val day = Palette.DAY.bg
        val night = Palette.NIGHT.bg
        for (shift in listOf(16, 8, 0)) {
            val d = (day shr shift) and 0xFF
            val n = (night shr shift) and 0xFF
            assertTrue("channel $shift lighter", d > n)
            assertTrue("channel $shift still dark", d < 0x48)
        }
    }

    @Test
    fun `picks Narsil's light drawing by day and the dark one by night`() {
        val base = "https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/etapa4"
        assertEquals("$base/base-claro.png", artVariant("$base/base-oscuro.png", day = true))
        assertEquals("$base/base-oscuro.png", artVariant("$base/base-claro.png", day = false))
        assertEquals("$base/base-oscuro.png", artVariant("$base/base-oscuro.png", day = false))
        assertEquals("$base/other.png", artVariant("$base/other.png", day = true))
    }
}
