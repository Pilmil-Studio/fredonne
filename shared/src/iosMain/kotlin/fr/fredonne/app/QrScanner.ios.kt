package fr.fredonne.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
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
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrScannerCameraPreview(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
) {
    val currentOnQr = rememberUpdatedState(onQrDetected)
    var authStatus by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))
    }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            authStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    LaunchedEffect(authStatus) {
        if (authStatus == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    authStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                }
            }
        }
    }

    if (authStatus != AVAuthorizationStatusAuthorized) {
        val message = when (authStatus) {
            AVAuthorizationStatusNotDetermined ->
                "Demande d'autorisation d'accès à la caméra en cours…"
            AVAuthorizationStatusRestricted ->
                "L'accès à la caméra est restreint sur cet appareil."
            AVAuthorizationStatusDenied ->
                "Accès à la caméra refusé. Autorisez la caméra dans Réglages > Fredonne pour scanner un QR code."
            else ->
                "Autorisation d'accès à la caméra requise pour scanner un QR code."
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color(0xFF6B655B),
                    textAlign = TextAlign.Center,
                )
                if (authStatus == AVAuthorizationStatusDenied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                                UIApplication.sharedApplication.openURL(
                                    url,
                                    options = emptyMap<Any?, Any>(),
                                    completionHandler = null,
                                )
                            }
                        },
                    ) {
                        Text("Ouvrir les Réglages")
                    }
                }
            }
        }
        return
    }

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
