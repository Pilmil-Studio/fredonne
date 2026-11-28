package fr.fredonne.app

import androidx.compose.runtime.Composable

/**
 * Exporteur hors-ligne de partitions (MusicXML, MIDI, PDF) sans aucune permission de stockage.
 */
interface ScoreExporter {
    fun exportBytes(fileName: String, mimeType: String, bytes: ByteArray): String
    fun saveBytes(fileName: String, mimeType: String, bytes: ByteArray, onResult: (String?) -> Unit)
}

@Composable
expect fun rememberScoreExporter(): ScoreExporter
