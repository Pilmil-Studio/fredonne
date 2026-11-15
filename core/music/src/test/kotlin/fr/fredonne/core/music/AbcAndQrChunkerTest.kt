package fr.fredonne.core.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbcAndQrChunkerTest {

    @Test
    fun `AbcCodec encode et decode round-trip avec alterations octaves tuplets et instrument`() {
        val melody = Melody(
            key = Key(Step.D, 0, Mode.MAJOR), // F# et C# à l'armure
            time = TimeSignature(3, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.D, 4, 0), Durations.QUARTER),
                        Note(Pitch(Step.F, 4, 1), Durations.EIGHTH),
                        Note(Pitch(Step.F, 4, 0), Durations.EIGHTH), // bécarre explicite
                        Note(Pitch(Step.A, 5, 0), Durations.QUARTER), // octave 5
                    ),
                ),
                Measure(
                    listOf(
                        Note(Pitch(Step.B, 3, -1), Durations.EIGHTH, Tuplet(3, 2)),
                        Note(Pitch(Step.C, 4, 1), Durations.EIGHTH, Tuplet(3, 2)),
                        Note(Pitch(Step.D, 4, 0), Durations.EIGHTH, Tuplet(3, 2)),
                        Rest(Durations.HALF),
                    ),
                ),
            ),
        )

        val abc = AbcCodec.encode(
            melody = melody,
            instrument = Instruments.CLARINET_BB,
            tempoBpm = 108,
            title = "Valse test",
        )
        assertTrue(abc.contains("X:1"))
        assertTrue(abc.contains("K:D"))
        assertTrue(abc.contains("%%fredonne-instrument:clarinet_bb"))
        assertTrue(abc.contains("(3"))

        val decoded = AbcCodec.decode(abc)
        assertEquals(melody.key, decoded.melody.key)
        assertEquals(melody.time, decoded.melody.time)
        assertEquals(108, decoded.tempoBpm)
        assertEquals(Instruments.CLARINET_BB, decoded.instrument)
        assertEquals(melody.measures, decoded.melody.measures)
    }

    @Test
    fun `ShareCodec decodeAny reconnait FR1 ABC et MusicXML`() {
        val melody = Melody(
            key = Key(Step.A, 0, Mode.MINOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.A, 4, 0), Durations.QUARTER),
                        Note(Pitch(Step.C, 5, 0), Durations.QUARTER),
                        Note(Pitch(Step.E, 5, 0), Durations.HALF),
                    ),
                ),
            ),
        )

        val fr1 = ShareCodec.encode(melody, Instruments.CLARINET_BB, 92)
        val abc = AbcCodec.encode(melody, Instruments.CLARINET_BB, 92)
        val xml = MusicXmlCodec.export(melody, Instruments.CLARINET_BB, "Test")

        assertEquals(melody, ShareCodec.decodeAny(fr1).melody)
        assertEquals(melody, ShareCodec.decodeAny(abc).melody)
        assertEquals(melody, ShareCodec.decodeAny(xml).melody)
        assertFailsWith<IllegalArgumentException> {
            ShareCodec.decodeAny("du texte quelconque sans format musical")
        }
    }

    @Test
    fun `QrChunker garde un seul QR sans prefixe FR2 et reassemble le multi-QR dans le desordre`() {
        val shortPayload = "X:1\nK:C\nC2 D2 E2 F2 |]"
        val single = QrChunker.split(shortPayload, maxSingleBytes = 100)
        assertEquals(listOf(shortPayload), single)

        val longPayload = "X:1\nT:Longue melodie avec accents éèà\nK:C\n" +
            (1..20).joinToString(" | ") { "C2 D2 E2 G2" } + " |]"
        val chunks = QrChunker.split(longPayload, maxSingleBytes = 90)
        assertTrue(chunks.size >= 3)
        assertTrue(chunks.all { it.encodeToByteArray().size <= 90 })

        val assembler = QrChunker.Assembler()
        // Scan dans le désordre : dernier, puis premier, puis le reste
        val rLast = assembler.accept(chunks.last())
        assertTrue(rLast is QrChunker.AssemblyResult.Partial)
        assertEquals(1, (rLast as QrChunker.AssemblyResult.Partial).collected)

        for (i in 0 until chunks.size - 2) {
            val partial = assembler.accept(chunks[i])
            assertTrue(partial is QrChunker.AssemblyResult.Partial)
        }
        val finalResult = assembler.accept(chunks[chunks.size - 2])
        assertTrue(finalResult is QrChunker.AssemblyResult.Complete)
        assertEquals(longPayload, (finalResult as QrChunker.AssemblyResult.Complete).payload)
    }

    @Test
    fun `QrCodeEncoder encodeOrNull supporte jusqu a la version 16 et retourne null au-dela`() {
        val v16Payload = "A".repeat(QrCodeEncoder.MAX_CAPACITY_BYTES)
        val matrix = QrCodeEncoder.encodeOrNull(v16Payload)
        assertNotNull(matrix)
        assertEquals(17 + 4 * 16, matrix.size)

        val tooBig = "A".repeat(QrCodeEncoder.MAX_CAPACITY_BYTES + 1)
        assertNull(QrCodeEncoder.encodeOrNull(tooBig))
    }

    @Test
    fun `AbcCodec gere doubles alterations octaves extremes en-tetes externes et barres de reprise`() {
        val externalAbc = """
            X:1
            T:Externe
            % commentaire ignore
            M:C|
            L:1/4
            Q:120
            K:Fm
            |: ^^F c' __B, =A | z2 G/2 A/2 :|
        """.trimIndent()

        val decoded = AbcCodec.decode(externalAbc)
        assertEquals(Key(Step.F, 0, Mode.MINOR), decoded.melody.key)
        assertEquals(TimeSignature(2, 2), decoded.melody.time)
        assertEquals(120, decoded.tempoBpm)
        assertEquals(2, decoded.melody.measures.size)

        val m1 = decoded.melody.measures[0].events
        assertEquals(Note(Pitch(Step.F, 4, 2), Durations.QUARTER), m1[0])
        assertEquals(Note(Pitch(Step.C, 6, 0), Durations.QUARTER), m1[1])
        assertEquals(Note(Pitch(Step.B, 3, -2), Durations.QUARTER), m1[2])
        assertEquals(Note(Pitch(Step.A, 4, 0), Durations.QUARTER), m1[3])

        val m2 = decoded.melody.measures[1].events
        assertEquals(Rest(Durations.HALF), m2[0])
        assertEquals(Note(Pitch(Step.G, 4, 0), Durations.EIGHTH), m2[1])
        assertEquals(Note(Pitch(Step.A, 4, -1), Durations.EIGHTH), m2[2])
    }

    @Test
    fun `QrChunker rejette les fragments malformes et reinitialise sur changement de tag`() {
        val assembler = QrChunker.Assembler()
        assertTrue(assembler.accept("FR2|badheader") is QrChunker.AssemblyResult.Invalid)
        assertTrue(assembler.accept("FR2|abcd:0/2|hello") is QrChunker.AssemblyResult.Invalid)
        assertTrue(assembler.accept("FR2|abcd:3/2|hello") is QrChunker.AssemblyResult.Invalid)

        val p1 = assembler.accept("FR2|1111:1/2|abc")
        assertTrue(p1 is QrChunker.AssemblyResult.Partial)
        // Nouveau tag -> réinitialise l'accumulation précédente
        val p2 = assembler.accept("FR2|2222:1/2|xyz")
        assertTrue(p2 is QrChunker.AssemblyResult.Partial)
        assertEquals(1, (p2 as QrChunker.AssemblyResult.Partial).collected)
        val done = assembler.accept("FR2|2222:2/2|123")
        assertEquals(QrChunker.AssemblyResult.Complete("xyz123"), done)
    }

    @Test
    fun `ScorePdfGenerator gere multi-QR et repli sans QR sur tres longue partition`() {
        val measure = Measure(
            listOf(
                Note(Pitch(Step.C, 4, 0), Durations.QUARTER),
                Note(Pitch(Step.D, 4, 1), Durations.QUARTER),
                Note(Pitch(Step.E, 4, 0), Durations.QUARTER),
                Note(Pitch(Step.F, 4, 1), Durations.QUARTER),
            ),
        )
        // ~40 mesures -> tient en 2 ou 3 QR codes (mention "Scannez les X codes")
        val mediumMelody = Melody(Key(Step.C, 0, Mode.MAJOR), TimeSignature(4, 4), List(40) { measure })
        val mediumPdf = ScorePdfGenerator.generate(mediumMelody, title = "Moyenne").decodeToString()
        assertTrue(mediumPdf.startsWith("%PDF-1.4"))
        assertTrue(mediumPdf.contains("QR codes") || mediumPdf.contains("Format ouvert ABC"))

        // ~150 mesures -> > 3 QR codes -> mention de repli sans QR tronqué
        val hugeMelody = Melody(Key(Step.C, 0, Mode.MAJOR), TimeSignature(4, 4), List(150) { measure })
        val hugePdf = ScorePdfGenerator.generate(hugeMelody, title = "Tres longue").decodeToString()
        assertTrue(hugePdf.startsWith("%PDF-1.4"))
        assertTrue(hugePdf.contains("Partition longue : exportez en ABC ou MusicXML"))
    }

    @Test
    fun `QR malveillants ou extremes ne provoquent ni crash ni debordement`() {
        // 1. Payload FR2 géant / total absurde
        val assembler = QrChunker.Assembler()
        assertTrue(assembler.accept("FR2|abcd:1/999999999|C") is QrChunker.AssemblyResult.Invalid)
        assertTrue(assembler.accept("X".repeat(QrChunker.MAX_ASSEMBLED_BYTES + 1)) is QrChunker.AssemblyResult.Invalid)

        // 2. ABC avec débordement de fractions, octaves extrêmes et milliers de notes
        val nastyAbc = buildString {
            appendLine("X:1")
            appendLine("M:999999999/4")
            appendLine("L:1/999999999999999")
            appendLine("Q:999999")
            appendLine("K:C")
            append("c" + "'".repeat(500) + "999999999999/999999999999 ")
            repeat(6000) { append("C | ") }
        }
        val parsedAbc = ShareCodec.decodeAny(nastyAbc)
        assertTrue(parsedAbc.melody.measures.size <= 500)
        assertEquals(200, parsedAbc.tempoBpm)

        // 3. FR1 avec fractions invalides, altérations/octaves extrêmes et milliers de mesures
        val nastyFr1 = "FR1|C+99:M:999999/4:999:-|C########-9999_0,R999999_999999*0:0/" +
            List(1000) { "=0" }.joinToString("/")
        val parsedFr1 = ShareCodec.decodeAny(nastyFr1)
        assertTrue(parsedFr1.melody.measures.size <= 500)

        // 4. Texte binaire / nul / vide -> IllegalArgumentException propre (attrapée par runCatching)
        assertFailsWith<IllegalArgumentException> { ShareCodec.decodeAny("\u0000\u0001\u0002") }
        assertFailsWith<IllegalArgumentException> { ShareCodec.decodeAny("FR1|bad") }
    }
}
