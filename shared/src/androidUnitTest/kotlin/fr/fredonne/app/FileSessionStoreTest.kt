package fr.fredonne.app

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSessionStoreTest {

    private fun tempDir(): File = Files.createTempDirectory("fredonne-session").toFile()

    @Test
    fun `ecriture atomique sans fichier temporaire residuel et rechargement`() {
        val dir = tempDir()
        val store = FileSessionStore(dir)
        assertNull(store.read())

        store.write("premier")
        store.write("second")

        assertEquals("second", store.read())
        assertEquals(listOf("current-score.txt"), dir.list()!!.toList())
    }

    @Test
    fun `fichier corrompu mis de cote par le depot puis nouvelle sauvegarde possible`() {
        val dir = tempDir()
        val store = FileSessionStore(dir)
        store.write("contenu illisible")

        assertNull(SessionRepository(store).load())

        assertFalse(File(dir, "current-score.txt").exists())
        assertEquals("contenu illisible", File(dir, "current-score.corrupt").readText())

        assertTrue(SessionRepository(store).save(EditorState(tempoBpm = 120)))
        assertEquals(120, assertNotNull(SessionRepository(store).load()).tempoBpm)
    }

    @Test
    fun `aller-retour complet par le systeme de fichiers`() {
        val store = FileSessionStore(File(tempDir(), "session"))
        val state = EditorState(tempoBpm = 88, dotted = true)
        assertTrue(SessionRepository(store).save(state))
        assertEquals(state, SessionRepository(store).load())
    }
}
