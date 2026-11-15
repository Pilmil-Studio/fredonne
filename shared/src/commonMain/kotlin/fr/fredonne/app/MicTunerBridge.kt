package fr.fredonne.app

import androidx.compose.runtime.Composable

/** État de l'autorisation microphone et actions associées. */
data class MicPermissionState(
    val granted: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
expect fun rememberMicPermissionState(onResult: (Boolean) -> Unit = {}): MicPermissionState

/** Session de capture audio PCM mono locale pour l'accordeur et la dictée mélodique. */
interface LiveMicSession {
    fun start(sampleRate: Int = 16_000, onSamples: (FloatArray) -> Unit)
    fun stop()
}

expect fun createLiveMicSession(): LiveMicSession?
