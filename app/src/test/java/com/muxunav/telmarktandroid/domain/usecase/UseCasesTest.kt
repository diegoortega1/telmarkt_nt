package com.muxunav.telmarktandroid.domain.usecase

import com.muxunav.telmarktandroid.domain.repository.MdbRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class UseCasesTest {

    private lateinit var mockRepository: MdbRepository

    @Before fun setUp() {
        mockRepository = mockk(relaxed = true)
    }

    // ── BeginSessionUseCase ───────────────────────────────────────────────────

    @Test fun `BeginSessionUseCase delega a repository_beginSession`() {
        BeginSessionUseCase(mockRepository).invoke()
        verify(exactly = 1) { mockRepository.beginSession() }
    }

    @Test fun `BeginSessionUseCase no llama a otros métodos del repository`() {
        BeginSessionUseCase(mockRepository).invoke()
        verify(exactly = 0) { mockRepository.approveVend() }
        verify(exactly = 0) { mockRepository.denyVend() }
    }

    // ── ApproveVendUseCase ────────────────────────────────────────────────────

    @Test fun `ApproveVendUseCase delega a repository_approveVend`() {
        ApproveVendUseCase(mockRepository).invoke()
        verify(exactly = 1) { mockRepository.approveVend() }
    }

    @Test fun `ApproveVendUseCase no llama a otros métodos del repository`() {
        ApproveVendUseCase(mockRepository).invoke()
        verify(exactly = 0) { mockRepository.beginSession() }
        verify(exactly = 0) { mockRepository.denyVend() }
    }

    // ── DenyVendUseCase ───────────────────────────────────────────────────────

    @Test fun `DenyVendUseCase delega a repository_denyVend`() {
        DenyVendUseCase(mockRepository).invoke()
        verify(exactly = 1) { mockRepository.denyVend() }
    }

    @Test fun `DenyVendUseCase no llama a otros métodos del repository`() {
        DenyVendUseCase(mockRepository).invoke()
        verify(exactly = 0) { mockRepository.beginSession() }
        verify(exactly = 0) { mockRepository.approveVend() }
    }
}
