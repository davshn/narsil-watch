package tech.davshn.narsilwatch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The vector comes from Narsil's own hooks/seal.ts, with a fixed nonce
// (100..115), so the watch opens exactly what the PC seals.
class SealTest {
    private val key = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8" // gitleaks:allow (fixed test key)
    private val sealed =
        "narsil1:ZGVmZ2hpamtsbW5vcHFyc8j7TQ3D9UDP2cUgLgqhOn-GJYgtFgAwi_uAfzv-eYe_ofBymqjmum6a9XfCbMR8O3gwU49qfWlbc0bD367k6T8jWpBs2b-wJ62sbDwNDOCpzRutwXQ1X5ZfFlWYh3qRyLDh5ysEz5gaahKydkXtf3KyIDmupoQf9z7Hy1ycRuYFG3G5VPvVW22zatwmctg"
    private val plain =
        """{"message":"{\"xp\":\"Nivel 3\",\"sessions\":[{\"title\":\"web\",\"status\":\"error\",\"label\":\"ERROR\"}]}"}"""

    @Test
    fun `opens what Narsil seals`() {
        assertEquals(plain, Seal.unseal(key, sealed))
    }

    @Test
    fun `another key opens nothing`() {
        assertNull(Seal.unseal("HwAeHRwbGhkYFxYVFBMSERAPDg0MCwoJCAcGBQQDAgE", sealed)) // gitleaks:allow (fixed test key)
    }

    @Test
    fun `a tampered message opens nothing`() {
        val body = sealed.removePrefix(Seal.PREFIX)
        val flipped = body.substring(0, 30) + (if (body[30] == 'A') 'B' else 'A') + body.substring(31)
        assertNull(Seal.unseal(key, Seal.PREFIX + flipped))
    }

    @Test
    fun `what is not sealed opens nothing`() {
        assertNull(Seal.unseal(key, "no sellado"))
        assertNull(Seal.unseal("not base64!", sealed))
    }
}
