package com.muxunav.telmarktandroid.data.host

import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.repository.HostRepository
import java.io.IOException
import javax.inject.Inject

/**
 * TODO: implementar conexión TCP real con el servidor.
 *
 * El protocolo es binario sobre TCP. El flujo esperado:
 *   1. Abrir socket a la IP/puerto configurada del servidor
 *   2. Serializar ServerInfoRequest con los datos del dispositivo (ICC, IMEI, IP, señal)
 *   3. Envolver en el frame del protocolo (cabecera + commandCode + payload)
 *   4. Enviar y leer la respuesta
 *   5. Parsear ServerInfoResponse.deserialize(bytes)
 *   6. Mapear ServerInfoResponse → AppConfig (ver toDomain() abajo)
 *
 * Mientras no esté implementado, lanza IOException para que AppStartupUseCase
 * caiga en el path de fallback (última config guardada o DEFAULT).
 */
class HostRepositoryImpl @Inject constructor() : HostRepository {

    override suspend fun syncServerInfo(): AppConfig {
        throw IOException("Conexión TCP con servidor no implementada todavía")
    }
}

// ── Mapper ServerInfoResponse → AppConfig ────────────────────────────────────
// Se activará cuando el TCP client esté listo. Lo dejamos aquí como referencia.
//
// private fun ServerInfoResponse.toDomain() = AppConfig(
//     paymentMode      = paymentMode.toDomain(),
//     ageControlMethod = ageControlMethod.toDomain(),
//     nextCommMinutes  = nextCommMinutes,
//     mdbInUse         = mdbInUse,
//     useMdbLevel3     = useMdbLevel3,
//     maxMobileCredit  = maxMobileCredit,
//     rebootTime       = rebootTime,
//     updateProductsNeeded = updateProductsNeeded,
//     updatePulsesNeeded   = updatePulsesNeeded,
//     updateLcdsNeeded     = updateLcdsNeeded,
//     lastSyncAt       = System.currentTimeMillis(),
// )
//
// private fun ServerInfoResponse.PaymentMode.toDomain() = when (this) {
//     ServerInfoResponse.PaymentMode.NO_PAYMENT          -> AppConfig.PaymentMode.NO_PAYMENT
//     ServerInfoResponse.PaymentMode.MDB_NORMAL          -> AppConfig.PaymentMode.MDB_NORMAL
//     ServerInfoResponse.PaymentMode.PAYMENT_PROTOCOL    -> AppConfig.PaymentMode.PAYMENT_PROTOCOL
//     ServerInfoResponse.PaymentMode.PULSES_SINGLE_PRICE -> AppConfig.PaymentMode.PULSES_SINGLE_PRICE
//     ServerInfoResponse.PaymentMode.MDB_WITH_AGE_CONTROL-> AppConfig.PaymentMode.MDB_WITH_AGE_CONTROL
//     ServerInfoResponse.PaymentMode.PULSES_MULTI_PRICE  -> AppConfig.PaymentMode.PULSES_MULTI_PRICE
// }
//
// private fun ServerInfoResponse.AgeControlMethod.toDomain() = when (this) {
//     ServerInfoResponse.AgeControlMethod.NONE              -> AppConfig.AgeControlMethod.NONE
//     ServerInfoResponse.AgeControlMethod.AGE_VALIDATOR_APP -> AppConfig.AgeControlMethod.AGE_VALIDATOR_APP
// }
