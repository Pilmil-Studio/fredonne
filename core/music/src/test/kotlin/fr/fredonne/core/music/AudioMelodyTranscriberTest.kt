package fr.fredonne.core.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioMelodyTranscriberTest {

    private fun synthTone(freqHz: Double, durationSec: Double, sampleRate: Int = 16_000): FloatArray {
        val count = (durationSec * sampleRate).toInt()
        return FloatArray(count) { i ->
            val t = i.toDouble() / sampleRate
            // Fondamentale + 2e harmonique pour simuler une voix / instrument à vent
            (0.6 * sin(2.0 * PI * freqHz * t) + 0.25 * sin(2.0 * PI * 2.0 * freqHz * t)).toFloat()
        }
    }

    @Test
    fun `detecte la hauteur exacte du La 440 Hz`() {
        val detector = AudioPitchDetector(sampleRate = 16_000)
        val frame = synthTone(440.0, 0.08)
        val est = detector.estimate(frame.copyOfRange(0, 1024))

        assertTrue(est.isVoiced)
        assertEquals(69, est.nearestMidi)
        assertTrue(abs(est.centsOffset) <= 3, "écart attendu <= 3 cents, obtenu ${est.centsOffset}")
    }

    @Test
    fun `transcrit une melodie do re mi sol avec silence et transposition clarinette`() {
        // 120 BPM : 1 noire = 0.5 s ; Do4 (261.63), Ré4 (293.66), silence, Mi4 (329.63), Sol4 (392.00)
        val silence = FloatArray(8000) // 0.5 s
        val pcm = synthTone(261.63, 0.5) +
            synthTone(293.66, 0.5) +
            silence +
            synthTone(329.63, 0.5) +
            synthTone(392.00, 0.5)

        val keyBb = Key(Step.B, -1, Mode.MAJOR)
        val melody = AudioMelodyTranscriber().transcribePcm(
            pcm = pcm,
            bpm = 120,
            key = keyBb,
            grid = Fraction.of(1, 4),
        )

        val notes = melody.measures.flatMap { it.events }.filterIsInstance<Note>()
        val midis = notes.map { it.pitch.midi }
        assertEquals(listOf(60, 62, 64, 67), midis)

        // En clarinette Si♭ (+2 demi-tons écrits), Do4 réel s'écrit Ré4 (MIDI 62)
        val clarinet = Instruments.ALL.first { it.id == "clarinet_bb" }
        val writtenFirst = clarinet.toWritten(notes.first().pitch)
        assertEquals(62, writtenFirst.midi)
    }

    @Test
    fun `orthographie les bemols en tonalite a bemols`() {
        val pitch = AudioMelodyTranscriber.spellMidi(70, Key(Step.F, 0, Mode.MAJOR))
        assertEquals(Pitch(Step.B, 4, -1), pitch)
    }

    @Test
    fun `detector rejette trame trop courte silence et bruit aperiodique`() {
        val detector = AudioPitchDetector(sampleRate = 16_000)

        val shortFrame = detector.estimate(FloatArray(32) { 0.5f })
        assertFalse(shortFrame.isVoiced)
        assertNull(shortFrame.frequencyHz)

        val silence = detector.estimate(FloatArray(1024))
        assertFalse(silence.isVoiced)
        assertEquals(0.0, silence.rms)

        // Signal pseudo-aléatoire déterministe (sans périodicité)
        var seed = 1234567
        val noise = FloatArray(1024) {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            ((seed / 2147483647.0) * 2.0 - 1.0).toFloat() * 0.4f
        }
        val noiseEst = detector.estimate(noise)
        assertFalse(noiseEst.isVoiced)
    }

    @Test
    fun `transcribeEstimates gere liste vide micro-artefacts et lissage median d un saut d octave`() {
        val transcriber = AudioMelodyTranscriber()

        val empty = transcriber.transcribeEstimates(emptyList())
        assertTrue(empty.measures.isEmpty())

        val voiced60 = PitchEstimate(261.63, 60.0, 60, 0, 0.95, 0.2)
        val jump72 = PitchEstimate(523.25, 72.0, 72, 0, 0.95, 0.2)
        val unvoiced = PitchEstimate(null, null, null, 0, 0.0, 0.001)

        // Uniquement des silences ou artefacts < 3 trames
        val artifactsOnly = transcriber.transcribeEstimates(listOf(unvoiced, voiced60, voiced60, unvoiced))
        assertTrue(artifactsOnly.measures.isEmpty())

        // Saut d'octave isolé au milieu de 10 trames de Do4 -> lissé par la médiane sur 3 trames
        val withSpike = List(5) { voiced60 } + listOf(jump72) + List(5) { voiced60 }
        val smoothed = transcriber.transcribeEstimates(
            estimates = withSpike,
            bpm = 120,
            grid = Fraction.of(1, 4),
        )
        val notes = smoothed.measures.flatMap { it.events }.filterIsInstance<Note>()
        assertEquals(1, notes.size)
        assertEquals(60, notes.first().pitch.midi)
    }

    @Test
    fun `transcribeEstimates decoupe les notes a cheval sur la barre de mesure et complete par silence`() {
        val transcriber = AudioMelodyTranscriber()
        // 120 BPM : 25 trames de 20ms = 0.5s = 1 noire. 5 noires en 4/4 -> 1 mesure pleine + 1 mesure (1 noire + 3 soupirs/demi-pause)
        val voiced60 = PitchEstimate(261.63, 60.0, 60, 0, 0.95, 0.2)
        val voiced64 = PitchEstimate(329.63, 64.0, 64, 0, 0.95, 0.2)
        val estimates = List(100) { voiced60 } + List(25) { voiced64 }

        val melody = transcriber.transcribeEstimates(
            estimates = estimates,
            bpm = 120,
            time = TimeSignature(4, 4),
            grid = Fraction.of(1, 4),
        )
        assertEquals(2, melody.measures.size)
        assertEquals(Fraction.of(1, 1), melody.measures[0].events.fold(Fraction.ZERO) { a, e -> a + e.length })
        assertEquals(Fraction.of(1, 1), melody.measures[1].events.fold(Fraction.ZERO) { a, e -> a + e.length })
        assertTrue(melody.measures[1].events.last() is Rest)
    }
}
