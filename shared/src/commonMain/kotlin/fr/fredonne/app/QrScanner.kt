package fr.fredonne.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.fredonne.core.music.QrChunker

@Composable
expect fun QrScannerCameraPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (String) -> Unit,
)

@Composable
fun QrScannerDialog(
    onPayloadAssembled: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val assembler = remember { QrChunker.Assembler() }
    var statusText by remember {
        mutableStateOf("Visez le QR code sur une partition PDF ou un autre écran (format ABC ou FR1).")
    }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastRaw by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scanner un QR code") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    QrScannerCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQrDetected = { raw ->
                            if (raw == lastRaw) return@QrScannerCameraPreview
                            lastRaw = raw
                            when (val res = assembler.accept(raw)) {
                                is QrChunker.AssemblyResult.Complete -> {
                                    onPayloadAssembled(res.payload)
                                }
                                is QrChunker.AssemblyResult.Partial -> {
                                    progress = res.collected to res.total
                                    statusText = "Fragment ${res.collected}/${res.total} capturé — passez au QR code suivant."
                                }
                                is QrChunker.AssemblyResult.Invalid -> {
                                    statusText = res.reason
                                }
                            }
                        },
                    )
                }

                progress?.let { (collected, total) ->
                    LinearProgressIndicator(
                        progress = { collected.toFloat() / total.toFloat().coerceAtLeast(1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = if (progress != null) FontWeight.Medium else FontWeight.Normal,
                    color = Color(0xFF6B655B),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
    )
}
