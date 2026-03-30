package com.example.telmarktnt.domain.usecase

import com.example.telmarktnt.domain.repository.MdbRepository
import javax.inject.Inject

class DenyVendUseCase @Inject constructor(
    private val repository: MdbRepository
) {
    operator fun invoke() = repository.denyVend()
}
