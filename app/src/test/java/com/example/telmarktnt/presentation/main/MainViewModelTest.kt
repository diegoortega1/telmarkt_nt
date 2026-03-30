package com.example.telmarktnt.presentation.main

import app.cash.turbine.test
import com.example.telmarktnt.domain.model.MdbState
import com.example.telmarktnt.domain.repository.MdbRepository
import com.example.telmarktnt.domain.usecase.ApproveVendUseCase
import com.example.telmarktnt.domain.usecase.BeginSessionUseCase
import com.example.telmarktnt.domain.usecase.DenyVendUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val VEND_SUCCESS_DISPLAY_MS = 5_000L
private const val VEND_DENIED_DISPLAY_MS  = 6_000L

class MainViewModelTest {

    private lateinit var fakeHardwareState: MutableStateFlow<MdbState>
    private lateinit var mockRepository: MdbRepository
    private lateinit var mockBeginSession: BeginSessionUseCase
    private lateinit var mockApproveVend: ApproveVendUseCase
    private lateinit var mockDenyVend: DenyVendUseCase
    private lateinit var viewModel: MainViewModel

    @Before fun setUp() {
        fakeHardwareState = MutableStateFlow(MdbState.Idle)
        mockRepository    = mockk { every { state } returns fakeHardwareState }
        mockBeginSession  = mockk(relaxed = true)
        mockApproveVend   = mockk(relaxed = true)
        mockDenyVend      = mockk(relaxed = true)
    }

    private fun buildViewModel() = MainViewModel(
        repository        = mockRepository,
        beginSessionUseCase = mockBeginSession,
        approveVendUseCase  = mockApproveVend,
        denyVendUseCase     = mockDenyVend,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Estado inicial
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `uiState inicial es Idle`() = runTest(UnconfinedTestDispatcher()) {
        viewModel = buildViewModel()
        assertEquals(MdbState.Idle, viewModel.uiState.value)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Propagación normal de estado
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `uiState refleja cambios normales del hardware inmediatamente`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            viewModel.uiState.test {
                assertEquals(MdbState.Idle, awaitItem())

                fakeHardwareState.value = MdbState.ReaderEnabled
                assertEquals(MdbState.ReaderEnabled, awaitItem())

                fakeHardwareState.value = MdbState.SessionActive
                assertEquals(MdbState.SessionActive, awaitItem())

                val vend = MdbState.VendPending(itemPrice = 100u, itemNumber = 5u)
                fakeHardwareState.value = vend
                assertEquals(vend, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // VendSuccess — delay de display
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `VendSuccess aparece en uiState inmediatamente`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendSuccess
            assertEquals(MdbState.VendSuccess, viewModel.uiState.value)
        }

    @Test fun `uiState revierte al estado hardware después del delay de VendSuccess`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendSuccess

            advanceTimeBy(VEND_SUCCESS_DISPLAY_MS + 1)

            // Tras el delay, el hardware ya pasó a ReaderEnabled
            fakeHardwareState.value = MdbState.ReaderEnabled
            assertEquals(MdbState.ReaderEnabled, viewModel.uiState.value)
        }

    @Test fun `cambios de hardware durante delay VendSuccess no actualizan uiState`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendSuccess

            // El hardware cambia mientras la UI muestra la pantalla de éxito
            fakeHardwareState.value = MdbState.ReaderEnabled

            // La UI debe seguir mostrando VendSuccess durante el delay
            assertEquals(MdbState.VendSuccess, viewModel.uiState.value)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // VendDenied — delay de display
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `VendDenied aparece en uiState inmediatamente`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendDenied
            assertEquals(MdbState.VendDenied, viewModel.uiState.value)
        }

    @Test fun `uiState revierte al estado hardware después del delay de VendDenied`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendDenied

            advanceTimeBy(VEND_DENIED_DISPLAY_MS + 1)

            fakeHardwareState.value = MdbState.ReaderEnabled
            assertEquals(MdbState.ReaderEnabled, viewModel.uiState.value)
        }

    @Test fun `cambios de hardware durante delay VendDenied no actualizan uiState`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.VendDenied

            fakeHardwareState.value = MdbState.ReaderEnabled

            assertEquals(MdbState.VendDenied, viewModel.uiState.value)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegación a use cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `startSession invoca BeginSessionUseCase`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            viewModel.startSession()
            verify(exactly = 1) { mockBeginSession() }
        }

    @Test fun `approveVend invoca ApproveVendUseCase`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            viewModel.approveVend()
            verify(exactly = 1) { mockApproveVend() }
        }

    @Test fun `denyVend invoca DenyVendUseCase`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            viewModel.denyVend()
            verify(exactly = 1) { mockDenyVend() }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `VendPending incluye precio e ítem correctos`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            val expected = MdbState.VendPending(itemPrice = 250u, itemNumber = 12u)
            fakeHardwareState.value = expected
            val actual = viewModel.uiState.value
            assertTrue(actual is MdbState.VendPending)
            assertEquals(250u, (actual as MdbState.VendPending).itemPrice)
            assertEquals(12u,  actual.itemNumber)
        }

    @Test fun `Error del hardware se propaga a uiState`() =
        runTest(UnconfinedTestDispatcher()) {
            viewModel = buildViewModel()
            fakeHardwareState.value = MdbState.Error("Puerto MDB no disponible")
            val state = viewModel.uiState.value
            assertTrue(state is MdbState.Error)
            assertEquals("Puerto MDB no disponible", (state as MdbState.Error).message)
        }
}
