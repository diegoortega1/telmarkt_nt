package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBaseScreen
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBody
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuHeader

@Composable
fun VerifyRequiredScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "IDENTIFICACIÓN REQUERIDA", buttonText = "VERIFICAR MI EDAD")
        MuxuBody()
    }
}
