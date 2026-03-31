package com.muxunav.telmarktandroid.presentation.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.muxunav.telmarktandroid.R
import com.muxunav.telmarktandroid.presentation.ui.color.LocalMuxuColors

@Composable
fun MuxuBody(
    title: String = "Hola! Soy MuxuBot",
    subtitle: String = "Tu asistente de compra",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalMuxuColors.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.robot_king)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Robot GIF",
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.size(170.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.container),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.header,
                        textAlign = TextAlign.Center
                    )
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.button,
                        textAlign = TextAlign.Center
                    )
                }
                content()
            }
        }
    }
}
