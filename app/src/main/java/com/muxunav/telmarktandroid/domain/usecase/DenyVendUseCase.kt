package com.muxunav.telmarktandroid.domain.usecase

import com.muxunav.telmarktandroid.domain.repository.MdbRepository
import javax.inject.Inject

class DenyVendUseCase @Inject constructor(
    private val repository: MdbRepository
) {
    operator fun invoke() = repository.denyVend()
}
