@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package fr.fredonne.app

import fr.fredonne.core.music.Melody
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.NSTimer
import platform.Foundation.create
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

actual class TonePlayer {

    private var singlePlayer: AVAudioPlayer? = null
    private var melodyPlayer: AVAudioPlayer? = null
    private var progressTimer: NSTimer? = null
    private var playingFlag: Boolean = false
    private var melodyGeneration: Long = 0L
    private var singleNoteGeneration: Long = 0L

    actual fun isPlaying(): Boolean = playingFlag

    actual fun stop() {
        melodyGeneration++
        playingFlag = false
        progressTimer?.invalidate()
        progressTimer = null
        melodyPlayer?.stop()
        melodyPlayer = null
    }

    actual fun playSingleNote(
        midi: Int,
        durationMs: Int,
        timbre: InstrumentTimbre,
        referenceA4Hz: Double,
    ) {
        val gen = ++singleNoteGeneration
        val bgQueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
        dispatch_async(bgQueue) {
            val samples = ToneSynthesizer.synthesizeTone(
                freqHz = ToneSynthesizer.midiToFreq(midi, referenceA4Hz),
                durationMs = durationMs,
                timbre = timbre,
            )
            if (samples.isEmpty()) return@dispatch_async
            val wav = ToneSynthesizer.pcm16ToWav(samples)
            dispatch_async(dispatch_get_main_queue()) {
                if (gen != singleNoteGeneration) return@dispatch_async
                ensureAudioSession()
                val data = wav.toNSData() ?: return@dispatch_async
                val player = AVAudioPlayer(data = data, error = null) ?: return@dispatch_async
                singlePlayer = player
                player.prepareToPlay()
                player.play()
            }
        }
    }

    actual fun playMelody(
        melody: Melody,
        bpm: Int,
        onEventPlaying: (measureIndex: Int, eventIndex: Int) -> Unit,
        onFinished: () -> Unit,
        timbre: InstrumentTimbre,
        referenceA4Hz: Double,
    ) {
        stop()
        val gen = ++melodyGeneration
        playingFlag = true
        val bgQueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
        dispatch_async(bgQueue) {
            val (pcm, timings) = ToneSynthesizer.synthesizeMelody(
                melody = melody,
                bpm = bpm,
                timbre = timbre,
                referenceA4Hz = referenceA4Hz,
            )
            val wav = if (pcm.isNotEmpty() && timings.isNotEmpty()) {
                ToneSynthesizer.pcm16ToWav(pcm)
            } else {
                ByteArray(0)
            }
            dispatch_async(dispatch_get_main_queue()) {
                if (gen != melodyGeneration || !playingFlag) return@dispatch_async
                if (wav.isEmpty() || timings.isEmpty()) {
                    playingFlag = false
                    onFinished()
                    return@dispatch_async
                }
                ensureAudioSession()
                val data = wav.toNSData() ?: run {
                    playingFlag = false
                    onFinished()
                    return@dispatch_async
                }
                val player = AVAudioPlayer(data = data, error = null) ?: run {
                    playingFlag = false
                    onFinished()
                    return@dispatch_async
                }

                melodyPlayer = player
                player.prepareToPlay()
                var lastReportedIdx = -1
                player.play()

                progressTimer = NSTimer.scheduledTimerWithTimeInterval(
                    interval = 0.025,
                    repeats = true,
                ) { timer ->
                    if (gen != melodyGeneration || !playingFlag) {
                        timer?.invalidate()
                        return@scheduledTimerWithTimeInterval
                    }
                    val currentMs = (player.currentTime * 1000.0).toLong()
                    var activeIdx = 0
                    for (i in timings.indices) {
                        if (timings[i].startMs <= currentMs) activeIdx = i else break
                    }
                    if (activeIdx != lastReportedIdx) {
                        lastReportedIdx = activeIdx
                        val timing = timings[activeIdx]
                        onEventPlaying(timing.measureIndex, timing.eventIndex)
                    }
                    if (!player.isPlaying()) {
                        timer?.invalidate()
                        progressTimer = null
                        melodyPlayer = null
                        playingFlag = false
                        onFinished()
                    }
                }
            }
        }
    }

    private fun ensureAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    private fun ByteArray.toNSData(): NSData? {
        if (isEmpty()) return null
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
