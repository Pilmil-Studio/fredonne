package fr.fredonne.app

import fr.fredonne.core.music.Durations
import fr.fredonne.core.music.Instruments
import fr.fredonne.core.music.Key
import fr.fredonne.core.music.MeasureTooLong
import fr.fredonne.core.music.MeasureTooShort
import fr.fredonne.core.music.Mode
import fr.fredonne.core.music.Note
import fr.fredonne.core.music.OutOfRange
import fr.fredonne.core.music.Pitch
import fr.fredonne.core.music.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorStateTest {

    @Test
    fun `inserting four quarter notes fills first measure cleanly`() {
        var state = EditorState()
        state = state.insertNote(Pitch(Step.C, 4))
            .insertNote(Pitch(Step.D, 4))
            .insertNote(Pitch(Step.E, 4))
            .insertNote(Pitch(Step.G, 4))

        assertEquals(1, state.melody.measures.size)
        assertEquals(4, state.melody.measures[0].events.size)
        assertTrue(state.issues.isEmpty())
    }

    @Test
    fun `inserting a fifth quarter note automatically starts a second measure`() {
        var state = EditorState()
        repeat(5) {
            state = state.insertNote(Pitch(Step.C, 4))
        }
        assertEquals(2, state.melody.measures.size)
        assertEquals(4, state.melody.measures[0].events.size)
        assertEquals(1, state.melody.measures[1].events.size)
        assertTrue(state.issues.any { it is MeasureTooShort && it.measure == 1 })
    }

    @Test
    fun `undo and redo restore notes and cursor position`() {
        val initial = EditorState()
        val after = initial.insertNote(Pitch(Step.E, 4)).insertNote(Pitch(Step.G, 4))
        assertEquals(2, after.melody.measures[0].events.size)
        assertTrue(after.canUndo)

        val undone = after.undo()
        assertEquals(1, undone.melody.measures[0].events.size)
        assertTrue(undone.canRedo)

        val redone = undone.redo()
        assertEquals(2, redone.melody.measures[0].events.size)
        assertFalse(redone.canRedo)
    }

    @Test
    fun `transposing to G major moves C4 to G4 and updates key signature`() {
        val state = EditorState().insertNote(Pitch(Step.C, 4))
        val transposed = state.transposeToKey(Step.G, 0)
        assertEquals(Key(Step.G, 0, Mode.MAJOR), transposed.melody.key)
        val note = transposed.melody.measures[0].events[0] as Note
        assertEquals(Step.G, note.pitch.step)
        assertEquals(1, transposed.melody.key.fifths)
    }

    @Test
    fun `bb clarinet written display transposes sounding C4 up a major second to D4`() {
        val state = EditorState()
            .setInstrument(Instruments.CLARINET_BB)
            .insertNote(Pitch(Step.C, 4))
        val written = state.displayedMelody.measures[0].events[0] as Note
        assertEquals(Pitch(Step.D, 4), written.pitch)
    }

    @Test
    fun `verifier flags out of range notes for flute`() {
        val state = EditorState()
            .setInstrument(Instruments.FLUTE)
            .setDuration(Durations.WHOLE)
            .insertNote(Pitch(Step.G, 3))
        assertTrue(state.issues.any { it is OutOfRange && it.tooLow })
    }

    @Test
    fun `measure exceeding time signature reports MeasureTooLong`() {
        val state = EditorState()
            .setTimeSignature(3, 4)
            .setDuration(Durations.WHOLE)
            .insertNote(Pitch(Step.C, 4))
        assertTrue(state.issues.any { it is MeasureTooLong && it.measure == 0 })
    }

    @Test
    fun `black key insertion in F major spells A sharp as B flat via Meredith PS13 context`() {
        val state = EditorState()
            .setKey(Key(Step.F, 0, Mode.MAJOR))
            .insertNote(Pitch(Step.A, 4, 1))
        val note = state.melody.measures[0].events[0] as Note
        assertEquals(Pitch(Step.B, 4, -1), note.pitch)
    }

    @Test
    fun `each instrument family synthesizes non-empty harmonic audio with Schroeder reverb`() {
        for (timbre in InstrumentTimbre.entries) {
            val pcm = ToneSynthesizer.synthesizeTone(
                freqHz = ToneSynthesizer.midiToFreq(69),
                durationMs = 240,
                tailMs = 180,
                timbre = timbre,
            )
            assertTrue(pcm.isNotEmpty())
            assertTrue(pcm.any { it != 0.toShort() })
        }
    }

    @Test
    fun `EditorState exports and imports MusicXML, MIDI and PDF cleanly`() {
        val initial = EditorState()
            .setInstrument(Instruments.CLARINET_BB)
            .insertNote(Pitch(Step.C, 4))
            .insertNote(Pitch(Step.E, 4))
            .insertNote(Pitch(Step.G, 4))
            .insertNote(Pitch(Step.C, 5))

        val xml = initial.exportMusicXml("Test Export")
        val fromXml = EditorState().importMusicXml(xml)
        assertEquals(Instruments.CLARINET_BB, fromXml.instrument)
        assertEquals(initial.melody.measures, fromXml.melody.measures)

        val midi = initial.exportMidi()
        val fromMidi = EditorState().importMidi(midi)
        assertEquals(initial.melody.measures, fromMidi.melody.measures)

        val pdf = initial.exportPdf("Test Partition")
        assertTrue(pdf.decodeToString().startsWith("%PDF-1.4"))
    }

    @Test
    fun `transposeSemitones shifts pitch and key cleanly without double accidentals across 12 semitones`() {
        var state = EditorState()
            .insertNote(Pitch(Step.C, 4))
            .insertNote(Pitch(Step.E, 4))
            .insertNote(Pitch(Step.G, 4))

        val upTone = state.transposeSemitones(2)
        assertEquals(Key(Step.D, 0, Mode.MAJOR), upTone.melody.key)
        assertEquals(Pitch(Step.D, 4), (upTone.melody.measures[0].events[0] as Note).pitch)
        assertEquals(Pitch(Step.F, 4, 1), (upTone.melody.measures[0].events[1] as Note).pitch)

        val downHalf = upTone.transposeSemitones(-1)
        assertEquals(Key(Step.D, -1, Mode.MAJOR), downHalf.melody.key)
        assertEquals(Pitch(Step.D, 4, -1), (downHalf.melody.measures[0].events[0] as Note).pitch)

        repeat(12) {
            state = state.transposeSemitones(1)
            assertTrue(state.melody.key.fifths in -6..6)
        }
        assertEquals(Key(Step.C, 0, Mode.MAJOR), state.melody.key)
        assertEquals(Pitch(Step.C, 5), (state.melody.measures[0].events[0] as Note).pitch)
    }

    @Test
    fun `loop range and playback speed slice melody and scale bpm`() {
        val sample = EditorState(melody = EditorState.sampleMelody(), tempoBpm = 100)
        assertEquals(2, sample.playbackMelody.measures.size)
        assertEquals(100, sample.effectivePlaybackBpm)

        val looped = sample
            .toggleLoop()
            .setLoopRange(1, 1)
            .setPlaybackSpeed(0.6)

        assertTrue(looped.loopEnabled)
        assertEquals(1, looped.playbackMelody.measures.size)
        assertEquals(sample.melody.measures[1], looped.playbackMelody.measures[0])
        assertEquals(60, looped.effectivePlaybackBpm)
    }

    @Test
    fun `lyric and breath mark before cursor update note with undo support`() {
        val base = EditorState()
            .insertNote(Pitch(Step.C, 4))
            .insertNote(Pitch(Step.D, 4))

        val withLyric = base.setNoteLyricBeforeCursor("  la  ")
        assertEquals("la", withLyric.noteBeforeCursor?.lyric)

        val withBreath = withLyric.toggleBreathMarkBeforeCursor()
        assertTrue(withBreath.noteBeforeCursor?.breathMark == true)

        val undone = withBreath.undo()
        assertEquals(false, undone.noteBeforeCursor?.breathMark)
        assertEquals("la", undone.noteBeforeCursor?.lyric)
    }

    @Test
    fun `insertRest, toggleDotted and mid-measure insertion update measure and cursor`() {
        val state = EditorState()
            .toggleDotted()
            .insertRest()
            .toggleDotted()
            .insertNote(Pitch(Step.C, 4))
            .setCursor(0, 1)
            .insertNote(Pitch(Step.E, 4))

        val events = state.melody.measures[0].events
        assertEquals(3, events.size)
        assertEquals(Durations.dotted(Durations.QUARTER), events[0].written)
        assertEquals(Pitch(Step.E, 4), (events[1] as Note).pitch)
        assertEquals(Pitch(Step.C, 4), (events[2] as Note).pitch)
        assertEquals(2, state.cursorEventIndex)
    }

    @Test
    fun `deleteAtCursor, addMeasure and cursor navigation across measures work cleanly`() {
        val s1 = EditorState()
            .insertNote(Pitch(Step.C, 4))
            .addMeasure()
            .insertNote(Pitch(Step.D, 4))

        assertEquals(2, s1.melody.measures.size)
        assertEquals(1, s1.cursorMeasure)
        assertEquals(1, s1.cursorEventIndex)

        val left1 = s1.moveCursorLeft()
        assertEquals(1, left1.cursorMeasure)
        assertEquals(0, left1.cursorEventIndex)

        val left2 = left1.moveCursorLeft()
        assertEquals(0, left2.cursorMeasure)
        assertEquals(1, left2.cursorEventIndex)

        val right1 = left2.moveCursorRight()
        assertEquals(1, right1.cursorMeasure)
        assertEquals(0, right1.cursorEventIndex)

        // Suppression à (1, 0) sur mesure non vide recule vers mesure 0 sans supprimer mesure 1
        val delEmpty = s1.deleteAtCursor().deleteAtCursor()
        assertEquals(1, delEmpty.melody.measures.size)
        assertEquals(0, delEmpty.cursorMeasure)
    }

    @Test
    fun `applyDetectedKey, transposeOctave, clearAll, loadSampleMelody and shareCode roundtrip`() {
        // Mélodie en Sol majeur écrite avec armure de Do majeur -> applyDetectedKey détecte Sol majeur
        val cMajNotes = EditorState()
            .insertNote(Pitch(Step.G, 4))
            .insertNote(Pitch(Step.B, 4))
            .insertNote(Pitch(Step.D, 5))
            .insertNote(Pitch(Step.F, 5, 1))
            .insertNote(Pitch(Step.G, 5))
        val detected = cMajNotes.applyDetectedKey()
        assertEquals(Key(Step.G, 0, Mode.MAJOR), detected.melody.key)
        // Idempotent si déjà dans la tonalité détectée
        assertEquals(detected, detected.applyDetectedKey())

        val octUp = detected.transposeOctave(up = true)
        assertEquals(Pitch(Step.G, 5), (octUp.melody.measures[0].events[0] as Note).pitch)
        val octDown = octUp.transposeOctave(up = false)
        assertEquals(Pitch(Step.G, 4), (octDown.melody.measures[0].events[0] as Note).pitch)

        val shareCode = detected.exportShareCode()
        val imported = EditorState().importShareCode(shareCode)
        assertEquals(detected.melody.key, imported.melody.key)
        assertEquals(detected.melody.measures, imported.melody.measures)

        val sampled = EditorState().loadSampleMelody()
        assertEquals(2, sampled.melody.measures.size)
        val cleared = sampled.clearAll()
        assertEquals(1, cleared.melody.measures.size)
        assertTrue(cleared.melody.measures[0].events.isEmpty())
    }

    @Test
    fun `clamping of tempo, speed, tuner A4, loop range and undo stack limit`() {
        val clamped = EditorState()
            .setTempo(500)
            .setPlaybackSpeed(0.1)
            .setTunerReferenceA4Hz(500.0)
            .setLoopRange(-5, 99)
        assertEquals(240, clamped.tempoBpm)
        assertEquals(0.25, clamped.playbackSpeed, 1e-6)
        assertEquals(466.0, clamped.tunerReferenceA4Hz, 1e-6)
        assertEquals(0, clamped.loopStartMeasure)
        assertEquals(0, clamped.loopEndMeasure)

        // Pile d'annulation bornée à 50 états
        var s = EditorState()
        repeat(60) {
            s = s.insertNote(Pitch(Step.C, 4))
        }
        assertEquals(50, s.undoStack.size)

        // undo/redo sans historique sont sans effet
        val base = EditorState()
        assertEquals(base, base.undo())
        assertEquals(base, base.redo())
    }
}
