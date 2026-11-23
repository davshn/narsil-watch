package tech.davshn.narsilwatch.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.davshn.narsilwatch.domain.NarsilState

sealed interface StatusResult {
    data class Ok(val state: NarsilState) : StatusResult

    // the topic holds no state yet, or only noise
    data object Empty : StatusResult

    // sealed, and the watch has no key or the wrong one
    data object NeedsKey : StatusResult
}

// One ntfy poll answer (a JSON message per line) turned into Narsil's state.
object StatusParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(ntfyLines: String, key: String?): StatusResult {
        val message = ntfyLines.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> objectOf(line) }
            .filter { it["event"]?.jsonPrimitive?.content == "message" }
            .lastOrNull()
            ?.get("message")?.jsonPrimitive?.content
            ?: return StatusResult.Empty
        val body = if (message.startsWith(Seal.PREFIX)) {
            val inside = key?.let { Seal.unseal(it, message) } ?: return StatusResult.NeedsKey
            // what goes inside the seal is { message } (sealedPayload)
            objectOf(inside)?.get("message")?.jsonPrimitive?.content ?: return StatusResult.Empty
        } else {
            message
        }
        return try {
            StatusResult.Ok(json.decodeFromString<NarsilState>(body))
        } catch (_: SerializationException) {
            StatusResult.Empty
        } catch (_: IllegalArgumentException) {
            StatusResult.Empty
        }
    }

    private fun objectOf(text: String): JsonObject? = try {
        json.parseToJsonElement(text).jsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
