package com.muxunav.telmarktandroid.integration

import androidx.lifecycle.viewModelScope
import com.muxunav.telmarktandroid.data.mdb.MdbFrameProcessor
import com.muxunav.telmarktandroid.data.mdb.MdbProtocolHandler
import com.muxunav.telmarktandroid.data.mdb.MdbRepositoryImpl
import com.muxunav.telmarktandroid.data.mdb.MdbService
import com.muxunav.telmarktandroid.domain.model.MdbState
import com.muxunav.telmarktandroid.domain.usecase.ApproveVendUseCase
import com.muxunav.telmarktandroid.domain.usecase.BeginSessionUseCase
import com.muxunav.telmarktandroid.domain.usecase.DenyVendUseCase
import com.muxunav.telmarktandroid.presentation.main.MainViewModel
import com.muxunav.telmarktandroid.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests de integración del flujo MDB completo.
 *
 * Verifican toda la cadena sin mocks intermedios:
 *   bytes VMC → MdbProtocolHandler → MdbFrameProcessor → MdbState
 *   → MdbRepositoryImpl → MainViewModel.uiState
 *
 * También verifican los frames enviados de vuelta al hardware (onWrite),
 * que en producción corresponden a las llamadas mdbWrite con <5ms.
 *
 * El único mock es MdbService (Android Service — no instanciable en JUnit).
 * Su comportamiento está completamente delegado al MdbFrameProcessor real.
 */
class MdbFlowIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repoScope = CoroutineScope(testDispatcher + SupervisorJob())

    // Implementaciones reales — sin mocks de lógica
    private val handler = MdbProtocolHandler()
    private val writtenFrames = mutableListOf<ShortArray>()
    private val hardwareState = MutableStateFlow<MdbState>(MdbState.Idle)

    private lateinit var processor: MdbFrameProcessor
    private lateinit var viewModel: MainViewModel

    @Before fun setUp() {
        writtenFrames.clear()

        processor = MdbFrameProcessor(
            handler       = handler,
            onStateChange = { hardwareState.value = it },
            onWrite       = { data -> writtenFrames.add(data.copyOf()) },
        )

        // Mock solo del Android Service — su API pública delega al processor real
        val fakeService = mockk<MdbService>(relaxed = true) {
            every { state }        returns hardwareState
            every { beginSession() } answers { processor.beginSession() }
            every { approveVend() }  answers { processor.approveVend() }
            every { denyVend() }     answers { processor.denyVend() }
        }

        val repository = MdbRepositoryImpl(repoScope)
        repository.onServiceConnected(fakeService)

        viewModel = MainViewModel(
            repository          = repository,
            beginSessionUseCase = BeginSessionUseCase(repository),
            approveVendUseCase  = ApproveVendUseCase(repository),
            denyVendUseCase     = DenyVendUseCase(repository),
        )
    }

    @After fun tearDown() {
        viewModel.viewModelScope.cancel()
        repoScope.cancel()
    }

    /**
     * Simula el envío de una trama desde el VMC.
     * Los literales Int como 0x113, 0xE8 se convierten a Short igual que
     * el hardware los produce (valores en rango [0, 0x1FF]).
     */
    private fun vmcSend(vararg words: Int) {
        val buf = ShortArray(words.size) { words[it].toShort() }
        processor.processFrame(buf, buf.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flujo aprobado: ENABLE → SESSION → VEND_REQUEST → APPROVED → SUCCESS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `flujo aprobado completo - ENABLE SESSION VEND_REQUEST APPROVED VEND_SUCCESS`() = runTest {
        // 1. VMC habilita el reader → UI pasa a ReaderEnabled
        vmcSend(0x114, 0x01)
        assertEquals(MdbState.ReaderEnabled, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.ackData))

        // 2. Usuario inicia sesión (tap en pantalla → ViewModel → UseCase → Processor)
        viewModel.startSession()

        // 3. VMC hace POLL → processor despacha BeginSession
        vmcSend(0x112, 0x12)
        assertEquals(MdbState.SessionActive, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.beginSessionData))

        // 4. VMC envía VendRequest: precio 10.00 (0x03E8 = 1000 centavos), ítem 3
        vmcSend(0x113, 0x00, 0x03, 0xE8, 0x00, 0x03)
        val pending = viewModel.uiState.value
        assertTrue(pending is MdbState.VendPending)
        assertEquals(1000.toUShort(), (pending as MdbState.VendPending).itemPrice)
        assertEquals(3.toUShort(), pending.itemNumber)
        // El hardware recibió ACK inmediatamente (constraint <5ms)
        assertTrue(writtenFrames.last().contentEquals(handler.ackData))

        // 5. Usuario aprueba el vend → awaitingUserApproval = false
        viewModel.approveVend()

        // 6. VMC hace POLL → processor despacha VendApproved con los mismos amounts
        vmcSend(0x112, 0x12)
        val expectedApproved = handler.buildVendApproved(0x03.toShort(), 0xE8.toShort())
        assertTrue(writtenFrames.last().contentEquals(expectedApproved))

        // 7. VMC confirma dispensación exitosa
        vmcSend(0x113, 0x02)
        assertEquals(MdbState.VendSuccess, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.ackData))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flujo denegado: ENABLE → SESSION → VEND_REQUEST → DENIED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `flujo denegado - ENABLE SESSION VEND_REQUEST DENIED`() = runTest {
        vmcSend(0x114, 0x01)
        viewModel.startSession()
        vmcSend(0x112, 0x12)

        // VendRequest: precio 1.00 (0x0064 = 100 centavos), ítem 7
        vmcSend(0x113, 0x00, 0x00, 0x64, 0x00, 0x07)
        assertTrue(viewModel.uiState.value is MdbState.VendPending)
        assertEquals(100.toUShort(), (viewModel.uiState.value as MdbState.VendPending).itemPrice)
        assertEquals(7.toUShort(),   (viewModel.uiState.value as MdbState.VendPending).itemNumber)

        viewModel.denyVend()

        // POLL → VendDenied enviado al hardware
        vmcSend(0x112, 0x12)
        assertEquals(MdbState.VendDenied, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.vendDenied))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VendCancel: el VMC cancela la transacción antes de que el usuario decida
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `flujo VendCancel - VMC cancela el vend`() = runTest {
        vmcSend(0x114, 0x01)
        viewModel.startSession()
        vmcSend(0x112, 0x12)

        // precio 4.00 (0x0190 = 400 centavos), ítem 2
        vmcSend(0x113, 0x00, 0x01, 0x90, 0x00, 0x02)
        assertTrue(viewModel.uiState.value is MdbState.VendPending)

        // VMC envía VendCancel → volvemos a ReaderEnabled
        vmcSend(0x113, 0x01)
        assertEquals(MdbState.ReaderEnabled, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.vendDenied))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VendFailure: el usuario aprueba pero la máquina falla al dispensar
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `flujo VendFailure - maquina falla al dispensar`() = runTest {
        vmcSend(0x114, 0x01)
        viewModel.startSession()
        vmcSend(0x112, 0x12)

        // precio 0.50 (0x0032 = 50 centavos), ítem 1
        vmcSend(0x113, 0x00, 0x00, 0x32, 0x00, 0x01)
        viewModel.approveVend()
        vmcSend(0x112, 0x12) // POLL → VendApproved enviado

        // La máquina no pudo dispensar
        vmcSend(0x113, 0x03) // VendFailure
        assertEquals(MdbState.ReaderEnabled, viewModel.uiState.value)
        assertTrue(writtenFrames.last().contentEquals(handler.ackData))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POLL con awaitingUserApproval activo: debe responder ACK, no VendApproved
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `POLL durante VendPending sin decision del usuario responde ACK`() = runTest {
        vmcSend(0x114, 0x01)
        viewModel.startSession()
        vmcSend(0x112, 0x12)

        vmcSend(0x113, 0x00, 0x00, 0x64, 0x00, 0x01)
        assertTrue(viewModel.uiState.value is MdbState.VendPending)

        // El usuario todavía no decidió — el POLL debe recibir ACK, no VendApproved
        vmcSend(0x112, 0x12)
        assertTrue(writtenFrames.last().contentEquals(handler.ackData))
        // Estado sigue en VendPending
        assertTrue(viewModel.uiState.value is MdbState.VendPending)
    }
}
