package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuBody
import com.example.telmarktnt.presentation.ui.components.MuxuHeader

@Composable
fun ProductVerifyScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "RESTRICCIÓN POR PRODUCTO", buttonText = "VERIFICAR MI EDAD")
        MuxuBody(title = "Has seleccionado un producto restringido", subtitle = "Necesitas verificar tu edad")
    }
}
