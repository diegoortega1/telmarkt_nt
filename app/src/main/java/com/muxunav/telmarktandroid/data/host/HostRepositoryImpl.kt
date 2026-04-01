package com.muxunav.telmarktandroid.data.host

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.commands.ServerInfoRequest
import com.muxunav.telmarktandroid.data.host.protocol.commands.ServerInfoResponse
import com.muxunav.telmarktandroid.domain.model.AppConfig
import com.muxunav.telmarktandroid.domain.repository.HostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── Conexión ──────────────────────────────────────────────────────────────────

private const val HOST = "3.74.37.246"
private const val PORT = 2001
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS    = 15_000

// ── Protocolo ─────────────────────────────────────────────────────────────────

private const val PROTOCOL_VERSION = "101"   // versión del protocolo (1.01)
private const val CODE_VERSION      = "0001"  // versión de la app cliente
private const val LENGTH_SIZE       = 4
private const val HEADER_SIZE       = 36
private const val COMMAND_CODE_SIZE = 3
private const val CHECKSUM_SIZE     = 2

class HostRepositoryImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
) : HostRepository {

    /**
     * Envía ServerInfoRequest (010), recibe ServerInfoResponse (011),
     * mapea al modelo de dominio y lo devuelve.
     * Lanza IOException si la conexión o el framing falla.
     */
    override suspend fun syncServerInfo(): AppConfig = withContext(Dispatchers.IO) {
        val request = ServerInfoRequest(
            icc         = deviceInfo.icc,
            imei        = deviceInfo.imei,
            signalLevel = deviceInfo.signalLevel,
            specialInfo = ServerInfoRequest.SpecialInfo.START_UP,
            ipAddress   = deviceInfo.ipAddress,
        )
        val payload = exchange(
            requestCode          = request.commandCode,
            requestPayload       = request.serialize(),
            expectedResponseCode = "011",
        )
        ServerInfoResponse.deserialize(payload).toDomain()
    }

    // ── Transporte ────────────────────────────────────────────────────────────

    private fun exchange(
        requestCode: String,
        requestPayload: ByteArray,
        expectedResponseCode: String,
    ): ByteArray {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS

            val frame = buildFrame(requestCode, requestPayload)
            socket.getOutputStream().apply { write(frame); flush() }

            return readResponsePayload(socket.getInputStream(), expectedResponseCode)
        }
    }

    // ── Construcción de trama ─────────────────────────────────────────────────
    //
    // Estructura:
    //   [LENGTH 4 bytes ASCII] [HEADER 36 bytes] [COMMAND 3 bytes] [PAYLOAD N bytes] [CK 2 bytes hex]

    private fun buildFrame(commandCode: String, payload: ByteArray): ByteArray {
        val header  = buildHeader()
        val command = commandCode.toByteArray(ASCII_CHARSET)
        val body    = header + command + payload

        val totalLength = LENGTH_SIZE + body.size + CHECKSUM_SIZE
        val lengthBytes = totalLength.toString()
            .padStart(LENGTH_SIZE, '0')
            .toByteArray(ASCII_CHARSET)

        val frameWithoutCk = lengthBytes + body
        val ck = computeChecksum(frameWithoutCk).toHex2().toByteArray(ASCII_CHARSET)

        return frameWithoutCk + ck
    }

    private fun buildHeader(): ByteArray {
        // 3 + 12 + 4 + 16 + 1 = 36 bytes
        val sb = StringBuilder()
        sb.append(PROTOCOL_VERSION)
        sb.append(currentTimestamp())
        sb.append(CODE_VERSION)
        sb.append(deviceInfo.serialNumber.padStart(16, ' '))
        sb.append('1')   // estado MDB: inactivo
        return sb.toString().toByteArray(ASCII_CHARSET)
    }

    // ── Lectura de trama ──────────────────────────────────────────────────────

    private fun readResponsePayload(input: InputStream, expectedCode: String): ByteArray {
        // Leer los 4 bytes de longitud
        val lengthBytes  = input.readExact(LENGTH_SIZE)
        val totalLength  = String(lengthBytes, ASCII_CHARSET).trim().toInt()
        val remaining    = input.readExact(totalLength - LENGTH_SIZE)

        // Layout del resto: HEADER(36) + COMMAND(3) + DATA(N) + CK(2)
        val ckStart   = remaining.size - CHECKSUM_SIZE
        val dataStart = HEADER_SIZE + COMMAND_CODE_SIZE
        if (dataStart > ckStart) throw IOException("Trama demasiado corta: ${remaining.size} bytes")

        // Validar checksum
        val frameWithoutCk = lengthBytes + remaining.sliceArray(0 until ckStart)
        val receivedCk     = String(remaining.sliceArray(ckStart until remaining.size), ASCII_CHARSET)
        val expectedCk     = computeChecksum(frameWithoutCk).toHex2()
        if (receivedCk != expectedCk) {
            throw IOException("Checksum incorrecto: esperado $expectedCk, recibido $receivedCk")
        }

        // Validar código de respuesta
        val responseCode = String(remaining.sliceArray(HEADER_SIZE until dataStart), ASCII_CHARSET)
        if (responseCode != expectedCode) {
            throw IOException("Código de respuesta inesperado: esperado $expectedCode, recibido $responseCode")
        }

        return remaining.sliceArray(dataStart until ckStart)
    }

    // ── Checksum ──────────────────────────────────────────────────────────────
    //
    // XOR de (byte[i] * (i+1)) para todos los bytes del frame sin checksum.
    // Resultado: 1 byte (LSB), representado como 2 dígitos hex en mayúsculas.

    private fun computeChecksum(bytes: ByteArray): Int {
        var result = 0
        for (i in bytes.indices) {
            result = result xor ((bytes[i].toInt() and 0xFF) * (i + 1))
        }
        return result and 0xFF
    }

    private fun Int.toHex2(): String = toString(16).uppercase().padStart(2, '0')

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentTimestamp(): String =
        SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())

    private fun InputStream.readExact(n: Int): ByteArray {
        val buf = ByteArray(n)
        var offset = 0
        while (offset < n) {
            val read = read(buf, offset, n - offset)
            if (read == -1) throw IOException("Conexión cerrada tras $offset/$n bytes")
            offset += read
        }
        return buf
    }
}

// ── Mappers ServerInfoResponse → AppConfig ───────────────────────────────────

private fun ServerInfoResponse.toDomain() = AppConfig(
    paymentMode          = paymentMode.toDomain(),
    ageControlMethod     = ageControlMethod.toDomain(),
    nextCommMinutes      = nextCommMinutes,
    mdbInUse             = mdbInUse,
    useMdbLevel3         = useMdbLevel3,
    maxMobileCredit      = maxMobileCredit,
    rebootTime           = rebootTime,
    updateProductsNeeded = updateProductsNeeded,
    updatePulsesNeeded   = updatePulsesNeeded,
    updateLcdsNeeded     = updateLcdsNeeded,
    lastSyncAt           = System.currentTimeMillis(),
)

private fun ServerInfoResponse.PaymentMode.toDomain() = when (this) {
    ServerInfoResponse.PaymentMode.NO_PAYMENT           -> AppConfig.PaymentMode.NO_PAYMENT
    ServerInfoResponse.PaymentMode.MDB_NORMAL           -> AppConfig.PaymentMode.MDB_NORMAL
    ServerInfoResponse.PaymentMode.PAYMENT_PROTOCOL     -> AppConfig.PaymentMode.PAYMENT_PROTOCOL
    ServerInfoResponse.PaymentMode.PULSES_SINGLE_PRICE  -> AppConfig.PaymentMode.PULSES_SINGLE_PRICE
    ServerInfoResponse.PaymentMode.MDB_WITH_AGE_CONTROL -> AppConfig.PaymentMode.MDB_WITH_AGE_CONTROL
    ServerInfoResponse.PaymentMode.PULSES_MULTI_PRICE   -> AppConfig.PaymentMode.PULSES_MULTI_PRICE
}

private fun ServerInfoResponse.AgeControlMethod.toDomain() = when (this) {
    ServerInfoResponse.AgeControlMethod.NONE              -> AppConfig.AgeControlMethod.NONE
    ServerInfoResponse.AgeControlMethod.AGE_VALIDATOR_APP -> AppConfig.AgeControlMethod.AGE_VALIDATOR_APP
}
