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
import fr.fredonne.core.music.Tuplet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSessionStore(var content: String? = null) : SessionStore {
    var writes = 0
    var asideCalls = 0
    var asideContent: String? = null

    override fun read(): String? = content
    override fun write(text: String) {
        writes++
        content = text
    }
    override fun setAsideCorrupt() {
        asideCalls++
        asideContent = content
        content = null
    }
}

class SessionPersistenceTest {

    /** Partition non triviale : altérations, silence, triolet, paroles avec caractères spéciaux, respiration. */
    private fun richState(): EditorState {
        val melody = Melody(
            key = Key(Step.E, -1, Mode.MINOR),
            time = TimeSignature(6, 8),
            measures = listOf(
                Measure(
                    listOf(
                        Note(Pitch(Step.F, 4, 1), Durations.EIGHTH, lyric = "la\ttab \\ et\nligne"),
                        Note(Pitch(Step.B, 3, -1), Durations.dotted(Durations.QUARTER), breathMark = true),
                        Rest(Durations.EIGHTH),
                    ),
                ),
                Measure(
                    listOf(
                        Note(Pitch(Step.G, 4, 2), Durations.EIGHTH, tuplet = Tuplet(3, 2)),
                        Rest(Durations.QUARTER, tuplet = Tuplet(3, 2)),
                    ),
                ),
                Measure(emptyList()),
            ),
        )
        return EditorState(
            melody = melody,
            instrument = Instruments.CLARINET_BB,
            selectedDuration = Durations.EIGHTH,
            dotted = true,
            cursorMeasure = 1,
            cursorEventIndex = 2,
            tempoBpm = 132,
            loopEnabled = true,
            loopStartMeasure = 0,
            loopEndMeasure = 1,
            playbackSpeed = 0.75,
            tunerReferenceA4Hz = 442.0,
        ).transposeSemitones(2)
    }

    @Test
    fun `sauvegarde puis rechargement retrouvent toute la partition`() {
        val store = FakeSessionStore()
        val repository = SessionRepository(store)
        val original = richState()

        assertTrue(repository.save(original))
        val reloaded = assertNotNull(SessionRepository(store).load())

        assertEquals(original.copy(undoStack = emptyList(), redoStack = emptyList()), reloaded)
        assertEquals(original.melody.key, reloaded.melody.key)
        assertEquals(6, reloaded.melody.time.beats)
        assertEquals("la\ttab \\ et\nligne", (reloaded.melody.measures[0].events[0] as Note).lyric)
    }

    @Test
    fun `une partition par defaut fait aussi l aller-retour`() {
        val store = FakeSessionStore()
        val repository = SessionRepository(store)
        repository.save(EditorState())
        assertEquals(EditorState(), repository.load())
    }

    @Test
    fun `sans fichier le chargement renvoie null sans rien mettre de cote`() {
        val store = FakeSessionStore()
        assertNull(SessionRepository(store).load())
        assertEquals(0, store.asideCalls)
    }

    @Test
    fun `fichier corrompu donne une partition vide et le fichier est mis de cote`() {
        val garbage = listOf(
            "n'importe quoi",
            "fredonne-session\t99\nkey\tC\t0\tM\ntime\t4\t4\n",
            "fredonne-session\t1\nkey\tC\t0\tM\n",
            "fredonne-session\t1\nkey\tZ\t0\tM\ntime\t4\t4\nmeasure\n",
            "fredonne-session\t1\nkey\tC\t0\tM\ntime\t4\t4\nmeasure\nN\tC\t4\t0\t1\t0\t0\t0\t0\t\n",
            "fredonne-session\t1\nkey\tC\t0\tM\ntime\t4\t4\nN\tC\t4\t0\t1\t4\t0\t0\t0\t\n",
        )
        for (content in garbage) {
            val store = FakeSessionStore(content)
            assertNull(SessionRepository(store).load(), content)
            assertEquals(1, store.asideCalls, content)
            assertEquals(content, store.asideContent)
        }
    }

    @Test
    fun `une mise de cote qui echoue ne fait pas planter le chargement`() {
        val store = object : SessionStore {
            override fun read() = "corrompu"
            override fun write(text: String) = Unit
            override fun setAsideCorrupt() = throw IllegalStateException("disque en lecture seule")
        }
        assertNull(SessionRepository(store).load())
    }

    @Test
    fun `une ecriture qui echoue est signalee sans exception`() {
        val store = object : SessionStore {
            override fun read(): String? = null
            override fun write(text: String) = throw IllegalStateException("disque plein")
            override fun setAsideCorrupt() = Unit
        }
        assertFalse(SessionRepository(store).save(EditorState()))
    }

    @Test
    fun `les lignes inconnues sont ignorees et le curseur est ramene dans la partition`() {
        val text = EditorSessionCodec.encode(EditorState()) + "champ-futur\tvaleur\n"
        val withBadCursor = text.replace(Regex("cursor\t\\d+\t\\d+"), "cursor\t40\t40")
        val state = EditorSessionCodec.decode(withBadCursor)
        assertTrue(state.cursorMeasure in state.melody.measures.indices)
        assertTrue(state.cursorEventIndex <= state.melody.measures[state.cursorMeasure].events.size)
    }

    @Test
    fun `les ecritures rapprochees sont regroupees en une seule`() {
        val store = FakeSessionStore()
        val scheduler = AutosaveScheduler(SessionRepository(store))
        val a = EditorState(tempoBpm = 90)
        val b = EditorState(tempoBpm = 100)
        val c = EditorState(tempoBpm = 110)

        val t1 = scheduler.schedule(a)
        val t2 = scheduler.schedule(b)
        val t3 = scheduler.schedule(c)
        assertFalse(scheduler.commit(t1))
        assertFalse(scheduler.commit(t2))
        assertEquals(0, store.writes)

        assertTrue(scheduler.commit(t3))
        assertEquals(1, store.writes)
        assertEquals(110, SessionRepository(store).load()?.tempoBpm)
        assertFalse(scheduler.commit(t3), "rien de plus à écrire")
        assertEquals(1, store.writes)
    }

    @Test
    fun `le flush a la mise en arriere-plan ecrit tout de suite et une seule fois`() {
        val store = FakeSessionStore()
        val scheduler = AutosaveScheduler(SessionRepository(store))
        val token = scheduler.schedule(EditorState(tempoBpm = 77))

        assertTrue(scheduler.flush())
        assertEquals(1, store.writes)
        assertFalse(scheduler.commit(token), "le délai échu ne réécrit pas")
        assertFalse(scheduler.flush())
        assertEquals(1, store.writes)
    }
}
