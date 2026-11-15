package fr.fredonne.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

class AndroidScoreExporter(private val context: Context) : ScoreExporter {

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
}

@Composable
actual fun rememberScoreExporter(): ScoreExporter {
    val context = LocalContext.current
    return remember(context) { AndroidScoreExporter(context) }
}
