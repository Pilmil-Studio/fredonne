package fr.fredonne.app

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import fr.fredonne.core.music.AbcCodec
import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.QrCodeEncoder
import fr.fredonne.core.music.ShareCodec
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TimeSignature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QrRoundTripAndroidTest {

    private fun decodeWithZxing(matrix: QrCodeEncoder.QrMatrix): String {
        val quiet = 4
        val scale = 2
        val dim = (matrix.size + quiet * 2) * scale
        val pixels = IntArray(dim * dim) { 0xFFFFFFFF.toInt() }
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    for (sy in 0 until scale) {
                        for (sx in 0 until scale) {
                            val px = (x + quiet) * scale + sx
                            val py = (y + quiet) * scale + sy
                            pixels[py * dim + px] = 0xFF000000.toInt()
                        }
                    }
                }
            }
        }
        val source = RGBLuminanceSource(dim, dim, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun `QrCodeEncoder versions 1 a 16 sont relues exactement par ZXing QRCodeReader`() {
        val payloads = listOf(
            "FR1|C0:M:4/4|C4q", // Version 1
            "B".repeat(160), // Version 7 (version info 6x3)
            "C".repeat(300), // Version 11
            "D".repeat(360), // Version 12 (2 groupes de blocs)
            "E".repeat(450), // Version 14 (4 centres d'alignement)
            "F".repeat(QrCodeEncoder.MAX_CAPACITY_BYTES), // Version 16 (586 octets)
        )
        for (payload in payloads) {
            val matrix = QrCodeEncoder.encodeOrNull(payload)
            assertNotNull(matrix, "Payload de ${payload.length} octets doit tenir")
            assertEquals(payload, decodeWithZxing(matrix))
        }
    }

    @Test
    fun `round-trip complet AbcCodec vers QrCodeEncoder vers ZXing vers ShareCodec decodeAny`() {
        val melody = Melody(
            key = Key(Step.B, -1, Mode.MAJOR),
            time = TimeSignature(4, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.B, 4, -1), Durations.QUARTER),
                        Note(Pitch(Step.C, 5, 0), Durations.QUARTER),
                        Note(Pitch(Step.D, 5, 0), Durations.HALF),
                    ),
                ),
            ),
        )
        val abc = AbcCodec.encode(melody, Instruments.CLARINET_BB, tempoBpm = 112, title = "Test QR")
        val qr = QrCodeEncoder.encodeOrNull(abc)
        assertNotNull(qr)

        val scanned = decodeWithZxing(qr)
        val decoded = ShareCodec.decodeAny(scanned)
        assertEquals(melody, decoded.melody)
        assertEquals(Instruments.CLARINET_BB, decoded.instrument)
        assertEquals(112, decoded.tempoBpm)
    }
}
