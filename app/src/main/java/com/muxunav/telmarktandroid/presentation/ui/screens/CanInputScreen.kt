package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muxunav.telmarktandroid.presentation.ui.color.LocalMuxuColors
import com.muxunav.telmarktandroid.presentation.ui.components.AnimatedDni
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuBaseScreen
import com.muxunav.telmarktandroid.presentation.ui.components.MuxuHeader

@Composable
fun CanInputScreen(
    innerPadding: PaddingValues,
    canInput: String,
    onCanChange: (String) -> Unit,
    onConfirmed: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val colors = LocalMuxuColors.current
    MuxuBaseScreen(innerPadding) {
        MuxuHeader(upperTitle = "IDENTIFICACIÓN DNI-E", title = "Teclee el número CAN", showRobot = true)
        Spacer(modifier = Modifier.padding(top = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Esquina inferior derecha del DNI.", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.header, lineHeight = 22.sp)
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, colors.border),
                colors = CardDefaults.cardColors(containerColor = colors.container)
            ) { AnimatedDni() }
            OutlinedTextField(
                value = canInput,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        onCanChange(it)
                        if (it.length == 6) { focusManager.clearFocus(); onConfirmed() }
                    }
                },
                label = { Text("CAN (6 dígitos)", color = colors.muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.button,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.button,
                    unfocusedLabelColor = colors.muted,
                    cursorColor = colors.button,
                    focusedTextColor = colors.header,
                    unfocusedTextColor = colors.header,
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 28.sp, letterSpacing = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = colors.header)
            )
        }
    }
}

