package tech.davshn.narsilwatch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.davshn.narsilwatch.domain.Limit
import tech.davshn.narsilwatch.domain.Session

class StatusParserTest {
    private val state =
        """{"xp":"⭐ Nivel 3 · 40/300 XP","art":"https://x/etapa3/error-oscuro.png","at":1763900000,"cacheMinutes":5,""" +
            """"usage":[],"limits":[{"key":"fiveHour","label":"5 horas","percent":62.4,"color":"#F5B400","resetsAt":1763918000},""" +
            """{"key":"week","label":"Semana","percent":90,"color":"#F0435A","resetsAt":null}],""" +
            """"sessions":[{"title":"web","status":"error","label":"ERROR"},{"title":"landing","status":"waiting","label":"TE NECESITA"}]}"""

    private fun ntfyLine(message: String) =
        """{"id":"a1","time":1763900001,"event":"message","topic":"narsil-x-estado","message":${quote(message)},"priority":1}"""

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    @Test
    fun `reads the state as the PC sends it in the clear`() {
        val result = StatusParser.parse(ntfyLine(state), key = null)
        assertTrue(result is StatusResult.Ok)
        val s = (result as StatusResult.Ok).state
        assertEquals(1763900000L, s.at)
        assertEquals("⭐ Nivel 3 · 40/300 XP", s.xp)
        assertEquals(Limit("fiveHour", "5 horas", 62.4, "#F5B400", 1763918000L), s.limits[0])
        assertEquals(null, s.limits[1].resetsAt)
        assertEquals(listOf(Session("web", "error", "ERROR"), Session("landing", "waiting", "TE NECESITA")), s.sessions)
    }

    @Test
    fun `reads a state from before the watch fields, with no limits or sessions`() {
        val old = """{"xp":"⭐ Nivel 1 · 0/100 XP","art":"a","at":5,"usage":[]}"""
        val s = (StatusParser.parse(ntfyLine(old), key = null) as StatusResult.Ok).state
        assertEquals(emptyList<Limit>(), s.limits)
        assertEquals(emptyList<Session>(), s.sessions)
    }

    @Test
    fun `a sealed state needs the key`() {
        val sealed = "narsil1:ZGVmZ2hpamtsbW5vcHFyc8j7TQ3D9UDP2cUgLgqhOn-GJYgtFgAwi_uAfzv-eYe_ofBymqjmum6a9XfCbMR8O3gwU49qfWlbc0bD367k6T8jWpBs2b-wJ62sbDwNDOCpzRutwXQ1X5ZfFlWYh3qRyLDh5ysEz5gaahKydkXtf3KyIDmupoQf9z7Hy1ycRuYFG3G5VPvVW22zatwmctg"
        assertEquals(StatusResult.NeedsKey, StatusParser.parse(ntfyLine(sealed), key = null))
        assertEquals(StatusResult.NeedsKey, StatusParser.parse(ntfyLine(sealed), key = "HwAeHRwbGhkYFxYVFBMSERAPDg0MCwoJCAcGBQQDAgE")) // gitleaks:allow (fixed test key)
        val opened = StatusParser.parse(ntfyLine(sealed), key = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8") as StatusResult.Ok // gitleaks:allow (fixed test key)
        assertEquals(listOf(Session("web", "error", "ERROR")), opened.state.sessions)
    }

    @Test
    fun `an empty topic or noise is no state yet`() {
        assertEquals(StatusResult.Empty, StatusParser.parse("", key = null))
        assertEquals(StatusResult.Empty, StatusParser.parse(ntfyLine("hola"), key = null))
    }
}
