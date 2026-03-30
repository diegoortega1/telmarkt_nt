package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuBody
import com.example.telmarktnt.presentation.ui.components.MuxuHeader

@Composable
fun SelectProductScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(
            upperTitle = "OPERACIÓN EN CURSO",
            title = "Selecciona producto",
        )
        MuxuBody("Marca el número de tu elección en la máquina", "")
    }
}
