package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import com.muxunav.telmarktandroid.data.host.protocol.padLeft
import com.muxunav.telmarktandroid.data.host.protocol.padRight

// ── 056 ──────────────────────────────────────────────────────────────────────

data class DataphoneDataRequest(
    val manufacturer: String? = null,               // ≤30
    val appVersion: String? = null,                 // ≤20
    val serialNumber: String? = null,               // ≤24
    val publicKeyVersionOrTerminalId: String? = null, // ≤12
    val drawingNumber: String? = null,              // ≤12
    val deviceCode: String? = null,                 // 2 chars
    val assignedMerchant: String? = null,           // ≤9
    val sdkVersion: String? = null,                 // ≤64
) : Request() {

    override val commandCode = "056"

    init {
        require((manufacturer?.length ?: 0) <= 30) { "manufacturer must be ≤30 chars" }
        require((appVersion?.length ?: 0) <= 20) { "appVersion must be ≤20 chars" }
        require((serialNumber?.length ?: 0) <= 24) { "serialNumber must be ≤24 chars" }
        require((publicKeyVersionOrTerminalId?.length ?: 0) <= 12) { "publicKeyVersionOrTerminalId must be ≤12 chars" }
        require((drawingNumber?.length ?: 0) <= 12) { "drawingNumber must be ≤12 chars" }
        require((deviceCode?.length ?: 0) <= 2) { "deviceCode must be ≤2 chars" }
        require((assignedMerchant?.length ?: 0) <= 9) { "assignedMerchant must be ≤9 chars" }
        require((sdkVersion?.length ?: 0) <= 64) { "sdkVersion must be ≤64 chars" }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        manufacturer?.let { sb.append(it) };                    sb.append(SEPARATOR)
        appVersion?.let { sb.append(it) };                      sb.append(SEPARATOR)
        serialNumber?.let { sb.append(it) };                    sb.append(SEPARATOR)
        publicKeyVersionOrTerminalId?.let { sb.append(it) };    sb.append(SEPARATOR)
        drawingNumber?.let { sb.append(it) };                   sb.append(SEPARATOR)
        deviceCode?.let { sb.append(it) }
        assignedMerchant?.let { sb.append(it) };                sb.append(SEPARATOR)
        sdkVersion?.let { sb.append(it) };                      sb.append(SEPARATOR)
        return sb.toString().toByteArray(ASCII_CHARSET)
    }
}

// ── 057 — ack, no payload fields ─────────────────────────────────────────────

object DataphoneDataResponse : Response() {
    override val commandCode = "057"
}

// ── 074 ──────────────────────────────────────────────────────────────────────

data class DataphonePaymentRequest(val payments: List<PaymentDetail>) : Request() {

    override val commandCode = "074"

    init {
        require(payments.all { it.timestamp.length == 12 }) { "timestamp must be 12 chars" }
        require(payments.all { (it.serviceNumber?.length ?: 0) <= 40 }) { "serviceNumber must be ≤40 chars" }
        require(payments.all { it.price >= 0 }) { "price must be non-negative" }
        require(payments.all { it.paymentType in listOf(6, 9) }) { "paymentType must be 6 or 9" }
        require(payments.all { (it.authCode?.length ?: 0) <= 6 }) { "authCode must be ≤6 chars" }
        require(payments.all { (it.cardNumberFirst6Last4?.length ?: 0) <= 10 }) {
            "cardNumberFirst6Last4 must be ≤10 chars"
        }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(payments.size.padLeft(2))
        payments.forEach { p ->
            sb.append(p.timestamp.padRight(12))
            sb.append((p.serviceNumber ?: "").padRight(40))
            sb.append(SEPARATOR)
            sb.append(p.price.padLeft(8))
            sb.append(p.paymentType)
            sb.append((p.authCode ?: "").padRight(6))
            when (p.paymentType) {
                9 -> {
                    sb.append((p.cardNumberFirst6Last4 ?: "").padRight(10))
                    sb.append((p.approvalCode ?: "").padRight(2))
                    sb.append((p.operationNumber ?: "").padRight(6))
                }
                6 -> {
                    sb.append("".padRight(10))
                    sb.append((p.approvalCode ?: "").padRight(2))
                    sb.append("".padRight(6))
                }
                else -> sb.append("".padRight(18))
            }
            sb.append(SEPARATOR)
        }
        return sb.toString().toByteArray(ASCII_CHARSET)
    }

    data class PaymentDetail(
        val timestamp: String,                      // YYMMDDHHMMSS
        val serviceNumber: String?,                 // ≤40
        val price: Int,                             // non-negative, cents
        val paymentType: Int,                       // 6 (denied) or 9 (approved)
        val authCode: String?,                      // ≤6
        val cardNumberFirst6Last4: String? = null,  // ≤10
        val approvalCode: String? = null,           // ≤2
        val operationNumber: String? = null,        // ≤6
    )
}

// ── 075 ──────────────────────────────────────────────────────────────────────

data class DataphonePaymentResponse(
    val flagsByteCount: Int,
    val updateKioskNeeded: Boolean?,
) : Response() {

    override val commandCode = "075"

    companion object {
        fun deserialize(bytes: ByteArray): DataphonePaymentResponse {
            require(bytes.size >= 2) { "DataphonePaymentResponse payload too short" }
            val s = bytes.toString(ASCII_CHARSET)
            val flagsCount = s.take(2).toIntOrNull() ?: 0
            val updateNeeded = if (flagsCount >= 1 && s.length >= 3) s[2] == '1' else null
            return DataphonePaymentResponse(flagsCount, updateNeeded)
        }
    }
}
