package fr.fredonne.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import fr.fredonne.core.music.Melody
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

actual class TonePlayer {

    private val sampleRate = ToneSynthesizer.SAMPLE_RATE
    private val playing = AtomicBoolean(false)
    private val melodyGeneration = AtomicLong(0L)
    private val singleNoteGeneration = AtomicLong(0L)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeMelodyTrack: AudioTrack? = null

    private val melodyExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "fredonne-melody").apply { isDaemon = true }
    }

    private val singleNoteExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "fredonne-note").apply { isDaemon = true }
    }

    actual fun isPlaying(): Boolean = playing.get()

    actual fun stop() {
        melodyGeneration.incrementAndGet()
        playing.set(false)
        runCatching {
            activeMelodyTrack?.let { track ->
                track.pause()
                track.flush()
                track.stop()
            }
        }
    }

    actual fun playSingleNote(
        midi: Int,
        durationMs: Int,
        timbre: InstrumentTimbre,
        referenceA4Hz: Double,
    ) {
        val myGen = singleNoteGeneration.incrementAndGet()
        singleNoteExecutor.execute {
            if (singleNoteGeneration.get() != myGen) return@execute
            val samples = ToneSynthesizer.synthesizeTone(
                freqHz = ToneSynthesizer.midiToFreq(midi, referenceA4Hz),
                durationMs = durationMs,
                timbre = timbre,
            )
            if (singleNoteGeneration.get() != myGen) return@execute
            playPcm(samples)
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
        val myGen = melodyGeneration.incrementAndGet()
        playing.set(true)
        melodyExecutor.execute {
            if (melodyGeneration.get() != myGen || !playing.get()) {
                return@execute
            }

            var track: AudioTrack? = null
            try {
                val (pcm, timings) = ToneSynthesizer.synthesizeMelody(
                    melody = melody,
                    bpm = bpm,
                    timbre = timbre,
                    referenceA4Hz = referenceA4Hz,
                )
                if (melodyGeneration.get() != myGen || !playing.get()) {
                    return@execute
                }
                if (pcm.isEmpty() || timings.isEmpty()) {
                    return@execute
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                ).coerceAtLeast(4096)

                track = runCatching {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBuf * 2)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                }.getOrNull()
                activeMelodyTrack = track

                track?.play()
                val chunkSamples = (sampleRate / 50).coerceAtLeast(256) // Tranches de ~20 ms
                var offset = 0
                var lastReportedIdx = -1
                val startNano = System.nanoTime()

                fun reportProgress(currentMs: Long) {
                    var activeIdx = 0
                    for (i in timings.indices) {
                        if (timings[i].startMs <= currentMs) activeIdx = i else break
                    }
                    if (activeIdx != lastReportedIdx) {
                        lastReportedIdx = activeIdx
                        val t = timings[activeIdx]
                        val m = t.measureIndex
                        val e = t.eventIndex
                        mainHandler.post {
                            if (melodyGeneration.get() == myGen && playing.get()) {
                                onEventPlaying(m, e)
                            }
                        }
                    }
                }

                // Signale immédiatement la première note au démarrage
                reportProgress(0L)

                while (offset < pcm.size && playing.get() && melodyGeneration.get() == myGen) {
                    val count = minOf(chunkSamples, pcm.size - offset)
                    if (track != null) {
                        val written = track.write(pcm, offset, count, AudioTrack.WRITE_BLOCKING)
                        if (written < 0) break
                        val headSamples = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                        val currentMs = if (headSamples > 0L) {
                            (headSamples * 1000L) / sampleRate
                        } else {
                            (System.nanoTime() - startNano) / 1_000_000L
                        }
                        reportProgress(currentMs)
                    } else {
                        val currentMs = (offset.toLong() * 1000L) / sampleRate
                        reportProgress(currentMs)
                        Thread.sleep((count * 1000L) / sampleRate)
                    }
                    offset += count
                }

                // Attend que le tampon matériel finisse de jouer les derniers échantillons tout en mettant à jour le curseur
                if (track != null && playing.get() && melodyGeneration.get() == myGen) {
                    val totalDurationMs = (pcm.size.toLong() * 1000L) / sampleRate
                    var safetyLoops = 0
                    while (playing.get() && melodyGeneration.get() == myGen && safetyLoops < 25) {
                        val headSamples = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                        val headMs = (headSamples * 1000L) / sampleRate
                        reportProgress(headMs)
                        if (headSamples >= pcm.size - chunkSamples || headMs >= totalDurationMs) break
                        Thread.sleep(16L)
                        safetyLoops++
                    }
                }
                runCatching { track?.stop() }
            } catch (_: Throwable) {
                // Ignorer proprement sur périphérique audio indisponible ou interruption
            } finally {
                activeMelodyTrack = null
                runCatching { track?.release() }
                if (melodyGeneration.get() == myGen) {
                    playing.set(false)
                    mainHandler.post {
                        if (melodyGeneration.get() == myGen) {
                            onFinished()
                        }
                    }
                }
            }
        }
    }

    private fun playPcm(samples: ShortArray) {
        if (samples.isEmpty()) return
        var track: AudioTrack? = null
        try {
            val bufferBytes = samples.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferBytes, minBuf))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            val totalMs = (samples.size * 1000L) / sampleRate
            Thread.sleep(totalMs)
            track.stop()
        } catch (_: Throwable) {
            // Ignorer proprement sur périphérique audio indisponible
        } finally {
            runCatching { track?.release() }
        }
    }
}
