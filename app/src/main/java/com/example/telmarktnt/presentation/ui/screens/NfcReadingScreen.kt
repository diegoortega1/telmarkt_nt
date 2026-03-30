package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telmarktnt.presentation.ui.color.LocalMuxuColors
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuHeader
import com.example.telmarktnt.presentation.ui.components.StatusMessage

enum class NfcStatus { READING, DO_NOT_REMOVE, SUCCESS }

@Composable
fun NfcReadingScreen(
    innerPadding: PaddingValues,
    canInput: String,
    onBackToCan: () -> Unit,
) {
    var currentStatus by remember { mutableStateOf(NfcStatus.READING) }
    val colors = LocalMuxuColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "nfcAnim")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wave1"
    )
    val waveAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "alpha1"
    )

    MuxuBaseScreen(innerPadding) {
        MuxuHeader(
            upperTitle = "IDENTIFICACIÓN NFC",
            title = when (currentStatus) {
                NfcStatus.READING -> "Acerca el DNI aquí"
                NfcStatus.DO_NOT_REMOVE -> "No retires el DNI"
                NfcStatus.SUCCESS -> "DNI leido con éxito"
            },
            showRobot = true
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (currentStatus != NfcStatus.SUCCESS) {
                Box(modifier = Modifier.size(100.dp).graphicsLayer { scaleX = waveScale1; scaleY = waveScale1; alpha = waveAlpha1 }.border(1.5.dp, colors.button.copy(alpha = waveAlpha1), RoundedCornerShape(50)))
                Box(modifier = Modifier.size(100.dp).graphicsLayer {
                    val d = if (waveScale1 > 1.75f) waveScale1 - 0.75f else waveScale1 + 0.75f
                    scaleX = d; scaleY = d; alpha = if (d > 1f) 0.5f * (1f - (d - 1f) / 1.5f) else 0f
                }.border(1.dp, colors.button.copy(alpha = if (waveAlpha1 > 0.2f) waveAlpha1 - 0.2f else 0f), RoundedCornerShape(50)))
            }
            Icon(imageVector = if (currentStatus == NfcStatus.SUCCESS) Icons.Default.Check else Icons.Default.CreditCard, contentDescription = null, tint = if (currentStatus == NfcStatus.SUCCESS) colors.success else colors.button, modifier = Modifier.size(80.dp))
            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, tint = if (currentStatus == NfcStatus.SUCCESS) colors.success else colors.header.copy(alpha = 0.5f), modifier = Modifier.size(30.dp).align(Alignment.TopEnd).padding(12.dp))
        }
        Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp), color = colors.container, border = BorderStroke(1.dp, colors.border)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("CAN: ", color = colors.muted, fontSize = 14.sp)
                Text(canInput, color = colors.button, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = onBackToCan, contentPadding = PaddingValues(0.dp)) { Text("Cambiar", color = colors.header, fontSize = 13.sp) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.container), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, colors.border), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                StatusMessage("Leyendo datos del DNI...", Icons.Default.Refresh, colors.header, if (currentStatus == NfcStatus.READING) 1f else 0.3f)
                Spacer(modifier = Modifier.height(12.dp))
                StatusMessage("No retire el DNI", Icons.Default.Warning, colors.button, if (currentStatus == NfcStatus.DO_NOT_REMOVE) 1f else 0.3f)
                Spacer(modifier = Modifier.height(12.dp))
                StatusMessage("DNI leído correctamente", Icons.Default.Check, colors.success, if (currentStatus == NfcStatus.SUCCESS) 1f else 0.3f)
            }
        }
    }
}
