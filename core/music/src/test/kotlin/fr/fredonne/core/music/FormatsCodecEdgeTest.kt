package fr.fredonne.core.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatsCodecEdgeTest {

    @Test
    fun `MusicXmlCodec ignore chord et grace, resout transpose sans instrument-name et gere clef-octave-change`() {
        val externalXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="4.0">
              <part-list>
                <score-part id="P1"><part-name>Clarinet</part-name></score-part>
              </part-list>
              <part id="P1">
                <measure number="1">
                  <attributes>
                    <divisions>4</divisions>
                    <key><fifths>0</fifths><mode>major</mode></key>
                    <time><beats>4</beats><beat-type>4</beat-type></time>
                    <transpose>
                      <diatonic>-1</diatonic>
                      <chromatic>-2</chromatic>
                    </transpose>
                  </attributes>
                  <note><grace/><pitch><step>B</step><octave>3</octave></pitch><type>16th</type></note>
                  <note><pitch><step>D</step><octave>4</octave></pitch><duration>4</duration></note>
                  <note><chord/><pitch><step>F</step><octave>4</octave></pitch><duration>4</duration></note>
                  <note><rest/><duration>12</duration></note>
                </measure>
              </part>
            </score-partwise>
        """.trimIndent()

        val (melody, instrument) = MusicXmlCodec.import(externalXml)
        assertEquals(Instruments.CLARINET_BB, instrument)
        assertEquals(2, melody.measures[0].events.size)
        val firstNote = melody.measures[0].events[0] as Note
        // D4 écrit en Sib -> Do4 réel (C4)
        assertEquals(Pitch(Step.C, 4), firstNote.pitch)
        assertEquals(Durations.QUARTER, firstNote.written)
        val rest = melody.measures[0].events[1] as Rest
        assertEquals(Durations.dotted(Durations.HALF), rest.written)

        // Export/import avec clé octaviée (TREBLE_8VB) pour VOICE_TENOR et titre avec caractères XML
        val tenorMelody = Melody(
            key = Key(Step.A, 0, Mode.MINOR),
            time = TimeSignature(3, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.C, 4), Durations.HALF, lyric = "A & B <test>"),
                        Note(Pitch(Step.D, 4), Durations.QUARTER),
                    ),
                ),
            ),
        )
        val xml = MusicXmlCodec.export(tenorMelody, instrument = Instruments.VOICE_TENOR, title = "Duo <1 & 2>")
        assertTrue(xml.contains("<clef-octave-change>-1</clef-octave-change>"))
        assertTrue(xml.contains("Duo &lt;1 &amp; 2&gt;"))

        val (reimported, reInstrument) = MusicXmlCodec.import(xml)
        assertEquals(Instruments.VOICE_TENOR, reInstrument)
        assertEquals(tenorMelody, reimported)
    }

    @Test
    fun `MusicXmlCodec import sur XML sans mesure retourne une mesure vide`() {
        val (melody, instrument) = MusicXmlCodec.import("<score-partwise version=\"4.0\"></score-partwise>")
        assertNull(instrument)
        assertEquals(1, melody.measures.size)
        assertTrue(melody.measures[0].events.isEmpty())
    }

    @Test
    fun `MidiCodec rejette en-tete invalide et gere running status, NoteOn vel 0, silences et tonalite implicite`() {
        assertFailsWith<IllegalArgumentException> { MidiCodec.import(byteArrayOf(1, 2, 3)) }
        assertFailsWith<IllegalArgumentException> { MidiCodec.import("BAD_HEADER_12345".encodeToByteArray()) }

        // Export/import en tonalité mineure avec silences et instrument
        val minorMelody = Melody(
            key = Key(Step.D, 0, Mode.MINOR),
            time = TimeSignature(6, 8),
            measures = listOf(
                Measure(
                    listOf(
                        Rest(Durations.EIGHTH),
                        Note(Pitch(Step.D, 4), Durations.QUARTER),
                        Note(Pitch(Step.F, 4), Durations.dotted(Durations.QUARTER)),
                    ),
                ),
            ),
        )
        val bytes = MidiCodec.export(minorMelody, bpm = 112, instrument = Instruments.OBOE)
        val (decoded, bpm) = MidiCodec.import(bytes)
        assertEquals(112, bpm)
        assertEquals(Key(Step.D, 0, Mode.MINOR), decoded.key)
        assertEquals(TimeSignature(6, 8), decoded.time)
        assertEquals(minorMelody.measures, decoded.measures)

        // Construction manuelle d'un MIDI Format 0 avec running status + NoteOn vel 0 (sans FF 59)
        val manualTrack = byteArrayOf(
            0x00, 0x90.toByte(), 60, 80, // NoteOn C4 à tick 0
            0x83.toByte(), 0x60, 60, 0,  // Running status NoteOn vel=0 (480 ticks = 0x83 0x60)
            0x00, 0xFF.toByte(), 0x2F, 0x00, // End of track
        )
        val manualMidi = byteArrayOf(
            'M'.code.toByte(), 'T'.code.toByte(), 'h'.code.toByte(), 'd'.code.toByte(),
            0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0.toByte(), // division = 480
            'M'.code.toByte(), 'T'.code.toByte(), 'r'.code.toByte(), 'k'.code.toByte(),
            0, 0, 0, manualTrack.size.toByte(),
        ) + manualTrack
        val (parsedManual, defaultBpm) = MidiCodec.import(manualMidi)
        assertEquals(96, defaultBpm)
        val note = parsedManual.measures[0].events.single() as Note
        assertEquals(60, note.pitch.midi)
        assertEquals(Durations.QUARTER, note.written)
    }

    @Test
    fun `ShareCodec valide les erreurs et preserve alterations doubles, points, tuplets et mode mineur`() {
        assertFailsWith<IllegalArgumentException> { ShareCodec.decode("INVALID|C0:M:4/4:96:-|") }
        assertFailsWith<IllegalArgumentException> { ShareCodec.decode("FR1|no_pipe") }
        assertFailsWith<IllegalArgumentException> { ShareCodec.decode("FR1|C0:M:4/4|") }

        val richMelody = Melody(
            key = Key(Step.F, 1, Mode.MINOR),
            time = TimeSignature(3, 8),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.F, 4, 2), Durations.dotted(Durations.EIGHTH)),
                        Note(Pitch(Step.G, 4, -2), Durations.SIXTEENTH, Tuplet(3, 2)),
                        Rest(Durations.EIGHTH),
                    ),
                ),
                Measure(emptyList()),
            ),
        )
        val code = ShareCodec.encode(richMelody, instrument = null, tempoBpm = 84)
        val decoded = ShareCodec.decode(code)
        assertEquals(richMelody.key, decoded.melody.key)
        assertEquals(richMelody.time, decoded.melody.time)
        assertEquals(84, decoded.tempoBpm)
        assertNull(decoded.instrument)
        assertEquals(richMelody.measures, decoded.melody.measures)

        // Référence =99 hors bornes -> mesure vide de secours
        val badRef = ShareCodec.decode("FR1|C0:M:4/4:96:-|C4q/=99")
        assertEquals(2, badRef.melody.measures.size)
        assertTrue(badRef.melody.measures[1].events.isEmpty())
    }

    @Test
    fun `QrCodeEncoder genere des matrices valides pour differentes versions et tronque au-dela de 586 octets`() {
        val v1 = QrCodeEncoder.encode("FR1|C0:M:4/4|C4q") // 16 octets <= 17 (capacité Version 1)
        assertEquals(21, v1.size) // Version 1 : 21x21

        val small = QrCodeEncoder.encode("FR1|C0:M:4/4:96:-|C4q") // 21 octets -> Version 2
        assertEquals(25, small.size) // Version 2 : 25x25
        // Motif de repérage 7x7 en haut à gauche : coin (0,0) = true, bord blanc (7,0) = false
        assertTrue(small[0, 0])
        assertTrue(small[6, 6])
        assertTrue(small[small.size - 1, 0]) // Coin haut-droit
        assertTrue(small[0, small.size - 1]) // Coin bas-gauche

        val longPayload = "FR1|C0:M:4/4:96:-|" + List(60) { "C4q,D4q,E4q,F4q" }.joinToString("/")
        val large = QrCodeEncoder.encode(longPayload)
        assertEquals(81, large.size) // Version 16 : 17 + 4*16 = 81
    }

    @Test
    fun `ScorePdfGenerator gere multi-systemes, harpe a pedales, melodie vide et echappement PDF`() {
        val q = Durations.QUARTER
        val manyMeasures = List(10) {
            Measure(listOf(Note(Pitch(Step.C, 4), q), Note(Pitch(Step.D, 4), q), Note(Pitch(Step.E, 4), q), Note(Pitch(Step.F, 4), q)))
        }
        val harpMelody = Melody(Key(Step.C, -1, Mode.MAJOR), TimeSignature(4, 4), manyMeasures)
        val pdfBytes = ScorePdfGenerator.generate(
            melody = harpMelody,
            instrument = Instruments.HARP_PEDAL,
            tempoBpm = 104,
            title = "Etude (1) \\ Harpe",
        )
        val pdfText = pdfBytes.decodeToString()
        assertTrue(pdfText.startsWith("%PDF-1.4"))
        assertTrue(pdfText.trimEnd().endsWith("%%EOF"))
        assertTrue(pdfText.contains("Etude \\(1\\) \\\\ Harpe"))
        assertTrue(pdfText.contains("Pedales") || pdfText.contains("P\\351dales") || pdfText.contains("Harpe"))

        val emptyPdf = ScorePdfGenerator.generate(
            melody = Melody(Key(Step.C, 0, Mode.MAJOR), TimeSignature(4, 4), emptyList()),
        ).decodeToString()
        assertTrue(emptyPdf.startsWith("%PDF-1.4"))
        assertTrue(emptyPdf.trimEnd().endsWith("%%EOF"))
    }
}
