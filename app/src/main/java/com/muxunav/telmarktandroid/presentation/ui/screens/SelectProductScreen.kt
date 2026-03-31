package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBaseScreen
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBody
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuHeader

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
