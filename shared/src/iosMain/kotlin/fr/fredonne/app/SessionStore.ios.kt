@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

/** Même document que sur Android, dans le dossier privé « Application Support » de l'appli. */
class IosSessionStore : SessionStore {

    private val manager = NSFileManager.defaultManager
    private val dir: String = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/session"
    private val path get() = "$dir/current-score.txt"

    override fun read(): String? =
        if (manager.fileExistsAtPath(path)) NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) else null

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun write(text: String) {
        manager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        // atomically = true : NSString écrit dans un fichier temporaire puis le renomme.
        check((text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)) {
            "Écriture de la session impossible"
        }
    }

    override fun setAsideCorrupt() {
        if (!manager.fileExistsAtPath(path)) return
        val corrupt = "$dir/current-score.corrupt"
        manager.removeItemAtPath(corrupt, error = null)
        manager.moveItemAtPath(path, toPath = corrupt, error = null)
    }
}

@Composable
actual fun rememberSessionStore(): SessionStore = remember { IosSessionStore() }

@Composable
actual fun OnAppBackground(onBackground: () -> Unit) {
    val current by rememberUpdatedState(onBackground)
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val tokens = listOf(UIApplicationWillResignActiveNotification, UIApplicationDidEnterBackgroundNotification)
            .map { name -> center.addObserverForName(name, null, NSOperationQueue.mainQueue) { _ -> current() } }
        onDispose { tokens.forEach { center.removeObserver(it) } }
    }
}
