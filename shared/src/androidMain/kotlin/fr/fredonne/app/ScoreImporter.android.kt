package fr.fredonne.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import java.io.ByteArrayOutputStream

private class AndroidScoreImporter(
    private val launchPicker: () -> Unit,
) : ScoreImporter {
    override fun pickFile() {
        launchPicker()
    }
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "fichier"
}

private fun readBytesWithLimit(context: Context, uri: Uri, maxBytes: Int): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: error("Impossible d'ouvrir le flux du fichier.")
    return input.use { stream ->
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) {
                error("Fichier trop volumineux (maximum 10 Mo).")
            }
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    }
}

@Composable
actual fun rememberScoreImporter(
    onFileLoaded: (ImportedFile) -> Unit,
    onError: (String) -> Unit,
): ScoreImporter {
    if (LocalInspectionMode.current || LocalActivityResultRegistryOwner.current == null) {
        return remember { AndroidScoreImporter(launchPicker = {}) }
    }
    val context = LocalContext.current
    val currentOnLoaded by rememberUpdatedState(onFileLoaded)
    val currentOnError by rememberUpdatedState(onError)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val fileName = resolveDisplayName(context, uri)
            val bytes = readBytesWithLimit(context, uri, MAX_IMPORT_FILE_BYTES)
            if (bytes.isEmpty()) {
                error("Le fichier sélectionné est vide.")
            }
            ImportedFile(fileName = fileName, bytes = bytes)
        }.onSuccess { file ->
            currentOnLoaded(file)
        }.onFailure { err ->
            currentOnError(err.message ?: "Impossible de lire le fichier.")
        }
    }

    return remember(launcher) {
        AndroidScoreImporter(launchPicker = { launcher.launch(arrayOf("*/*")) })
    }
}
