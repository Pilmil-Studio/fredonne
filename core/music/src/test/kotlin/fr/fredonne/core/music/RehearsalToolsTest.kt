package fr.fredonne.core.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RehearsalToolsTest {

    private fun pureSine(freqHz: Double, durationSec: Double = 0.1, sampleRate: Int = 16_000): FloatArray {
        val count = (durationSec * sampleRate).toInt()
        return FloatArray(count) { i ->
            (0.7 * sin(2.0 * PI * freqHz * i / sampleRate)).toFloat()
        }
    }

    // --- ChromaticTuner ---

    @Test
    fun `tuner identifie La 440 Hz juste a 440 Hz et bas a 442 Hz`() {
        val tuner440 = ChromaticTuner(referenceA4Hz = 440.0)
        val reading440 = tuner440.evaluateFrequency(440.0)
        assertNotNull(reading440)
        assertEquals(Pitch(Step.A, 4, 0), reading440.nearestPitch)
        assertEquals(0.0, reading440.centsOffset, 1e-6)
        assertEquals(TuningStatus.IN_TUNE, reading440.status)

        val tuner442 = ChromaticTuner(referenceA4Hz = 442.0)
        val reading442 = tuner442.evaluateFrequency(440.0)
        assertNotNull(reading442)
        assertEquals(Pitch(Step.A, 4, 0), reading442.nearestPitch)
        assertEquals(-7.85, reading442.centsOffset, 0.05)
        assertEquals(TuningStatus.FLAT, reading442.status)
    }

    @Test
    fun `tuner distingue flat in_tune et sharp selon le seuil`() {
        val tuner = ChromaticTuner(referenceA4Hz = 440.0, inTuneThresholdCents = 5.0)
        // +15 cents au-dessus de La 440 Hz (~443.83 Hz)
        val sharp = tuner.evaluateFrequency(443.83)
        assertNotNull(sharp)
        assertEquals(TuningStatus.SHARP, sharp.status)

        // +3 cents (~440.76 Hz) -> IN_TUNE
        val inTune = tuner.evaluateFrequency(440.76)
        assertNotNull(inTune)
        assertEquals(TuningStatus.IN_TUNE, inTune.status)

        // -12 cents (~436.96 Hz) -> FLAT
        val flat = tuner.evaluateFrequency(436.96)
        assertNotNull(flat)
        assertEquals(TuningStatus.FLAT, flat.status)

        assertNull(tuner.evaluateFrequency(0.0))
        assertNull(tuner.evaluateFrequency(-100.0))
    }

    @Test
    fun `tuner calcule la frequence cible du diapason`() {
        val tuner440 = ChromaticTuner(440.0)
        assertEquals(440.0, tuner440.targetFrequencyHz(69), 1e-6)
        assertEquals(220.0, tuner440.targetFrequencyHz(57), 1e-6)

        val tuner442 = ChromaticTuner(442.0)
        assertEquals(442.0, tuner442.targetFrequencyHz(69), 1e-6)
    }

    @Test
    fun `tuner analyzeSamples sur sinusoide synthetique A4 et Sib3`() {
        val tuner = ChromaticTuner(referenceA4Hz = 440.0)

        val a4Reading = tuner.analyzeSamples(pureSine(440.0))
        assertNotNull(a4Reading)
        assertEquals(69, a4Reading.nearestPitch.midi)
        assertTrue(abs(a4Reading.centsOffset) < 5.0, "Écart obtenu : ${a4Reading.centsOffset}")

        // Si♭3 = MIDI 58 ≈ 233.0819 Hz
        val bb3Reading = tuner.analyzeSamples(pureSine(233.0819))
        assertNotNull(bb3Reading)
        assertEquals(58, bb3Reading.nearestPitch.midi)
        assertTrue(abs(bb3Reading.centsOffset) < 5.0, "Écart obtenu : ${bb3Reading.centsOffset}")

        // Silence -> null
        assertNull(tuner.analyzeSamples(FloatArray(1024)))
    }

    // --- MetronomeEngine ---

    @Test
    fun `metronome borne le bpm et calcule intervalle`() {
        assertEquals(500.0, MetronomeEngine.beatIntervalMs(120), 1e-6)
        assertEquals(2000.0, MetronomeEngine.beatIntervalMs(10), 1e-6)
        assertEquals(250.0, MetronomeEngine.beatIntervalMs(300), 1e-6)
    }

    @Test
    fun `metronome genere temps et accents pour 4_4 et 3_4 et 6_8`() {
        val beats44 = MetronomeEngine.generateBeats(TimeSignature(4, 4), bpm = 120, barCount = 2)
        assertEquals(8, beats44.size)
        assertEquals(listOf(0, 4), beats44.filter { it.isAccent }.map { it.beatIndex })
        assertEquals(listOf(1, 2, 3, 4, 1, 2, 3, 4), beats44.map { it.measureBeat })
        assertEquals(listOf(0L, 500L, 1000L, 1500L, 2000L, 2500L, 3000L, 3500L), beats44.map { it.timestampMs })

        val beats34 = MetronomeEngine.generateBeats(TimeSignature(3, 4), bpm = 120, barCount = 2)
        assertEquals(6, beats34.size)
        assertEquals(listOf(0, 3), beats34.filter { it.isAccent }.map { it.beatIndex })

        val beats68Eighth = MetronomeEngine.generateBeats(
            TimeSignature(6, 8),
            bpm = 120,
            barCount = 1,
            compoundSixEightByDottedQuarter = false,
        )
        assertEquals(6, beats68Eighth.size)
        assertTrue(beats68Eighth[0].isAccent)
        assertFalse(beats68Eighth[3].isAccent)

        val beats68Dotted = MetronomeEngine.generateBeats(
            TimeSignature(6, 8),
            bpm = 90,
            barCount = 2,
            compoundSixEightByDottedQuarter = true,
        )
        assertEquals(4, beats68Dotted.size)
        assertEquals(listOf(0, 2), beats68Dotted.filter { it.isAccent }.map { it.beatIndex })
    }

    @Test
    fun `metronome countInBeats genere le decompte attendu`() {
        val countIn = MetronomeEngine.countInBeats(TimeSignature(4, 4), bpm = 100, bars = 1)
        assertEquals(4, countIn.size)
        assertTrue(countIn.first().isAccent)
        assertEquals(0L, countIn.first().timestampMs)
        assertEquals(600L, countIn[1].timestampMs)
    }

    // --- TapTempoTracker ---

    @Test
    fun `tap tempo converge vers 120 bpm et reinitialise apres timeout`() {
        val tracker = TapTempoTracker(maxTaps = 8, resetTimeoutMs = 2_500L)
        assertNull(tracker.tap(1_000L))
        assertEquals(120, tracker.tap(1_500L))
        assertEquals(120, tracker.tap(2_000L))
        assertEquals(120, tracker.tap(2_500L))
        assertEquals(120, tracker.currentBpm())

        // Pause > 2500 ms -> remise à zéro
        assertNull(tracker.tap(5_500L))
        assertNull(tracker.currentBpm())
        assertEquals(150, tracker.tap(5_900L))

        tracker.reset()
        assertNull(tracker.currentBpm())
    }

    // --- PracticeLoop ---

    @Test
    fun `practice loop valide les bornes et calcule effectiveBpm`() {
        assertFailsWith<IllegalArgumentException> { PracticeLoop(-1, 2, 1.0) }
        assertFailsWith<IllegalArgumentException> { PracticeLoop(3, 1, 1.0) }
        assertFailsWith<IllegalArgumentException> { PracticeLoop(0, 1, 0.2) }
        assertFailsWith<IllegalArgumentException> { PracticeLoop(0, 1, 2.5) }

        val loopHalf = PracticeLoop(0, 1, 0.5)
        assertEquals(60, loopHalf.effectiveBpm(120))
        assertEquals(20, loopHalf.effectiveBpm(30))

        val loopFast = PracticeLoop(0, 1, 2.0)
        assertEquals(300, loopFast.effectiveBpm(180))

        assertEquals(listOf(0.5, 0.6, 0.8, 1.0), PracticeLoop.PRESET_SPEEDS)
    }

    @Test
    fun `practice loop extrait exactement les mesures demandees sans modifier original`() {
        val quarter = Fraction.of(1, 4)
        val m0 = Measure(listOf(Note(Pitch(Step.C, 4), quarter)))
        val m1 = Measure(listOf(Note(Pitch(Step.D, 4), quarter)))
        val m2 = Measure(listOf(Note(Pitch(Step.E, 4), quarter)))
        val m3 = Measure(listOf(Note(Pitch(Step.F, 4), quarter)))
        val melody = Melody(
            key = Key(Step.C, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(m0, m1, m2, m3),
        )

        val loop = PracticeLoop(startMeasure = 1, endMeasure = 2, speedFactor = 0.8)
        val sliced = loop.sliceMelody(melody)

        assertEquals(2, sliced.measures.size)
        assertEquals(listOf(m1, m2), sliced.measures)
        assertEquals(4, melody.measures.size)

        assertFailsWith<IllegalArgumentException> {
            PracticeLoop(0, 4, 1.0).sliceMelody(melody)
        }
    }

    @Test
    fun `tuner valide les parametres et rejette les frequences invalides ou le silence`() {
        assertFailsWith<IllegalArgumentException> { ChromaticTuner(referenceA4Hz = 400.0) }
        assertFailsWith<IllegalArgumentException> { ChromaticTuner(referenceA4Hz = 480.0) }
        assertFailsWith<IllegalArgumentException> { ChromaticTuner(inTuneThresholdCents = 0.0) }

        val tuner = ChromaticTuner()
        assertNull(tuner.evaluateFrequency(0.0))
        assertNull(tuner.evaluateFrequency(-440.0))
        assertNull(tuner.evaluateFrequency(Double.NaN))
        assertNull(tuner.evaluateFrequency(Double.POSITIVE_INFINITY))
        assertNull(tuner.analyzeSamples(FloatArray(2048)))
    }

    @Test
    fun `metronome et tap tempo gerent les cas limites et la remise a zero`() {
        assertTrue(MetronomeEngine.generateBeats(TimeSignature(4, 4), 100, barCount = 0).isEmpty())
        assertFailsWith<IllegalArgumentException> {
            MetronomeEngine.generateBeats(TimeSignature(4, 4), 100, barCount = -1)
        }

        assertFailsWith<IllegalArgumentException> { TapTempoTracker(maxTaps = 1) }
        assertFailsWith<IllegalArgumentException> { TapTempoTracker(resetTimeoutMs = 0L) }

        val tracker = TapTempoTracker()
        assertNull(tracker.tap(1000L))
        // Même timestamp -> span == 0 -> null
        assertNull(tracker.tap(1000L))
        tracker.reset()
        assertNull(tracker.tap(1000L))
        assertEquals(120, tracker.tap(1500L))
        // Remise à zéro si horodatage antérieur
        assertNull(tracker.tap(1000L))
        tracker.tap(1200L) // 300 BPM -> borné à 240
        assertEquals(240, tracker.currentBpm())
        tracker.reset()
        assertNull(tracker.currentBpm())
    }
}
