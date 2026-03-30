package com.example.telmarktnt.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.telmarktnt.data.mdb.MdbRepositoryImpl
import com.example.telmarktnt.data.mdb.MdbService
import com.example.telmarktnt.domain.model.MdbState
import com.example.telmarktnt.presentation.main.MainViewModel
import com.example.telmarktnt.presentation.ui.color.DarkMuxuColors
import com.example.telmarktnt.presentation.ui.color.LocalMuxuColors
import com.example.telmarktnt.presentation.ui.screens.AgeSelectionScreen
import com.example.telmarktnt.presentation.ui.screens.CanInputScreen
import com.example.telmarktnt.presentation.ui.screens.NfcReadingScreen
import com.example.telmarktnt.presentation.ui.screens.ProductDispensedScreen
import com.example.telmarktnt.presentation.ui.screens.ProductVerifyScreen
import com.example.telmarktnt.presentation.ui.screens.SelectProductScreen
import com.example.telmarktnt.presentation.ui.screens.TapToStartScreen
import com.example.telmarktnt.presentation.ui.screens.VendDeniedScreen
import com.example.telmarktnt.presentation.ui.screens.VendRequestScreen
import com.example.telmarktnt.presentation.ui.screens.VerifyRequiredScreen
import com.example.telmarktnt.presentation.ui.screens.VerifyToStartScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // MdbRepositoryImpl inyectado directamente (no la interfaz) para poder
    // llamar a onServiceConnected/onServiceDisconnected desde el ServiceConnection.
    // El ViewModel usa la interfaz MdbRepository — la dependencia a la impl queda
    // contenida exclusivamente aquí.
    @Inject lateinit var mdbRepository: MdbRepositoryImpl

    private val viewModel: MainViewModel by viewModels()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as MdbService.LocalBinder).getService()
            mdbRepository.onServiceConnected(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            mdbRepository.onServiceDisconnected()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemUI()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        Intent(this, MdbService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            val mdbState by viewModel.uiState.collectAsStateWithLifecycle()
            var canInput by remember { mutableStateOf("") }

            val currentScreen = when (mdbState) {
                is MdbState.Idle -> AppScreen.TAP_TO_START
                is MdbState.ReaderEnabled -> AppScreen.TAP_TO_START
                is MdbState.SessionActive -> AppScreen.SELECT_PRODUCT
                is MdbState.VendPending -> AppScreen.VEND_REQUEST
                is MdbState.VendSuccess -> AppScreen.DISPENSED
                is MdbState.VendDenied -> AppScreen.VEND_DENIED
                is MdbState.Error -> AppScreen.TAP_TO_START
            }

            CompositionLocalProvider(LocalMuxuColors provides DarkMuxuColors) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        AppScreen.TAP_TO_START -> TapToStartScreen(
                            innerPadding = innerPadding,
                            readerEnabled = mdbState is MdbState.ReaderEnabled,
                            onStartSession = viewModel::startSession
                        )
                        AppScreen.SELECT_PRODUCT -> SelectProductScreen(innerPadding)
                        AppScreen.VEND_REQUEST -> {
                            val vend = mdbState as? MdbState.VendPending
                            VendRequestScreen(
                                innerPadding = innerPadding,
                                itemPrice = vend?.itemPrice ?: 0u,
                                itemNumber = vend?.itemNumber ?: 0u,
                                onApprove = viewModel::approveVend,
                                onDeny = viewModel::denyVend
                            )
                        }
                        AppScreen.DISPENSED -> ProductDispensedScreen(innerPadding)
                        AppScreen.VEND_DENIED -> VendDeniedScreen(innerPadding)
                        AppScreen.VERIFY_TO_START -> VerifyToStartScreen(innerPadding)
                        AppScreen.VERIFY_REQUIRED -> VerifyRequiredScreen(innerPadding)
                        AppScreen.SELECTION -> AgeSelectionScreen(innerPadding)
                        AppScreen.CAN_INPUT -> CanInputScreen(
                            innerPadding = innerPadding,
                            canInput = canInput,
                            onCanChange = { canInput = it },
                            onConfirmed = { canInput = canInput }
                        )
                        AppScreen.NFC_READING -> NfcReadingScreen(
                            innerPadding = innerPadding,
                            canInput = canInput,
                            onBackToCan = {}
                        )
                        AppScreen.PRODUCT_VERIFY -> ProductVerifyScreen(innerPadding)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 1. Permitimos que el contenido se dibuje detrás de las barras (layout fullscreen)
            window.setDecorFitsSystemWindows(false)

            window.insetsController?.let { controller ->
                // 2. Escondemos SOLO las Navigation Bars (atrás, home, etc.)
                controller.hide(WindowInsets.Type.navigationBars())

                // 3. Importante: que no se esconda la de arriba (Status Bars)
                controller.show(WindowInsets.Type.statusBars())

                // 4. Comportamiento: Si el usuario desliza desde abajo, aparecen un momento y se van
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Retrocompatibilidad para tu minSdk 24
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
