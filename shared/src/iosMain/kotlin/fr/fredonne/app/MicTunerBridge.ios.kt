package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMicPermissionState(onResult: (Boolean) -> Unit): MicPermissionState =
    remember { MicPermissionState(granted = false, request = { onResult(false) }, openSettings = {}) }

actual fun createLiveMicSession(): LiveMicSession? = null
