@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.getBytes
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

private class IosScoreImporter(
    private val onLoaded: () -> (ImportedFile) -> Unit,
    private val onError: () -> (String) -> Unit,
) : ScoreImporter {

    private var activeDelegate: NSObject? = null

    override fun pickFile() {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVc == null) {
            onError()("Impossible d'ouvrir le sélecteur de fichiers.")
            return
        }

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeData, UTTypeItem),
        )
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                activeDelegate = null
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                val accessed = url.startAccessingSecurityScopedResource()
                try {
                    val data = NSData.dataWithContentsOfURL(url)
                    if (data == null) {
                        onError()("Impossible de lire le fichier sélectionné.")
                        return
                    }
                    if (data.length > MAX_IMPORT_FILE_BYTES.toULong()) {
                        onError()("Fichier trop volumineux (maximum 10 Mo).")
                        return
                    }
                    val size = data.length.toInt()
                    if (size <= 0) {
                        onError()("Le fichier sélectionné est vide.")
                        return
                    }
                    val bytes = ByteArray(size)
                    bytes.usePinned { pinned ->
                        data.getBytes(pinned.addressOf(0), length = data.length)
                    }
                    val fileName = url.lastPathComponent ?: "fichier"
                    onLoaded()(ImportedFile(fileName = fileName, bytes = bytes))
                } finally {
                    if (accessed) {
                        url.stopAccessingSecurityScopedResource()
                    }
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                activeDelegate = null
            }
        }
        activeDelegate = delegate
        picker.delegate = delegate
        rootVc.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberScoreImporter(
    onFileLoaded: (ImportedFile) -> Unit,
    onError: (String) -> Unit,
): ScoreImporter {
    val currentOnLoaded by rememberUpdatedState(onFileLoaded)
    val currentOnError by rememberUpdatedState(onError)
    return remember {
        IosScoreImporter(
            onLoaded = { currentOnLoaded },
            onError = { currentOnError },
        )
    }
}
