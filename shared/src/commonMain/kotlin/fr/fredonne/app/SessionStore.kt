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
