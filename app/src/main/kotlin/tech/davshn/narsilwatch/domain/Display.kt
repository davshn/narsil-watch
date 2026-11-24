package tech.davshn.narsilwatch.domain

// How the watch says things, in Narsil's words and colors (overlay.html,
// dark theme). Pure functions, so the tile, the complication and the app
// read the same.

// Day from 6 to 18, as Narsil for Android and Narsil Skin's auto theme.
fun isDay(hour: Int): Boolean = hour in 6..17

data class Palette(val bg: Int, val text: Int, val dim: Int, val track: Int, val accent: Int) {
    companion object {
        val NIGHT = Palette(
            bg = 0xFF16182A.toInt(),
            text = 0xFFECEBF3.toInt(),
            dim = 0xFF9A98AD.toInt(),
            track = 0xFF2A2D45.toInt(),
            accent = 0xFF7C6CF0.toInt(),
        )

        // Only a step lighter than the night: the watch's OLED screen spends
        // its battery on every lit pixel.
        val DAY = Palette(
            bg = 0xFF2A2D47.toInt(),
            text = 0xFFF3F2F8.toInt(),
            dim = 0xFFABA9BE.toInt(),
            track = 0xFF3E4266.toInt(),
            accent = 0xFF8A7CF4.toInt(),
        )

        fun at(hour: Int): Palette = if (isDay(hour)) DAY else NIGHT
    }
}

// Narsil's drawing for the time of day: by day the light one (white Narsil).
// The PC always publishes the dark one.
fun artVariant(url: String, day: Boolean): String =
    ART_THEME.replace(url, if (day) "-claro.png" else "-oscuro.png")

private val ART_THEME = Regex("-(claro|oscuro)\\.png$")

private val STATUS_COLORS = mapOf(
    "working" to 0xFF3B8BFF.toInt(),
    "idle" to 0xFF8A90A6.toInt(),
    "waiting" to 0xFFF5B400.toInt(),
    "done" to 0xFF22B573.toInt(),
    "error" to 0xFFF0435A.toInt(),
)

fun statusColor(status: String): Int = STATUS_COLORS[status] ?: STATUS_COLORS.getValue("idle")

// A #RRGGBB color as the PC sends it; dim for anything else.
fun parseColor(hex: String): Int {
    val digits = hex.removePrefix("#")
    if (digits.length != 6) return Palette.NIGHT.dim
    val rgb = digits.toLongOrNull(16) ?: return Palette.NIGHT.dim
    return (0xFF000000L or rgb).toInt()
}

// The conversations waiting on you: a question, a plan or a permission.
fun needsYou(sessions: List<Session>): Int = sessions.count { it.status == "waiting" }

// How old the state is: the PC publishes only while you are away.
fun ageLabel(at: Long, nowSeconds: Long): String {
    val minutes = (nowSeconds - at).coerceAtLeast(0) / 60
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (24 * 60)} d"
    }
}

fun resetLabel(resetsAt: Long?, nowSeconds: Long): String {
    if (resetsAt == null) return ""
    if (resetsAt <= nowSeconds) return "reiniciado"
    val minutes = (resetsAt - nowSeconds) / 60
    return when {
        minutes < 60 -> "reinicia en $minutes min"
        minutes < 24 * 60 -> "reinicia en ${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')} min"
        else -> "reinicia en ${minutes / (24 * 60)} d ${(minutes % (24 * 60)) / 60} h"
    }
}
