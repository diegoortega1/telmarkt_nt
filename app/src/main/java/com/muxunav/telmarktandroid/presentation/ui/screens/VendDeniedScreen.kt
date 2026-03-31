package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBaseScreen
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBody
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuHeader

@Composable
fun VendDeniedScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "OPERACIÓN CANCELADA", title = "Venta denegada")
        MuxuBody(title = "Operación cancelada", subtitle = "La venta ha sido rechazada")
    }
}
