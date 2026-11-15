package fr.fredonne.app

import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Melody
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Step
import fr.fredonne.core.music.TimeSignature
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorStateAbcTest {

    @Test
    fun `exportAbc et importShareCode preservent melodie instrument et tempo`() {
        val melody = Melody(
            key = Key(Step.F, 0, Mode.MAJOR),
            time = TimeSignature(3, 4),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.F, 4, 0), Durations.QUARTER),
                        Note(Pitch(Step.G, 4, 0), Durations.QUARTER),
                        Note(Pitch(Step.A, 4, 0), Durations.QUARTER),
                    ),
                ),
            ),
        )
        val state = EditorState(
            melody = melody,
            instrument = Instruments.CLARINET_BB,
            tempoBpm = 104,
        )

        val abc = state.exportAbc("Valse")
        val imported = EditorState(melody = EditorState.defaultMelody()).importShareCode(abc)

        assertEquals(melody, imported.melody)
        assertEquals(Instruments.CLARINET_BB, imported.instrument)
        assertEquals(104, imported.tempoBpm)
    }
}
