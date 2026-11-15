@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.abs

@Composable
actual fun rememberMicPermissionState(onResult: (Boolean) -> Unit): MicPermissionState {
    val callback by rememberUpdatedState(onResult)

    fun isMicGranted(): Boolean =
        AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted

    var granted by remember { mutableStateOf(isMicGranted()) }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            granted = isMicGranted()
        }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    return MicPermissionState(
        granted = granted,
        request = {
            AVAudioSession.sharedInstance().requestRecordPermission { allowed ->
                dispatch_async(dispatch_get_main_queue()) {
                    granted = allowed
                    callback(allowed)
                }
            }
        },
        openSettings = {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(
                    url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        },
    )
}

actual fun createLiveMicSession(): LiveMicSession? = IosLiveMicSession()

private class IosLiveMicSession : LiveMicSession {
    private var engine: AVAudioEngine? = null

    override fun start(sampleRate: Int, onSamples: (FloatArray) -> Unit) {
        stop()

        val session = AVAudioSession.sharedInstance()
        if (session.recordPermission != AVAudioSessionRecordPermissionGranted) return

        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeMeasurement,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null,
        )
        session.setActive(true, error = null)

        val audioEngine = AVAudioEngine()
        val inputNode = audioEngine.inputNode
        val inputFormat = inputNode.outputFormatForBus(0u)
        val inputSampleRate = inputFormat.sampleRate
        val inputChannels = inputFormat.channelCount.toInt()
        if (inputSampleRate <= 0.0 || inputChannels <= 0) return

        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 4096u,
            format = inputFormat,
        ) { buffer: AVAudioPCMBuffer?, _ ->
            val pcm = buffer ?: return@installTapOnBus
            val frameLength = pcm.frameLength.toInt()
            if (frameLength <= 0) return@installTapOnBus

            val channelData = pcm.floatChannelData ?: return@installTapOnBus
            val mono = if (inputChannels == 1) {
                val ch0 = channelData[0] ?: return@installTapOnBus
                FloatArray(frameLength) { i -> ch0[i] }
            } else {
                FloatArray(frameLength).also { out ->
                    val scale = 1f / inputChannels.toFloat()
                    for (ch in 0 until inputChannels) {
                        val ptr = channelData[ch] ?: continue
                        for (i in 0 until frameLength) {
                            out[i] += ptr[i] * scale
                        }
                    }
                }
            }

            val resampled = resampleToTarget(mono, inputSampleRate, sampleRate)
            if (resampled.isNotEmpty()) {
                onSamples(resampled)
            }
        }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            runCatching { inputNode.removeTapOnBus(0u) }
            return
        }
        engine = audioEngine
    }

    override fun stop() {
        val current = engine ?: return
        engine = null
        runCatching { current.inputNode.removeTapOnBus(0u) }
        runCatching { current.stop() }
    }

    private fun resampleToTarget(
        input: FloatArray,
        sourceRate: Double,
        targetRate: Int,
    ): FloatArray {
        if (input.isEmpty() || sourceRate <= 0.0 || targetRate <= 0) return FloatArray(0)
        if (abs(sourceRate - targetRate.toDouble()) < 1.0) return input
        val ratio = sourceRate / targetRate.toDouble()
        val outLength = (input.size / ratio).toInt().coerceAtLeast(1)
        return FloatArray(outLength) { i ->
            val srcPos = i * ratio
            val idx0 = srcPos.toInt().coerceIn(0, input.lastIndex)
            val idx1 = (idx0 + 1).coerceIn(0, input.lastIndex)
            val frac = (srcPos - idx0).toFloat()
            input[idx0] * (1f - frac) + input[idx1] * frac
        }
    }
}
