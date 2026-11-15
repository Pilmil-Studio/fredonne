package fr.fredonne.core.music

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Transcrit une suite de trames audio PCM (ou d'estimations [PitchEstimate]) en une [Melody]
 * quantifiée rythmiquement et orthographiée selon l'armure [key].
 *
 * Les hauteurs produites sont en sons réels (ton de concert), prêtes à être affichées ou
 * transposées automatiquement pour l'instrument choisi dans Fredonne.
 */
class AudioMelodyTranscriber(
    private val detector: AudioPitchDetector = AudioPitchDetector(),
    private val hopSizeSamples: Int = 320, // 20 ms à 16 kHz
) {
    private data class RawSegment(
        val midi: Int?, // null = silence
        val frameCount: Int,
    )

    fun transcribePcm(
        pcm: FloatArray,
        frameSize: Int = 1024,
        bpm: Int = 100,
        time: TimeSignature = TimeSignature(4, 4),
        key: Key = Key(Step.C, 0, Mode.MAJOR),
        grid: Fraction = Fraction.of(1, 8),
    ): Melody {
        val estimates = mutableListOf<PitchEstimate>()
        var offset = 0
        while (offset + frameSize <= pcm.size) {
            val slice = pcm.copyOfRange(offset, offset + frameSize)
            estimates += detector.estimate(slice)
            offset += hopSizeSamples
        }
        return transcribeEstimates(estimates, bpm, time, key, grid)
    }

    fun transcribeEstimates(
        estimates: List<PitchEstimate>,
        bpm: Int = 100,
        time: TimeSignature = TimeSignature(4, 4),
        key: Key = Key(Step.C, 0, Mode.MAJOR),
        grid: Fraction = Fraction.of(1, 8),
    ): Melody {
        if (estimates.isEmpty()) {
            return Melody(key, time, emptyList())
        }

        // 1. Lissage médian sur 3 trames pour supprimer les sauts d'octave isolés
        val smoothedMidi = IntArray(estimates.size) { i ->
            val window = (-1..1)
                .mapNotNull { d -> estimates.getOrNull(i + d)?.nearestMidi }
            if (window.size >= 2) {
                val sorted = window.sorted()
                sorted[sorted.size / 2]
            } else {
                -1
            }
        }

        // 2. Découpage en segments (notes / silences) avec tolérance au vibrato
        val segments = mutableListOf<RawSegment>()
        var currentMidi: Int? = if (smoothedMidi[0] >= 0) smoothedMidi[0] else null
        var currentCount = 1

        for (i in 1 until smoothedMidi.size) {
            val m = if (smoothedMidi[i] >= 0) smoothedMidi[i] else null
            val sameState = when {
                currentMidi == null && m == null -> true
                currentMidi != null && m != null -> {
                    val est = estimates[i].midiFloat
                    m == currentMidi || (est != null && abs(est - currentMidi) < 0.42)
                }
                else -> false
            }
            if (sameState) {
                currentCount++
            } else {
                segments += RawSegment(currentMidi, currentCount)
                currentMidi = m
                currentCount = 1
            }
        }
        segments += RawSegment(currentMidi, currentCount)

        // Supprimer les micro-artefacts (< 3 trames = 60 ms) et les silences de bord
        val cleaned = segments
            .filter { it.frameCount >= 3 }
            .dropWhile { it.midi == null }
            .dropLastWhile { it.midi == null }

        if (cleaned.isEmpty()) {
            return Melody(key, time, emptyList())
        }

        // Fusionner les segments consécutifs identiques après nettoyage
        val merged = mutableListOf<RawSegment>()
        for (seg in cleaned) {
            val prev = merged.lastOrNull()
            if (prev != null && prev.midi == seg.midi) {
                merged[merged.lastIndex] = prev.copy(frameCount = prev.frameCount + seg.frameCount)
            } else {
                merged += seg
            }
        }

        // 3. Conversion durées -> fractions musicales quantifiées sur la grille
        val secondsPerFrame = hopSizeSamples.toDouble() / detector.sampleRate
        val secondsPerWholeNote = (60.0 / bpm.coerceIn(30, 240)) * 4.0
        val gridValue = grid.numerator.toDouble() / grid.denominator.toDouble()

        val events = mutableListOf<Event>()
        for (seg in merged) {
            val wholeFraction = (seg.frameCount * secondsPerFrame) / secondsPerWholeNote
            val steps = (wholeFraction / gridValue).roundToInt().coerceAtLeast(1)
            val quantized = quantizeDuration(steps, grid)
            val event: Event = if (seg.midi != null) {
                Note(spellMidi(seg.midi, key), quantized)
            } else {
                Rest(quantized)
            }
            events += event
        }

        // 4. Découpage propre en mesures selon time.measureLength
        return packIntoMeasures(events, time, key)
    }

    private fun quantizeDuration(steps: Int, grid: Fraction): Fraction {
        val raw = Fraction.of(steps * grid.numerator, grid.denominator)
        val standard = listOf(
            Fraction.of(1, 1),
            Fraction.of(3, 4),
            Fraction.of(1, 2),
            Fraction.of(3, 8),
            Fraction.of(1, 4),
            Fraction.of(3, 16),
            Fraction.of(1, 8),
            Fraction.of(1, 16),
        )
        return standard.minByOrNull { s ->
            abs((s.numerator.toDouble() / s.denominator) - (raw.numerator.toDouble() / raw.denominator))
        } ?: raw
    }

    private fun packIntoMeasures(events: List<Event>, time: TimeSignature, key: Key): Melody {
        val measureLen = time.measureLength
        val measures = mutableListOf<Measure>()
        var currentEvents = mutableListOf<Event>()
        var remaining = measureLen

        for (ev in events) {
            var left = ev.length
            while (left > Fraction.ZERO) {
                val take = if (left <= remaining) left else remaining
                val piece = when (ev) {
                    is Note -> Note(ev.pitch, take)
                    is Rest -> Rest(take)
                }
                currentEvents += piece
                remaining -= take
                left -= take
                if (remaining == Fraction.ZERO) {
                    measures += Measure(currentEvents)
                    currentEvents = mutableListOf()
                    remaining = measureLen
                }
            }
        }
        if (currentEvents.isNotEmpty()) {
            if (remaining > Fraction.ZERO) {
                currentEvents += Rest(remaining)
            }
            measures += Measure(currentEvents)
        }
        return Melody(key, time, measures)
    }

    companion object {
        /** Orthographie une hauteur MIDI selon l'armure [key] (bémols vs dièses). */
        fun spellMidi(midi: Int, key: Key): Pitch {
            val octave = (midi / 12) - 1
            val pc = midi.mod(12)
            val preferFlats = key.fifths < 0
            return when (pc) {
                0 -> Pitch(Step.C, octave, 0)
                1 -> if (preferFlats) Pitch(Step.D, octave, -1) else Pitch(Step.C, octave, 1)
                2 -> Pitch(Step.D, octave, 0)
                3 -> if (preferFlats) Pitch(Step.E, octave, -1) else Pitch(Step.D, octave, 1)
                4 -> Pitch(Step.E, octave, 0)
                5 -> Pitch(Step.F, octave, 0)
                6 -> if (preferFlats) Pitch(Step.G, octave, -1) else Pitch(Step.F, octave, 1)
                7 -> Pitch(Step.G, octave, 0)
                8 -> if (preferFlats) Pitch(Step.A, octave, -1) else Pitch(Step.G, octave, 1)
                9 -> Pitch(Step.A, octave, 0)
                10 -> if (preferFlats) Pitch(Step.B, octave, -1) else Pitch(Step.A, octave, 1)
                else -> Pitch(Step.B, octave, 0)
            }
        }
    }
}
