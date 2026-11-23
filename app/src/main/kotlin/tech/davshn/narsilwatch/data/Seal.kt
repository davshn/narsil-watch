package tech.davshn.narsilwatch.data

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Opens what Narsil seals with /narsil movil clave (hooks/seal.ts):
// HMAC-SHA256 in counter mode as the keystream, then a second HMAC over the
// nonce and the ciphertext (encrypt-then-MAC). "narsil1:" + base64url of
// nonce (16) | ciphertext | tag (32).
object Seal {
    const val PREFIX = "narsil1:"
    private const val NONCE = 16
    private const val BLOCK = 32

    fun unseal(key: String, sealed: String): String? {
        if (!sealed.startsWith(PREFIX)) return null
        return try {
            val bytes = fromBase64Url(sealed.removePrefix(PREFIX))
            if (bytes.size < NONCE + BLOCK) return null
            val k = fromBase64Url(key)
            val macKey = hmac(k, "mac".toByteArray())
            val tag = hmac(macKey, bytes.copyOfRange(0, bytes.size - BLOCK))
            if (!MessageDigest.isEqual(tag, bytes.copyOfRange(bytes.size - BLOCK, bytes.size))) return null
            val encKey = hmac(k, "enc".toByteArray())
            val nonce = bytes.copyOfRange(0, NONCE)
            String(xorStream(encKey, nonce, bytes.copyOfRange(NONCE, bytes.size - BLOCK)), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun fromBase64Url(text: String): ByteArray = Base64.getUrlDecoder().decode(text)

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun xorStream(encKey: ByteArray, nonce: ByteArray, bytes: ByteArray): ByteArray {
        val out = ByteArray(bytes.size)
        var block = 0
        while (block * BLOCK < bytes.size) {
            val counter = byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte())
            val stream = hmac(encKey, nonce + counter)
            for (j in 0 until BLOCK) {
                val i = block * BLOCK + j
                if (i >= bytes.size) break
                out[i] = (bytes[i].toInt() xor stream[j].toInt()).toByte()
            }
            block++
        }
        return out
    }
}
