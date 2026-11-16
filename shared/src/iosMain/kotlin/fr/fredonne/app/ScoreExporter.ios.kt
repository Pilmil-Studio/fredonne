@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package fr.fredonne.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

class IosScoreExporter : ScoreExporter {

    override fun exportBytes(fileName: String, mimeType: String, bytes: ByteArray): String {
        val path = NSTemporaryDirectory() + fileName
        if (bytes.isNotEmpty()) {
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data?.writeToFile(path, atomically = true)
        }
        val url = NSURL.fileURLWithPath(path)
        val rootVc = findRootViewController()
        if (rootVc != null) {
            val activityVc = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
            activityVc.popoverPresentationController?.let { popover ->
                popover.sourceView = rootVc.view
                rootVc.view.bounds.useContents {
                    popover.sourceRect = CGRectMake(
                        size.width / 2.0,
                        size.height / 2.0,
                        0.0,
                        0.0,
                    )
                }
                popover.permittedArrowDirections = 0uL
            }
            rootVc.presentViewController(activityVc, animated = true, completion = null)
        }
        return path
    }

    @Suppress("DEPRECATION")
    private fun findRootViewController(): UIViewController? {
        val app = UIApplication.sharedApplication
        val scenes = app.connectedScenes.filterIsInstance<UIWindowScene>()
        val activeScene = scenes.firstOrNull {
            it.activationState == UISceneActivationStateForegroundActive
        } ?: scenes.firstOrNull()

        val window = activeScene?.windows
            ?.filterIsInstance<UIWindow>()
            ?.firstOrNull { it.isKeyWindow() }
            ?: activeScene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull()
            ?: app.keyWindow

        var vc = window?.rootViewController
        while (vc?.presentedViewController != null) {
            vc = vc.presentedViewController
        }
        return vc
    }
}

@Composable
actual fun rememberScoreExporter(): ScoreExporter = remember { IosScoreExporter() }
