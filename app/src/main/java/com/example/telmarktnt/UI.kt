package com.example.telmarktnt

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlin.let
import kotlin.text.all
import kotlin.text.isDigit
import kotlin.text.isNotEmpty

enum class AppScreen {
  TAP_TO_START,
  VERIFY_TO_START,
  VERIFY_REQUIRED,
  SELECTION,
  CAN_INPUT,
  NFC_READING,
  PRODUCT_VERIFY,
  DISPENSED,
  SELECT_PRODUCT,
}

data class MuxuColorScheme(
  val background: Color,
  val header: Color,      // Azul oscuro (#1a1a2e en Light)
  val muted: Color,       // Gris (#888780 en Light)
  val button: Color,      // Rojo (#C9302C en Light)
  val onButton: Color,    // Blanco
  val container: Color,   // Cristal
  val border: Color,      // Borde
  val success: Color
)

val DarkMuxuColors = MuxuColorScheme(
  background = Color(0xFF151921),
  header = Color(0xFFE6F1FB).copy(alpha = 0.7f),
  muted = Color(0xFFE6F1FB).copy(alpha = 0.5f),
  button = Color(0xFFE24B4A),
  onButton = Color.White,
  container = Color.White.copy(alpha = 0.05f),
  border = Color.White.copy(alpha = 0.1f),
  success = Color(0xFF0F6E56)
)

val LightMuxuColors = MuxuColorScheme(
  background = Color.White,
  header = Color.Black,
  muted = Color(0xFF888780),
  button = Color(0xFFC9302C),
  onButton = Color.White,
  container = Color.Black.copy(alpha = 0.04f),
  border = Color.Black.copy(alpha = 0.1f),
  success = Color(0xFF0F6E56)
)

val LocalMuxuColors = staticCompositionLocalOf { DarkMuxuColors }

class MainActivity : ComponentActivity() {
  private fun hideSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Para Android 11+
      window.setDecorFitsSystemWindows(false)
      window.insetsController?.let { controller ->
        // CAMBIO AQUÍ: Solo ocultamos navigationBars, NO statusBars
        controller.hide(WindowInsets.Type.navigationBars())

        // Esto permite que aparezcan si el usuario desliza desde abajo
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    } else {
      // Para versiones antiguas
      @Suppress("DEPRECATION")
      window.decorView.systemUiVisibility = (
              View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                      or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Oculta solo la barra de navegación
                      or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              )
    }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    hideSystemUI()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
    setContent {
      var currentScreen by remember { mutableStateOf(AppScreen.TAP_TO_START) }
      var canInput by remember { mutableStateOf("") }
      var isLightMode by remember { mutableStateOf(false) } // MODO DINÁMICO PARA CARRUSEL
      val colors = if (isLightMode) LightMuxuColors else DarkMuxuColors

      CompositionLocalProvider(LocalMuxuColors provides colors) {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .clickable {
              currentScreen = when (currentScreen) {
                AppScreen.TAP_TO_START -> AppScreen.SELECT_PRODUCT
                AppScreen.SELECT_PRODUCT -> AppScreen.VERIFY_TO_START
                AppScreen.VERIFY_TO_START -> AppScreen.VERIFY_REQUIRED
                AppScreen.VERIFY_REQUIRED -> AppScreen.SELECTION
                AppScreen.SELECTION -> AppScreen.CAN_INPUT
                AppScreen.CAN_INPUT -> AppScreen.NFC_READING
                AppScreen.NFC_READING -> AppScreen.PRODUCT_VERIFY
                AppScreen.PRODUCT_VERIFY -> AppScreen.DISPENSED
                AppScreen.DISPENSED -> {
                  isLightMode = !isLightMode
                  AppScreen.TAP_TO_START
                }
              }
            }
        ) { innerPadding ->
          when (currentScreen) {
            AppScreen.TAP_TO_START -> TapToStart(innerPadding)
            AppScreen.VERIFY_TO_START -> VerifyToStart(innerPadding)
            AppScreen.VERIFY_REQUIRED -> VerifyRequired(innerPadding)
            AppScreen.SELECTION -> SelectionScreen(innerPadding)
            AppScreen.CAN_INPUT -> CanInputScreen(
              innerPadding,
              canInput,
              onCanChange = { canInput = it },
              onConfirmed = { currentScreen = AppScreen.NFC_READING })

            AppScreen.NFC_READING -> NfcReadingScreen(
              innerPadding,
              canInput,
              onBackToCan = { currentScreen = AppScreen.CAN_INPUT })

            AppScreen.PRODUCT_VERIFY -> ProductVerifyRequired(innerPadding)
            AppScreen.DISPENSED -> ProductDispensed(innerPadding)
            AppScreen.SELECT_PRODUCT -> SelectProduct(innerPadding)
          }
        }
      }
    }
  }

  @Composable
  fun TapToStart(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "BIENVENIDO",
        buttonText = "¡Toca para empezar!",
      )
      MuxuBody()
    }
  }

  @Composable
  fun VerifyToStart(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "MÁQUINA DESACTIVADA",
        buttonText = "ACTIVAR MÁQUINA",
      )
      MuxuBody()

    }
  }

  @Composable
  fun VerifyRequired(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "IDENTIFICACIÓN REQUERIDA",
        buttonText = "VERIFICAR MI EDAD",
      )
      MuxuBody()

    }
  }

  @Composable
  fun SelectionScreen(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "SELECCIONA UN MÉTODO",
        title = "Verificación de edad",
        showRobot = true
      )

      Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        SelectionCard(
          title = "DNI español",
          description = "Acerca tu DNI-e al lector por NFC",
          icon = Icons.Default.Nfc,
        )
        Spacer(modifier = Modifier.height(20.dp))
        SelectionCard(
          title = "Pasaporte / Otros IDs",
          description = "OCR sobre el documento físico",
          icon = Icons.Default.CameraAlt,
        )
      }


    }
  }

  @Composable
  fun SelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
  ) {

    val colors = LocalMuxuColors.current
    Card(
      onClick = { /* Acción */ },
      colors = CardDefaults.cardColors(
        containerColor = colors.container
      ),
      border = BorderStroke(0.5.dp, colors.border),
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(colors.button.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.button,
            modifier = Modifier.size(30.dp)
          )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
          Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.header
          )
          Text(
            text = description,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colors.muted
          )
        }
      }
    }
  }

  @Composable
  fun ProductVerifyRequired(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "RESTRICCIÓN POR PRODUCTO",
        buttonText = "VERIFICAR MI EDAD",
      )
      MuxuBody(title = "Has seleccionado un producto restringido", "Necesitas verificar tu edad")

    }
  }


  @Composable
  fun ProductDispensed(innerPadding: PaddingValues) {
    val colors = LocalMuxuColors.current
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "OPERACIÓN FINALIZADA",
        title = "Producto dispensado"
      )
      MuxuBody(
        "", "",
        content = {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
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
        }
      )

    }
  }

  @Composable
  fun FooterBrandText(text: String, color: Color) {
    Text(
      text = text,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.5.sp,
      color = color
    )
  }

  @Composable
  fun SelectProduct(innerPadding: PaddingValues) {
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "OPERACIÓN EN CURSO",
        title = "Selecciona producto",
      )
      MuxuBody("Marca el número de tu elección en la máquina", "")

    }
  }

  enum class NfcStatus { READING, DO_NOT_REMOVE, SUCCESS }

  @Composable
  fun NfcReadingScreen(
    innerPadding: PaddingValues,
    canInput: String,
    onBackToCan: () -> Unit
  ) {
    var currentStatus by remember { mutableStateOf(NfcStatus.READING) }
    val colors = LocalMuxuColors.current
    LaunchedEffect(Unit) {
      delay(5000)
      currentStatus = NfcStatus.DO_NOT_REMOVE
      delay(5000)
      currentStatus = NfcStatus.SUCCESS
    }

    val infiniteTransition = rememberInfiniteTransition(label = "nfcAnim")
    val waveScale1 by infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 2.5f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "wave1"
    )
    val waveAlpha1 by infiniteTransition.animateFloat(
      initialValue = 0.5f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "alpha1"
    )

    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "IDENTIFICACIÓN NFC",
        title = when (currentStatus) {
          NfcStatus.READING -> "Acerca el DNI aquí"
          NfcStatus.DO_NOT_REMOVE -> "No retires el DNI"
          NfcStatus.SUCCESS -> "DNI leido con éxito"
        },
        showRobot = true
      )

      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
      ) {
        if (currentStatus != NfcStatus.SUCCESS) {
          Box(
            modifier = Modifier
              .size(100.dp)
              .graphicsLayer {
                scaleX = waveScale1
                scaleY = waveScale1
                alpha = waveAlpha1
              }
              .border(
                1.5.dp,
                colors.button.copy(alpha = waveAlpha1),
                RoundedCornerShape(50)
              )
          )
          Box(
            modifier = Modifier
              .size(100.dp)
              .graphicsLayer {
                val delayedScale =
                  if (waveScale1 > 1.75f) waveScale1 - 0.75f else waveScale1 + 0.75f
                scaleX = delayedScale
                scaleY = delayedScale
                alpha =
                  if (delayedScale > 1f) 0.5f * (1f - (delayedScale - 1f) / 1.5f) else 0f
              }
              .border(
                1.dp,
                colors.button.copy(alpha = if (waveAlpha1 > 0.2f) waveAlpha1 - 0.2f else 0f),
                RoundedCornerShape(50)
              )
          )
        }

        Icon(
          imageVector = if (currentStatus == NfcStatus.SUCCESS) Icons.Default.Check else Icons.Default.CreditCard,
          contentDescription = "Card",
          tint = if (currentStatus == NfcStatus.SUCCESS) colors.success else colors.button,
          modifier = Modifier.size(80.dp)
        )
        Icon(
          imageVector = Icons.Default.Nfc,
          contentDescription = "NFC",
          tint = if (currentStatus == NfcStatus.SUCCESS) colors.success else colors.header.copy(
            alpha = 0.5f
          ),
          modifier = Modifier
            .size(30.dp)
            .align(Alignment.TopEnd)
            .padding(12.dp)
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 48.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text("CAN: ", color = colors.muted, fontSize = 14.sp)
          Text(
            text = canInput,
            color = colors.button,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
          Spacer(modifier = Modifier.width(12.dp))
          TextButton(
            onClick = onBackToCan,
            contentPadding = PaddingValues(0.dp)
          ) {
            Text(
              "Cambiar",
              color = colors.header,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      Card(
        colors = CardDefaults.cardColors(containerColor = colors.container),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.Start
        ) {
          StatusMessage(
            text = "Leyendo datos del DNI...",
            icon = Icons.Default.Refresh,
            color = colors.header,
            opacity = if (currentStatus == NfcStatus.READING) 1f else 0.3f
          )
          Spacer(modifier = Modifier.height(12.dp))
          StatusMessage(
            text = "No retire el DNI",
            icon = Icons.Default.Warning,
            color = colors.button,
            opacity = if (currentStatus == NfcStatus.DO_NOT_REMOVE) 1f else 0.3f
          )
          Spacer(modifier = Modifier.height(12.dp))
          StatusMessage(
            text = "DNI leído correctamente",
            icon = Icons.Default.Check,
            color = colors.success,
            opacity = if (currentStatus == NfcStatus.SUCCESS) 1f else 0.3f
          )
        }
      }


    }
  }

  @Composable
  fun StatusMessage(text: String, icon: ImageVector, color: Color, opacity: Float) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .alpha(opacity),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = color
      )
    }
  }

  @Composable
  fun CanInputScreen(
    innerPadding: PaddingValues,
    canInput: String,
    onCanChange: (String) -> Unit,
    onConfirmed: () -> Unit
  ) {
    val focusManager = LocalFocusManager.current
    val colors = LocalMuxuColors.current
    MuxuBaseScreen(innerPadding) {
      MuxuHeader(
        upperTitle = "IDENTIFICACIÓN DNI-E",
        title = "Teclee el número CAN",
        showRobot = true
      )

      Spacer(modifier = Modifier.padding(top = 12.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Esquina inferior derecha del DNI.",
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
          color = colors.header,
          lineHeight = 22.sp
        )
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, colors.border),
          colors = CardDefaults.cardColors(containerColor = colors.container)
        ) {
          AnimatedDni()
        }

        OutlinedTextField(
          value = canInput,
          onValueChange = {
            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
              onCanChange(it)
              if (it.length == 6) {
                focusManager.clearFocus()
                onConfirmed()
              }
            }
          },
          label = { Text("CAN (6 dígitos)", color = colors.muted) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.button,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.button,
            unfocusedLabelColor = colors.muted,
            cursorColor = colors.button,
            focusedTextColor = colors.header,
            unfocusedTextColor = colors.header,
          ),
          modifier = Modifier
            .fillMaxWidth(),
          textStyle = TextStyle(
            fontSize = 28.sp,
            letterSpacing = 10.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = colors.header
          )
        )
      }


    }
  }

  @Composable
  fun AnimatedDni() {
    val infiniteTransition = rememberInfiniteTransition(label = "DniZoomTransition")
    val scale by infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.4f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "DniScaleAnimation"
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .clipToBounds(),
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.dni), // Asegúrate de que el ID es correcto
        contentDescription = "Ejemplo de DNI CAN",
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin =
              TransformOrigin(0.9f, 0.9f) // Apunta a los 2/3 abajo de la derecha
          },
        contentScale = ContentScale.Crop
      )
    }
  }

  @Composable
  fun MuxuBaseScreen(
    innerPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
  ) {
    val colors = LocalMuxuColors.current
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(colors.background)
        .padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .padding(horizontal = 12.dp, vertical = 16.dp)
          .weight(1f)
          .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
      )
      MuxuFooter()
    }
  }

  @Composable
  fun MuxuHeader(
    upperTitle: String,
    title: String = "",
    buttonText: String = "",
    showRobot: Boolean = false,
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
          onClick = { /* Acción */ },
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.button,
            contentColor = colors.onButton
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
            color = colors.onButton,
            modifier = Modifier.alpha(opacity)
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
        Column(horizontalAlignment = Alignment.Start) {
          contentLayout()
        }
      }
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().height(100.dp)) {
        contentLayout()
      }
    }
  }

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
          if (Build.VERSION.SDK_INT >= 28) {
            add(AnimatedImageDecoder.Factory())
          } else {
            add(GifDecoder.Factory())
          }
        }
        .build()
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
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
        modifier = Modifier
          .fillMaxWidth(),
        border = BorderStroke(1.dp, colors.border)
      ) {
        Column(
          modifier = Modifier
            .padding(vertical = 18.dp, horizontal = 10.dp)
            .fillMaxWidth(),
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

  @Composable
  fun MuxuFooter() {
    val colors = LocalMuxuColors.current
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      FooterBrandText("MUXUPAY", colors.muted)
      Box(
        modifier = Modifier
          .size(4.dp)
          .background(colors.button, RoundedCornerShape(50))
      )
      FooterBrandText("MUXUNAV", colors.muted)
    }
  }


  @Preview(showBackground = true)
  @Composable
  fun GreetingPreview() {
    CompositionLocalProvider(LocalMuxuColors provides DarkMuxuColors) {
      Scaffold { innerPadding -> TapToStart(innerPadding) }
    }
  }
}
