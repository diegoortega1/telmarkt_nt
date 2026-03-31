package com.muxunav.telmarktandroid.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muxunav.telmarktandroid.R
import com.muxunav.telmarktandroid.presentation.ui.color.LocalMuxuColors

@Composable
fun MuxuHeader(
    upperTitle: String,
    title: String = "",
    buttonText: String = "",
    showRobot: Boolean = false,
    onButtonClick: (() -> Unit)? = null,
) {
    val colors = LocalMuxuColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "fadeText")
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )

    val contentLayout: @Composable () -> Unit = {
        Text(
            text = upperTitle,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = if (showRobot) 11.sp else 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                color = colors.header
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = if (showRobot) 22.sp else 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.button,
                textAlign = TextAlign.Center
            )
        }
        if (buttonText.isNotEmpty()) {
            Button(
                onClick = { onButtonClick?.invoke() },
                enabled = onButtonClick != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.button,
                    contentColor = colors.onButton,
                    disabledContainerColor = colors.button.copy(alpha = 0.3f),
                    disabledContentColor = colors.onButton.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (onButtonClick != null) opacity else 1f)
                )
            }
        }
    }

    if (showRobot) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.head),
                contentDescription = "Robot Head",
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.Start) { contentLayout() }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            contentLayout()
        }
    }
}

