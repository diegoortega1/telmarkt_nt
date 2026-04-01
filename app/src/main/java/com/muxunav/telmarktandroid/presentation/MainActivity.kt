package com.muxunav.telmarktandroid.presentation

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
import com.muxunav.telmarktandroid.data.mdb.MdbRepositoryImpl
import com.muxunav.telmarktandroid.data.mdb.MdbService
import com.muxunav.telmarktandroid.domain.model.MdbState
import com.muxunav.telmarktandroid.presentation.main.MainViewModel
import com.muxunav.telmarktandroid.presentation.startup.StartupViewModel
import com.muxunav.telmarktandroid.presentation.ui.color.DarkMuxuColors
import com.muxunav.telmarktandroid.presentation.ui.color.LocalMuxuColors
import com.muxunav.telmarktandroid.presentation.ui.screens.AgeSelectionScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.CanInputScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.NfcReadingScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.ProductDispensedScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.ProductVerifyScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.SelectProductScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.StartupScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.TapToStartScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.VendDeniedScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.VendRequestScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.VerifyRequiredScreen
import com.muxunav.telmarktandroid.presentation.ui.screens.VerifyToStartScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var mdbRepository: MdbRepositoryImpl

    private val mainViewModel: MainViewModel by viewModels()
    private val startupViewModel: StartupViewModel by viewModels()

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
            val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()
            val mdbState by mainViewModel.uiState.collectAsStateWithLifecycle()
            var canInput by remember { mutableStateOf("") }

            CompositionLocalProvider(LocalMuxuColors provides DarkMuxuColors) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Nivel superior: startup bloqueante hasta que la config esté lista
                    if (startupState !is StartupViewModel.UiState.Done) {
                        StartupScreen(
                            title = "TELMARKT",
                            steps = when (val s = startupState) {
                                is StartupViewModel.UiState.Running -> s.steps
                                else -> emptyList()
                            },
                        )
                        return@Scaffold
                    }

                    // Startup completado — routing normal basado en estado MDB
                    val currentScreen = when (mdbState) {
                        is MdbState.Idle          -> AppScreen.TAP_TO_START
                        is MdbState.ReaderEnabled -> AppScreen.TAP_TO_START
                        is MdbState.SessionActive -> AppScreen.SELECT_PRODUCT
                        is MdbState.VendPending   -> AppScreen.VEND_REQUEST
                        is MdbState.VendSuccess   -> AppScreen.DISPENSED
                        is MdbState.VendDenied    -> AppScreen.VEND_DENIED
                        is MdbState.Error         -> AppScreen.TAP_TO_START
                    }

                    when (currentScreen) {
                        AppScreen.TAP_TO_START -> TapToStartScreen(
                            innerPadding = innerPadding,
                            readerEnabled = mdbState is MdbState.ReaderEnabled,
                            onStartSession = mainViewModel::startSession
                        )
                        AppScreen.SELECT_PRODUCT  -> SelectProductScreen(innerPadding)
                        AppScreen.VEND_REQUEST    -> {
                            val vend = mdbState as? MdbState.VendPending
                            VendRequestScreen(
                                innerPadding = innerPadding,
                                itemPrice    = vend?.itemPrice ?: 0u,
                                itemNumber   = vend?.itemNumber ?: 0u,
                                onApprove    = mainViewModel::approveVend,
                                onDeny       = mainViewModel::denyVend
                            )
                        }
                        AppScreen.DISPENSED       -> ProductDispensedScreen(innerPadding)
                        AppScreen.VEND_DENIED     -> VendDeniedScreen(innerPadding)
                        AppScreen.VERIFY_TO_START -> VerifyToStartScreen(innerPadding)
                        AppScreen.VERIFY_REQUIRED -> VerifyRequiredScreen(innerPadding)
                        AppScreen.SELECTION       -> AgeSelectionScreen(innerPadding)
                        AppScreen.CAN_INPUT       -> CanInputScreen(
                            innerPadding = innerPadding,
                            canInput     = canInput,
                            onCanChange  = { canInput = it },
                            onConfirmed  = { canInput = canInput }
                        )
                        AppScreen.NFC_READING     -> NfcReadingScreen(
                            innerPadding = innerPadding,
                            canInput     = canInput,
                            onBackToCan  = {}
                        )
                        AppScreen.PRODUCT_VERIFY  -> ProductVerifyScreen(innerPadding)
                        AppScreen.STARTUP         -> Unit // inalcanzable — gestionado arriba
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
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars())
                controller.show(WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
