package fr.fredonne.app

import androidx.compose.runtime.Composable

/** Stockage privé de la partition courante : un seul document texte. */
interface SessionStore {
    /** Contenu du document, ou null s'il n'existe pas encore. */
    fun read(): String?

    /** Remplace le document de façon atomique (fichier temporaire puis renommage). */
    fun write(text: String)

    /** Écarte le document illisible (renommé, jamais supprimé) pour repartir d'une partition vide. */
    fun setAsideCorrupt()
}

@Composable
expect fun rememberSessionStore(): SessionStore

/** Appelle [onBackground] quand l'appli quitte le premier plan (pause, arrêt, retour à l'accueil, rotation). */
@Composable
expect fun OnAppBackground(onBackground: () -> Unit)

/** Reprise et sauvegarde de la partition courante, indépendamment de la plateforme. */
class SessionRepository(private val store: SessionStore) {

    /** Dernière partition enregistrée, ou null (aucune, ou fichier illisible mis de côté). */
    fun load(): EditorState? {
        val text = try {
            store.read()
        } catch (e: Exception) {
            return null
        } ?: return null
        return try {
            EditorSessionCodec.decode(text)
        } catch (e: Exception) {
            try {
                store.setAsideCorrupt()
            } catch (_: Exception) {
                // Mise de côté impossible : l'appli démarre quand même sur une partition vide.
            }
            null
        }
    }

    /** Une écriture qui échoue (disque plein) ne doit jamais faire planter l'appli. */
    fun save(state: EditorState): Boolean = try {
        store.write(EditorSessionCodec.encode(state))
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Regroupe les écritures : chaque modification appelle [schedule], et seule la dernière est
 * écrite, quand son [commit] arrive sans qu'une autre modification l'ait remplacée. Le délai
 * d'attente lui-même est du ressort de l'appelant (`delay` dans un `LaunchedEffect`).
 */
class AutosaveScheduler(private val repository: SessionRepository) {
    private var generation = 0
    private var pending: EditorState? = null

    fun schedule(state: EditorState): Int {
        pending = state
        return ++generation
    }

    /** Écrit si [token] est encore la dernière modification annoncée. */
    fun commit(token: Int): Boolean = token == generation && flush()

    /** Écrit tout de suite ce qui est en attente (mise en arrière-plan). */
    fun flush(): Boolean {
        val state = pending ?: return false
        pending = null
        return repository.save(state)
    }
}

/** Enregistrement brut d'une partition dans la bibliothèque locale. */
data class SavedScoreRecord(
    val id: String,
    val updatedAtMs: Long,
    val content: String,
)

/** Stockage privé de toutes les partitions enregistrées par l'utilisateur. */
interface ScoreLibraryStore {
    fun list(): List<SavedScoreRecord>
    fun read(id: String): String?
    fun write(id: String, text: String)
    fun delete(id: String): Boolean
    fun newId(): String
}

@Composable
expect fun rememberScoreLibraryStore(): ScoreLibraryStore

/** Partition décodée dans la bibliothèque locale. */
data class SavedScoreEntry(
    val id: String,
    val updatedAtMs: Long,
    val state: EditorState,
)

/** Gestion de la bibliothèque locale de partitions, indépendamment de la plateforme. */
class ScoreLibraryRepository(private val store: ScoreLibraryStore) {

    fun list(): List<SavedScoreEntry> = store.list()
        .sortedByDescending { it.updatedAtMs }
        .mapNotNull { rec ->
            try {
                SavedScoreEntry(rec.id, rec.updatedAtMs, EditorSessionCodec.decode(rec.content))
            } catch (_: Exception) {
                null
            }
        }

    fun save(id: String?, state: EditorState): SavedScoreEntry? = try {
        val targetId = id ?: store.newId()
        val clean = state.copy(undoStack = emptyList(), redoStack = emptyList())
        store.write(targetId, EditorSessionCodec.encode(clean))
        SavedScoreEntry(targetId, updatedAtMs = store.list().firstOrNull { it.id == targetId }?.updatedAtMs ?: 0L, state = clean)
    } catch (_: Exception) {
        null
    }

    fun load(id: String): EditorState? = try {
        store.read(id)?.let(EditorSessionCodec::decode)
    } catch (_: Exception) {
        null
    }

    fun delete(id: String): Boolean = try {
        store.delete(id)
    } catch (_: Exception) {
        false
    }
}
