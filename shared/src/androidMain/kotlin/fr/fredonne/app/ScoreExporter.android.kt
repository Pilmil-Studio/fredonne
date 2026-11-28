package fr.fredonne.app

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

class AndroidScoreExporter(
    private val context: Context,
    private val onLaunchCreateDocument: (fileName: String, bytes: ByteArray, onResult: (String?) -> Unit) -> Unit,
) : ScoreExporter {

    override fun exportBytes(fileName: String, mimeType: String, bytes: ByteArray): String {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)

        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
                putExtra(Intent.EXTRA_STREAM, uri)
                if (mimeType.contains("xml") || mimeType.startsWith("text/")) {
                    putExtra(Intent.EXTRA_TEXT, bytes.decodeToString())
                }
                clipData = ClipData.newRawUri(fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, fileName).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
        return file.absolutePath
    }

    override fun saveBytes(fileName: String, mimeType: String, bytes: ByteArray, onResult: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val saved = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                try {
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: return@runCatching null
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    "Téléchargements/$fileName"
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    null
                }
            }.getOrNull()
            if (saved != null) {
                onResult(saved)
                return
            }
        }
        onLaunchCreateDocument(fileName, bytes, onResult)
    }
}

@Composable
actual fun rememberScoreExporter(): ScoreExporter {
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    if (registryOwner == null) {
        return remember(context) {
            AndroidScoreExporter(context) { _, _, onResult -> onResult(null) }
        }
    }
    var pendingSave by remember { mutableStateOf<Triple<String, ByteArray, (String?) -> Unit>?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val task = pendingSave
        pendingSave = null
        if (uri == null || task == null) {
            task?.third?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(task.second) } != null
        }.getOrDefault(false)
        task.third(if (ok) task.first else null)
    }
    return remember(context, createDocLauncher) {
        AndroidScoreExporter(context) { fileName, bytes, onResult ->
            pendingSave = Triple(fileName, bytes, onResult)
            createDocLauncher.launch(fileName)
        }
    }
}
