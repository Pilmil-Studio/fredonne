package fr.fredonne.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel

/**
 * Collecte les échantillons du micro sans tampon partagé : le fil audio dépose une copie de chaque
 * bloc dans un canal, et un seul coroutine (hors fil principal) est propriétaire du tampon.
 */
internal class HumCapture(scope: CoroutineScope, private val maxSamples: Int = 16_000 * 20) {
    private val chunks = Channel<FloatArray>(Channel.UNLIMITED)

    private val result = scope.async(Dispatchers.Default) {
        val buffer = FloatArray(maxSamples)
        var size = 0
        for (chunk in chunks) {
            val n = minOf(chunk.size, maxSamples - size)
            chunk.copyInto(buffer, destinationOffset = size, endIndex = n)
            size += n
        }
        buffer.copyOf(size)
    }

    /** Appelable depuis n'importe quel fil. La copie évite que le fil audio réutilise le bloc. */
    fun offer(chunk: FloatArray) {
        chunks.trySend(chunk.copyOf())
    }

    suspend fun finish(): FloatArray {
        chunks.close()
        return result.await()
    }

    fun cancel() {
        chunks.close()
        result.cancel()
    }
}
