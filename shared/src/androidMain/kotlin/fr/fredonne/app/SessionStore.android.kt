package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileSessionStore(private val dir: File) : SessionStore {

    private val file get() = File(dir, "current-score.txt")

    override fun read(): String? = if (file.isFile) file.readText(Charsets.UTF_8) else null

    override fun write(text: String) {
        dir.mkdirs()
        val tmp = File(dir, "current-score.txt.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun setAsideCorrupt() {
        if (!file.exists()) return
        Files.move(file.toPath(), File(dir, "current-score.corrupt").toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

@Composable
actual fun rememberSessionStore(): SessionStore {
    val context = LocalContext.current
    return remember { FileSessionStore(File(context.applicationContext.filesDir, "session")) }
}

@Composable
actual fun OnAppBackground(onBackground: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(onBackground)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) current()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
