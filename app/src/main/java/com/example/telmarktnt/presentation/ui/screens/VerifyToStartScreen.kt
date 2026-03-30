package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuBody
import com.example.telmarktnt.presentation.ui.components.MuxuHeader

@Composable
fun VerifyToStartScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "MÁQUINA DESACTIVADA", buttonText = "ACTIVAR MÁQUINA")
        MuxuBody()
    }
}
