package fr.fredonne.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerCameraPreview(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
) {
    val currentOnQr = rememberUpdatedState(onQrDetected)
    val device = remember { AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) }
    if (device == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Caméra indisponible sur cet appareil.",
                fontSize = 13.sp,
                color = Color(0xFF6B655B),
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val session = remember { AVCaptureSession() }
    val delegate = remember {
        object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(
                output: platform.AVFoundation.AVCaptureOutput,
                didOutputMetadataObjects: List<*>,
                fromConnection: platform.AVFoundation.AVCaptureConnection,
            ) {
                val qrObj = didOutputMetadataObjects
                    .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                    .firstOrNull()
                val value = qrObj?.stringValue
                if (!value.isNullOrBlank()) {
                    currentOnQr.value(value)
                }
            }
        }
    }

    DisposableEffect(session, device) {
        val input = runCatching { AVCaptureDeviceInput.deviceInputWithDevice(device, null) }.getOrNull()
        val output = AVCaptureMetadataOutput()
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
        }
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        }
        session.startRunning()
        onDispose {
            if (session.isRunning()) {
                session.stopRunning()
            }
        }
    }

    UIKitView(
        factory = {
            val container = UIView()
            val previewLayer = AVCaptureVideoPreviewLayer(session = session)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            previewLayer.frame = container.bounds
            container.layer.addSublayer(previewLayer)
            container
        },
        update = { view ->
            val sublayer = view.layer.sublayers?.firstOrNull() as? AVCaptureVideoPreviewLayer
            sublayer?.frame = view.bounds
        },
        modifier = modifier.fillMaxSize(),
    )
}
