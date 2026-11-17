package fr.fredonne.app

import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Measure
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorStateHumTest {

    @Test
    fun `annuler apres fredonnement restaure la melodie precedente`() {
        val initial = EditorState(melody = EditorState.defaultMelody())
            .insertNote(Pitch(Step.C, 4, 0))
        val hummed = initial.melody.copy(
            measures = listOf(Measure(listOf(Note(Pitch(Step.G, 4, 0), Durations.HALF)))),
        )

        val applied = initial.applyTranscribedMelody(hummed)
        assertEquals(hummed, applied.melody)
        assertTrue(applied.canUndo)

        val undone = applied.undo()
        assertEquals(initial.melody, undone.melody)
        assertEquals(initial.cursorMeasure, undone.cursorMeasure)
        assertEquals(initial.cursorEventIndex, undone.cursorEventIndex)
        assertTrue(undone.canRedo)
    }
}
