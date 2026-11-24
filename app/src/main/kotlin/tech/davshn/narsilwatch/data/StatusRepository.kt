package tech.davshn.narsilwatch.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.davshn.narsilwatch.domain.NarsilState
import tech.davshn.narsilwatch.domain.artVariant

// Where the watch keeps the topic, the key, and the last state it read, so
// the tile and the complication draw at once, offline too.
class StatusRepository(context: Context) {
    val appContext: Context = context.applicationContext
    private val prefs = appContext.getSharedPreferences("narsil", Context.MODE_PRIVATE)
    private fun artFile(day: Boolean) = File(appContext.filesDir, if (day) "art-claro.png" else "art-oscuro.png")

    var topic: String
        get() = prefs.getString(TOPIC, "").orEmpty()
        set(value) = prefs.edit().putString(TOPIC, value.trim()).apply()

    var key: String
        get() = prefs.getString(KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY, value.trim()).apply()

    val isConfigured: Boolean get() = topic.isNotEmpty()

    // The topic the PC publishes its state on: the alerts' topic + "-estado".
    val statusTopic: String
        get() = topic.removeSuffix(STATUS_SUFFIX) + STATUS_SUFFIX

    // The last state read, and when the watch last asked (ms).
    fun cached(): NarsilState? = (StatusParser.parse(prefs.getString(RAW, "").orEmpty(), key.ifEmpty { null }) as? StatusResult.Ok)?.state

    val checkedAt: Long get() = prefs.getLong(CHECKED_AT, 0)

    // Narsil's drawing for the time of day; the other one until it arrives.
    fun art(day: Boolean): ByteArray? =
        (artFile(day).takeIf { it.exists() } ?: artFile(!day).takeIf { it.exists() })?.readBytes()

    // Asks ntfy.sh for the latest state; keeps it when there is one.
    suspend fun refresh(): StatusResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext StatusResult.Empty
        val url = "$NTFY/${URLEncoder.encode(statusTopic, "UTF-8")}/json?poll=1&since=latest"
        val lines = try {
            get(url, MAX_STATE)?.toString(Charsets.UTF_8)
        } catch (_: IOException) {
            null
        } ?: return@withContext cached()?.let { StatusResult.Ok(it) } ?: StatusResult.Empty
        val result = StatusParser.parse(lines, key.ifEmpty { null })
        prefs.edit().putLong(CHECKED_AT, System.currentTimeMillis()).apply()
        if (result is StatusResult.Ok) {
            prefs.edit().putString(RAW, lines).apply()
            result.state.art?.let { fetchArt(it) }
        }
        result
    }

    // Narsil's art, only from the repository that publishes it: whoever knows
    // an unsealed topic could write to it. Both drawings, dark and light, so
    // the watch turns at 6 and 18 with no network.
    private fun fetchArt(url: String) {
        if (!url.startsWith(ART_ORIGIN)) return
        for (day in listOf(false, true)) {
            val variant = artVariant(url, day)
            val urlKey = if (day) ART_URL_DAY else ART_URL_NIGHT
            if (variant == prefs.getString(urlKey, null)) continue
            val bytes = try {
                get(variant, MAX_ART)
            } catch (_: IOException) {
                null
            } ?: continue
            artFile(day).writeBytes(bytes)
            prefs.edit().putString(urlKey, variant).apply()
        }
        // The single drawing older versions kept.
        File(appContext.filesDir, "art.png").delete()
        prefs.edit().remove("artUrl").apply()
    }

    private fun get(url: String, limit: Int): ByteArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT
        conn.readTimeout = TIMEOUT
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.use { input ->
                val bytes = input.readNBytesCompat(limit + 1)
                if (bytes.size > limit) null else bytes
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() < n) {
            val read = read(buf, 0, minOf(buf.size, n - out.size()))
            if (read < 0) break
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val NTFY = "https://ntfy.sh"
        const val STATUS_SUFFIX = "-estado"
        const val ART_ORIGIN = "https://raw.githubusercontent.com/davshn/narsil-pet/"
        const val TIMEOUT = 10_000
        const val MAX_STATE = 64 * 1024
        const val MAX_ART = 256 * 1024
        const val TOPIC = "topic"
        const val KEY = "key"
        const val RAW = "raw"
        const val CHECKED_AT = "checkedAt"
        const val ART_URL_NIGHT = "artUrlNight"
        const val ART_URL_DAY = "artUrlDay"
    }
}
