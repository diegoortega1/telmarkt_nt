package com.example.telmarktnt.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telmarktnt.presentation.ui.color.LocalMuxuColors
import com.example.telmarktnt.presentation.ui.components.MuxuBaseScreen
import com.example.telmarktnt.presentation.ui.components.MuxuBody
import com.example.telmarktnt.presentation.ui.components.MuxuHeader

@Composable
fun ProductDispensedScreen(innerPadding: PaddingValues) {
    val colors = LocalMuxuColors.current
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "OPERACIÓN FINALIZADA", title = "Producto dispensado")
        MuxuBody("", "", content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "OPERACIÓN ACEPTADA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.success,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "**** **** **** 1234\nCode: 10212\n***********12",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = colors.muted
                )
            }
        })
    }
}
