package fr.fredonne.app

import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Rest
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TimeSignature
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneSynthesizerTest {

    @Test
    fun `midiToFreq calcule les frequences de reference exactes`() {
        assertEquals(440.0, ToneSynthesizer.midiToFreq(69), 1e-6)
        assertEquals(220.0, ToneSynthesizer.midiToFreq(57), 1e-6)
        assertEquals(880.0, ToneSynthesizer.midiToFreq(81), 1e-6)
    }

    @Test
    fun `timbreForInstrument associe chaque famille d instruments`() {
        assertEquals(InstrumentTimbre.WARM_KEYS, ToneSynthesizer.timbreForInstrument(null))
        assertEquals(InstrumentTimbre.FLUTE, ToneSynthesizer.timbreForInstrument(Instruments.FLUTE))
        assertEquals(InstrumentTimbre.REED, ToneSynthesizer.timbreForInstrument(Instruments.CLARINET_BB))
        assertEquals(InstrumentTimbre.REED, ToneSynthesizer.timbreForInstrument(Instruments.SAX_ALTO))
        assertEquals(InstrumentTimbre.BRASS, ToneSynthesizer.timbreForInstrument(Instruments.TRUMPET_BB))
        assertEquals(InstrumentTimbre.BRASS, ToneSynthesizer.timbreForInstrument(Instruments.HORN_F))
        assertEquals(InstrumentTimbre.STRING, ToneSynthesizer.timbreForInstrument(Instruments.VIOLIN))
        assertEquals(InstrumentTimbre.STRING, ToneSynthesizer.timbreForInstrument(Instruments.HARP_CELTIC))
        assertEquals(InstrumentTimbre.BASS, ToneSynthesizer.timbreForInstrument(Instruments.BASS_ELECTRIC))
        assertEquals(InstrumentTimbre.BASS, ToneSynthesizer.timbreForInstrument(Instruments.CONTRABASS))
    }

    @Test
    fun `iso226Gain reste borne et renforce le grave tout en adoucissant 3 kHz`() {
        val bassGain = ToneSynthesizer.iso226Gain(80.0)
        val midGain = ToneSynthesizer.iso226Gain(440.0)
        val earNotchGain = ToneSynthesizer.iso226Gain(3000.0)

        assertTrue(bassGain in 0.65..1.25)
        assertTrue(midGain in 0.65..1.25)
        assertTrue(earNotchGain in 0.65..1.25)
        assertTrue(bassGain > midGain)
        assertTrue(earNotchGain < midGain)
    }

    @Test
    fun `synthesizeNoteFloat et toPcm16 gerent duree nulle duree positive et saturation douce`() {
        val empty = ToneSynthesizer.synthesizeNoteFloat(440.0, durationMs = 0, tailMs = 0)
        assertEquals(0, empty.size)

        val note = ToneSynthesizer.synthesizeNoteFloat(
            freqHz = 440.0,
            durationMs = 100,
            tailMs = 50,
            timbre = InstrumentTimbre.FLUTE,
        )
        val expectedSamples = (ToneSynthesizer.SAMPLE_RATE * 150) / 1000
        assertEquals(expectedSamples, note.size)
        assertTrue(note.all { it.isFinite() })
        assertTrue(note.any { abs(it) > 0.001f })

        val clipped = ToneSynthesizer.toPcm16(floatArrayOf(-10f, 0f, 10f))
        assertEquals(3, clipped.size)
        assertTrue(clipped[0] < 0)
        assertEquals(0.toShort(), clipped[1])
        assertTrue(clipped[2] > 0)
    }

    @Test
    fun `synthesizeMelody produit des timings monotones et gere les silences`() {
        val melody = Melody(
            key = Key(Step.C, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.C, 4), Durations.QUARTER),
                        Rest(Durations.QUARTER),
                        Note(Pitch(Step.E, 4), Durations.HALF),
                    ),
                ),
            ),
        )
        val (pcm, timings) = ToneSynthesizer.synthesizeMelody(melody, bpm = 120, timbre = InstrumentTimbre.STRING)
        assertTrue(pcm.isNotEmpty())
        assertEquals(3, timings.size)
        assertEquals(0L, timings[0].startMs)
        assertEquals(0, timings[0].measureIndex)
        assertEquals(0, timings[0].eventIndex)
        assertTrue(timings[1].startMs > timings[0].startMs)
        assertTrue(timings[2].startMs > timings[1].startMs)
        assertTrue(abs(timings[1].startMs - 500L) <= 2L)
        assertTrue(abs(timings[2].startMs - 1000L) <= 2L)
    }

    @Test
    fun `pcm16ToWav genere un en-tete RIFF WAVE PCM 16 bits mono conforme`() {
        val samples = shortArrayOf(0, 1000, -1000, 32767)
        val wav = ToneSynthesizer.pcm16ToWav(samples, sampleRate = 44_100)

        assertEquals(44 + samples.size * 2, wav.size)
        assertEquals("RIFF", wav.copyOfRange(0, 4).decodeToString())
        assertEquals("WAVE", wav.copyOfRange(8, 12).decodeToString())
        assertEquals("fmt ", wav.copyOfRange(12, 16).decodeToString())
        assertEquals("data", wav.copyOfRange(36, 40).decodeToString())

        // Taille data = 8 octets en little-endian
        assertEquals(8, wav[40].toInt() and 0xFF)
        assertEquals(0, wav[41].toInt() and 0xFF)
    }

    @Test
    fun `AcousticSampleBank decode des echantillons finis pour tous les timbres`() {
        for (timbre in InstrumentTimbre.entries) {
            val sample = AcousticSampleBank.get(timbre, freqHz = 440.0)
            assertTrue(sample.pcm.isNotEmpty())
            assertTrue(sample.pcm.all { it.isFinite() && it in -1.0f..1.0f })
        }
        val lowString = AcousticSampleBank.get(InstrumentTimbre.STRING, freqHz = 110.0)
        assertEquals(45.0, lowString.rootMidi, 1e-6)
        assertTrue(lowString.pcm.isNotEmpty())
    }
}
