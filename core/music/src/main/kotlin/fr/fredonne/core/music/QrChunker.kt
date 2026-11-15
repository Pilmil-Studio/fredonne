package fr.fredonne.core.music

/**
 * Découpage et réassemblage multi-QR (`FR2|<tag>:<index>/<total>|<morceau>`) pour les partitions
 * dépassant la capacité d'un seul QR code.
 *
 * Si le payload tient en un seul QR code (<= [maxSingleBytes]), il est retourné tel quel sans
 * surcouche `FR2|`, préservant ainsi l'interopérabilité directe (ex. ABC pur).
 */
object QrChunker {

    private const val PREFIX = "FR2|"
    const val MAX_CHUNKS = 16
    const val MAX_ASSEMBLED_BYTES = 16_384

    data class Chunk(
        val tag: String,
        val index: Int,
        val total: Int,
        val body: String,
    )

    sealed interface AssemblyResult {
        data class Complete(val payload: String) : AssemblyResult
        data class Partial(val collected: Int, val total: Int, val tag: String) : AssemblyResult
        data class Invalid(val reason: String) : AssemblyResult
    }

    fun split(
        payload: String,
        maxSingleBytes: Int = QrCodeEncoder.MAX_CAPACITY_BYTES,
    ): List<String> {
        if (payload.encodeToByteArray().size <= maxSingleBytes) {
            return listOf(payload)
        }
        val tag = shortTag(payload)
        // Garde une marge suffisante pour l'en-tête "FR2|<tag>:99/99|" (~16 octets)
        val maxBodyBytes = (maxSingleBytes - 18).coerceAtLeast(32)
        val bodies = splitUtf8Safe(payload, maxBodyBytes)
        val total = bodies.size
        return bodies.mapIndexed { idx, body ->
            "$PREFIX$tag:${idx + 1}/$total|$body"
        }
    }

    fun parseChunk(raw: String): Chunk? {
        if (!raw.startsWith(PREFIX)) return null
        val rest = raw.removePrefix(PREFIX)
        val pipeIdx = rest.indexOf('|')
        if (pipeIdx <= 0) return null
        val header = rest.substring(0, pipeIdx)
        val body = rest.substring(pipeIdx + 1)
        val colonIdx = header.indexOf(':')
        if (colonIdx <= 0) return null
        val tag = header.substring(0, colonIdx)
        val slashIdx = header.indexOf('/', colonIdx + 1)
        if (slashIdx <= colonIdx + 1) return null
        val index = header.substring(colonIdx + 1, slashIdx).toIntOrNull() ?: return null
        val total = header.substring(slashIdx + 1).toIntOrNull() ?: return null
        if (total !in 1..MAX_CHUNKS || index !in 1..total) return null
        return Chunk(tag = tag, index = index, total = total, body = body)
    }

    class Assembler {
        private var currentTag: String? = null
        private var expectedTotal: Int = 0
        private val parts = mutableMapOf<Int, String>()

        fun reset() {
            currentTag = null
            expectedTotal = 0
            parts.clear()
        }

        fun accept(raw: String): AssemblyResult {
            if (raw.length > MAX_ASSEMBLED_BYTES) {
                return AssemblyResult.Invalid("Payload QR trop volumineux")
            }
            if (!raw.startsWith(PREFIX)) {
                return AssemblyResult.Complete(raw.trim())
            }
            val chunk = parseChunk(raw)
                ?: return AssemblyResult.Invalid("Fragment QR FR2 invalide")

            if (currentTag != chunk.tag || expectedTotal != chunk.total) {
                currentTag = chunk.tag
                expectedTotal = chunk.total
                parts.clear()
            }
            parts[chunk.index] = chunk.body

            return if (parts.size == expectedTotal) {
                val totalBytes = parts.values.sumOf { it.length }
                if (totalBytes > MAX_ASSEMBLED_BYTES) {
                    reset()
                    return AssemblyResult.Invalid("Partition assemblée trop volumineuse")
                }
                val assembled = (1..expectedTotal).joinToString("") { idx -> parts[idx].orEmpty() }
                reset()
                AssemblyResult.Complete(assembled)
            } else {
                AssemblyResult.Partial(collected = parts.size, total = expectedTotal, tag = chunk.tag)
            }
        }
    }

    private fun splitUtf8Safe(text: String, maxBytes: Int): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val charStr = if (ch.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                i++
                "$ch${text[i]}"
            } else {
                ch.toString()
            }
            val charBytes = charStr.encodeToByteArray().size
            if (currentBytes + charBytes > maxBytes && current.isNotEmpty()) {
                result += current.toString()
                current.clear()
                currentBytes = 0
            }
            current.append(charStr)
            currentBytes += charBytes
            i++
        }
        if (current.isNotEmpty()) {
            result += current.toString()
        }
        return result
    }

    private fun shortTag(text: String): String {
        var hash = 0x811C9DC5.toInt()
        for (b in text.encodeToByteArray()) {
            hash = hash xor (b.toInt() and 0xFF)
            hash *= 0x01000193
        }
        val unsigned = hash.toLong() and 0xFFFFL
        return unsigned.toString(16).padStart(4, '0')
    }
}
