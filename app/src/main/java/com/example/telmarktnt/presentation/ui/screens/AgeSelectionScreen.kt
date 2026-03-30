package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuHeader
import com.example.telmarktnt.presentation.ui.components.SelectionCard

@Composable
fun AgeSelectionScreen(
    innerPadding: PaddingValues,
    onSelectNfc: () -> Unit = {},
    onSelectCamera: () -> Unit = {},
) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "SELECCIONA UN MÉTODO", title = "Verificación de edad", showRobot = true)
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SelectionCard(
                title = "DNI español",
                description = "Acerca tu DNI-e al lector por NFC",
                icon = Icons.Default.Nfc,
                onClick = onSelectNfc
            )
            Spacer(modifier = Modifier.height(20.dp))
            SelectionCard(
                title = "Pasaporte / Otros IDs",
                description = "OCR sobre el documento físico",
                icon = Icons.Default.CameraAlt,
                onClick = onSelectCamera
            )
        }
    }
}

private val Modifier get() = androidx.compose.ui.Modifier
