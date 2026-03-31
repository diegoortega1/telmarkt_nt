package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muxunav.telmarktandroid.presentation.ui.color.LocalMuxuColors
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBaseScreen
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuHeader

@Composable
fun VendRequestScreen(
    innerPadding: PaddingValues,
    itemPrice: UShort,
    itemNumber: UShort,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    val colors = LocalMuxuColors.current
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "OPERACIÓN EN CURSO", title = "Solicitud de venta")
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.container),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Producto #${itemNumber.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.muted)
                    Text(text = "%.2f €".format(itemPrice.toInt() / 100.0), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = colors.header)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = colors.success, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(70.dp)
            ) {
                Text(text = "PAGAR", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDeny,
                colors = ButtonDefaults.buttonColors(containerColor = colors.button, contentColor = colors.onButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(70.dp)
            ) {
                Text(text = "DENEGAR", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
