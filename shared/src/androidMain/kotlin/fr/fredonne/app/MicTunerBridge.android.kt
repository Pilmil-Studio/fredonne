package fr.fredonne.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.concurrent.thread

@Composable
actual fun rememberMicPermissionState(onResult: (Boolean) -> Unit): MicPermissionState {
    if (LocalInspectionMode.current || androidx.activity.compose.LocalActivityResultRegistryOwner.current == null) {
        return remember { MicPermissionState(granted = false, request = {}, openSettings = {}) }
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onResult)

    fun checkGranted(): Boolean = runCatching {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    var granted by remember { mutableStateOf(checkGranted()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = checkGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok || checkGranted()
        callback(granted)
    }

    return MicPermissionState(
        granted = granted,
        request = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        openSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        },
    )
}

actual fun createLiveMicSession(): LiveMicSession? = AndroidLiveMicSession()

private class AndroidLiveMicSession : LiveMicSession {
    @Volatile
    private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    override fun start(sampleRate: Int, onSamples: (FloatArray) -> Unit) {
        stop()
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRate / 5 * 2)
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
        }.getOrNull() ?: return
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        running = true
        runCatching { record.startRecording() }.onFailure {
            running = false
            record.release()
            return
        }
        val chunkSize = (sampleRate / 10).coerceAtLeast(1024)
        worker = thread(name = "fredonne-mic", isDaemon = true) {
            val shortBuf = ShortArray(chunkSize)
            try {
                while (running) {
                    val read = record.read(shortBuf, 0, shortBuf.size)
                    if (read > 0) {
                        val floats = FloatArray(read) { i -> shortBuf[i] / 32768f }
                        onSamples(floats)
                    }
                }
            } finally {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
        }
    }

    override fun stop() {
        running = false
        worker?.join(300)
        worker = null
    }
}
