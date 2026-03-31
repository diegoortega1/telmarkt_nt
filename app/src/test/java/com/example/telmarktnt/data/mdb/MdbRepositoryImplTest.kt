package com.example.telmarktnt.data.mdb

import app.cash.turbine.test
import com.example.telmarktnt.domain.model.MdbState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MdbRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // Scope separado del TestScope para que el collectJob no sea hijo del runTest
    private val repoScope = CoroutineScope(testDispatcher + SupervisorJob())

    private lateinit var repository: MdbRepositoryImpl
    private lateinit var fakeServiceState: MutableStateFlow<MdbState>
    private lateinit var mockService: MdbService

    @Before fun setUp() {
        fakeServiceState = MutableStateFlow(MdbState.Idle)
        mockService = mockk(relaxed = true) {
            every { state } returns fakeServiceState
        }
        repository = MdbRepositoryImpl(repoScope)
    }

    @After fun tearDown() {
        repoScope.cancel()
    }

    @Test fun `estado inicial es Idle`() {
        assertEquals(MdbState.Idle, repository.state.value)
    }

    @Test fun `onServiceConnected empieza a recolectar estado del service`() = runTest(testDispatcher) {
        repository.onServiceConnected(mockService)
        fakeServiceState.value = MdbState.ReaderEnabled
        assertEquals(MdbState.ReaderEnabled, repository.state.value)
    }

    @Test fun `los cambios de estado del service se propagan al repositorio`() = runTest(testDispatcher) {
        repository.onServiceConnected(mockService)
        repository.state.test {
            assertEquals(MdbState.Idle, awaitItem())

            fakeServiceState.value = MdbState.ReaderEnabled
            assertEquals(MdbState.ReaderEnabled, awaitItem())

            fakeServiceState.value = MdbState.SessionActive
            assertEquals(MdbState.SessionActive, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `onServiceDisconnected cancela la recolección y resetea a Idle`() = runTest(testDispatcher) {
        repository.onServiceConnected(mockService)
        fakeServiceState.value = MdbState.ReaderEnabled

        repository.onServiceDisconnected()

        assertEquals(MdbState.Idle, repository.state.value)
    }

    @Test fun `onServiceDisconnected evita que cambios posteriores del service afecten el estado`() = runTest(testDispatcher) {
        repository.onServiceConnected(mockService)
        repository.onServiceDisconnected()

        fakeServiceState.value = MdbState.ReaderEnabled

        assertEquals(MdbState.Idle, repository.state.value)
    }

    @Test fun `beginSession delega al service cuando está conectado`() {
        repository.onServiceConnected(mockService)
        repository.beginSession()
        verify(exactly = 1) { mockService.beginSession() }
    }

    @Test fun `beginSession es no-op cuando no hay service`() {
        repository.beginSession()
    }

    @Test fun `approveVend delega al service cuando está conectado`() {
        repository.onServiceConnected(mockService)
        repository.approveVend()
        verify(exactly = 1) { mockService.approveVend() }
    }

    @Test fun `approveVend es no-op cuando no hay service`() {
        repository.approveVend()
    }

    @Test fun `denyVend delega al service cuando está conectado`() {
        repository.onServiceConnected(mockService)
        repository.denyVend()
        verify(exactly = 1) { mockService.denyVend() }
    }

    @Test fun `denyVend es no-op cuando no hay service`() {
        repository.denyVend()
    }

    @Test fun `reconectar service reemplaza la suscripción anterior`() = runTest(testDispatcher) {
        val secondServiceState = MutableStateFlow<MdbState>(MdbState.Idle)
        val secondService: MdbService = mockk(relaxed = true) {
            every { state } returns secondServiceState
        }

        repository.onServiceConnected(mockService)
        repository.onServiceConnected(secondService)

        secondServiceState.value = MdbState.ReaderEnabled

        assertEquals(MdbState.ReaderEnabled, repository.state.value)
    }
}
