package com.muxunav.telmarktandroid.presentation.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muxunav.telmarktandroid.domain.model.SequentialStep
import com.muxunav.telmarktandroid.domain.model.SequentialStep.StepStatus

private val ColorPending = Color(0xFF666666)
private val ColorRunning = Color(0xFF4FC3F7)
private val ColorDone    = Color(0xFF81C784)
private val ColorError   = Color(0xFFE57373)

/**
 * Pantalla genérica de proceso secuencial con feedback visual por pasos.
 * Acepta cualquier lista de [SequentialStep] — no está acoplada al startup
 * ni a ningún caso de uso concreto.
 */
@Composable
fun StartupScreen(
    title: String,
    steps: List<SequentialStep>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(48.dp))

            if (steps.isEmpty()) {
                CircularProgressIndicator(
                    color = ColorRunning,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    steps.forEach { step -> StepRow(step) }
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: SequentialStep) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        StepIcon(step.status)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = step.label,
                color = when (step.status) {
                    StepStatus.PENDING -> ColorPending
                    StepStatus.RUNNING -> Color.White
                    StepStatus.DONE    -> ColorDone
                    StepStatus.ERROR   -> ColorError
                },
                fontSize = 15.sp,
            )
            step.errorMessage?.let {
                Text(text = it, color = ColorError, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StepIcon(status: StepStatus) {
    val size = Modifier.size(20.dp)
    when (status) {
        StepStatus.PENDING -> Icon(
            imageVector = Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = ColorPending,
            modifier = size,
        )
        StepStatus.RUNNING -> {
            val transition = rememberInfiniteTransition(label = "spin")
            val angle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                label = "spin_angle",
            )
            CircularProgressIndicator(
                color = ColorRunning,
                modifier = size.rotate(angle),
                strokeWidth = 2.dp,
            )
        }
        StepStatus.DONE -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ColorDone,
            modifier = size,
        )
        StepStatus.ERROR -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = ColorError,
            modifier = size,
        )
    }
}
