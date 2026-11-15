package fr.fredonne.app

import androidx.compose.runtime.Composable

internal const val MAX_IMPORT_FILE_BYTES: Int = 10 * 1024 * 1024

data class ImportedFile(
    val fileName: String,
    val bytes: ByteArray,
)

/**
 * Sélecteur de fichiers hors-ligne (.abc, .musicxml, .xml, .mid) sans permission de stockage.
 */
interface ScoreImporter {
    fun pickFile()
}

@Composable
expect fun rememberScoreImporter(
    onFileLoaded: (ImportedFile) -> Unit,
    onError: (String) -> Unit,
): ScoreImporter
