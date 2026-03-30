package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuBody
import com.example.telmarktnt.presentation.ui.components.MuxuHeader

@Composable
fun TapToStartScreen(
    innerPadding: PaddingValues,
    readerEnabled: Boolean,
    onStartSession: () -> Unit,
) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(
            upperTitle = "BIENVENIDO",
            buttonText = "¡Toca para empezar!",
            onButtonClick = if (readerEnabled) onStartSession else null
        )
        MuxuBody()
    }
}
