package fr.fredonne.core.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnsembleNotationTest {

    private val q = Durations.QUARTER
    private val h = Durations.HALF

    @Test
    fun `Clef ALTO et TENOR ont les hauteurs de ligne du bas attendues`() {
        assertEquals(Pitch(Step.F, 3), Clef.ALTO.bottomLinePitch)
        assertEquals("C", Clef.ALTO.sign)
        assertEquals(3, Clef.ALTO.line)
        assertEquals(0, Clef.ALTO.octaveChange)
        assertEquals("C3", Clef.ALTO.displaySymbol)

        assertEquals(Pitch(Step.D, 3), Clef.TENOR.bottomLinePitch)
        assertEquals("C", Clef.TENOR.sign)
        assertEquals(4, Clef.TENOR.line)
        assertEquals(0, Clef.TENOR.octaveChange)
        assertEquals("C4", Clef.TENOR.displaySymbol)
    }

    @Test
    fun `Melody transposed et respellPractical conservent lyric et breathMark`() {
        val melody = Melody(
            key = Key(Step.C, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.C, 4), q, lyric = "Do"),
                        Note(Pitch(Step.D, 4), q, lyric = "ré", breathMark = true),
                        Note(Pitch(Step.E, 4), h, lyric = "mi", breathMark = false),
                    ),
                ),
            ),
        )

        val transposed = melody.transposed(Interval(1, 2)) // +1 ton (Ré majeur)
        val notes = transposed.measures.single().events.filterIsInstance<Note>()
        assertEquals(3, notes.size)
        assertEquals("Do", notes[0].lyric)
        assertFalse(notes[0].breathMark)
        assertEquals("ré", notes[1].lyric)
        assertTrue(notes[1].breathMark)
        assertEquals("mi", notes[2].lyric)
        assertFalse(notes[2].breathMark)

        // Vérifie aussi avec une tonalité théorique à 8 dièses -> respellPractical
        val sharpMelody = melody.transposed(Interval(4, 8)) // Sol# majeur (8 dièses)
        val respelled = sharpMelody.respellPractical()
        val respelledNotes = respelled.measures.single().events.filterIsInstance<Note>()
        assertEquals("Do", respelledNotes[0].lyric)
        assertEquals("ré", respelledNotes[1].lyric)
        assertTrue(respelledNotes[1].breathMark)
        assertEquals("mi", respelledNotes[2].lyric)
    }

    @Test
    fun `MusicXmlCodec export et import conservent lyric avec caracteres speciaux breathMark et cles Ut`() {
        val original = Melody(
            key = Key(Step.C, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.C, 4), q, lyric = "Chœur & voix"),
                        Note(Pitch(Step.D, 4), q, lyric = "a < b > c", breathMark = true),
                        Note(Pitch(Step.E, 4), q, lyric = "\"paix\""),
                        Note(Pitch(Step.F, 4), q, lyric = "l'aube", breathMark = true),
                    ),
                ),
            ),
        )

        val altoInst = Instrument("viola_test", Interval.UNISON, Pitch(Step.C, 3), Pitch(Step.E, 5), clef = Clef.ALTO)
        val xmlAlto = MusicXmlCodec.export(original, instrument = altoInst, title = "Chorale Alto")
        assertTrue(xmlAlto.contains("<sign>C</sign>"))
        assertTrue(xmlAlto.contains("<line>3</line>"))
        assertTrue(xmlAlto.contains("<lyric number=\"1\">"))
        assertTrue(xmlAlto.contains("<text>Chœur &amp; voix</text>"))
        assertTrue(xmlAlto.contains("<text>a &lt; b &gt; c</text>"))
        assertTrue(xmlAlto.contains("<notations>"))
        assertTrue(xmlAlto.contains("<breath-mark/>"))

        val tenorInst = Instrument("cello_tenor_test", Interval.UNISON, Pitch(Step.C, 3), Pitch(Step.E, 5), clef = Clef.TENOR)
        val xmlTenor = MusicXmlCodec.export(original, instrument = tenorInst, title = "Chorale Ténor")
        assertTrue(xmlTenor.contains("<sign>C</sign>"))
        assertTrue(xmlTenor.contains("<line>4</line>"))

        val (imported, _) = MusicXmlCodec.import(xmlAlto)
        assertEquals(original.key, imported.key)
        assertEquals(original.time, imported.time)
        assertEquals(original.measures, imported.measures)
    }

    @Test
    fun `ScorePdfGenerator produit un PDF valide avec cles ALTO et TENOR paroles et respiration`() {
        val melody = Melody(
            key = Key(Step.F, 0, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.F, 3), q, lyric = "Fa"),
                        Note(Pitch(Step.A, 3), q, lyric = "la", breathMark = true),
                        Note(Pitch(Step.C, 4), h, lyric = "do"),
                    ),
                ),
                Measure(
                    listOf(
                        Note(Pitch(Step.D, 4), q, lyric = "ré"),
                        Note(Pitch(Step.C, 4), q, lyric = "do", breathMark = true),
                        Note(Pitch(Step.A, 3), h, lyric = "la"),
                    ),
                ),
            ),
        )

        val altoInst = Instrument("viola_test", Interval.UNISON, Pitch(Step.C, 3), Pitch(Step.E, 5), clef = Clef.ALTO)
        val tenorInst = Instrument("cello_tenor_test", Interval.UNISON, Pitch(Step.C, 3), Pitch(Step.E, 5), clef = Clef.TENOR)

        val pdfAlto = ScorePdfGenerator.generate(melody, instrument = altoInst, tempoBpm = 88, title = "Test Alto")
        val altoText = pdfAlto.decodeToString()
        assertTrue(altoText.startsWith("%PDF-1.4"))
        assertTrue(altoText.contains("%%EOF"))
        assertTrue(altoText.contains("(C3) Tj"))
        assertTrue(altoText.contains("(Fa) Tj"))
        assertTrue(altoText.contains("(') Tj"))

        val pdfTenor = ScorePdfGenerator.generate(melody, instrument = tenorInst, tempoBpm = 88, title = "Test Ténor")
        val tenorText = pdfTenor.decodeToString()
        assertTrue(tenorText.startsWith("%PDF-1.4"))
        assertTrue(tenorText.contains("%%EOF"))
        assertTrue(tenorText.contains("(C4) Tj"))
        assertTrue(tenorText.contains("(ré) Tj") || tenorText.contains("(r\\351) Tj"))
        assertTrue(tenorText.contains("(') Tj"))
    }
}
