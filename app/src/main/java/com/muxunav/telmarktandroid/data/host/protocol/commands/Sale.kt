package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import com.muxunav.telmarktandroid.data.host.protocol.TIMESTAMP_REGEX
import com.muxunav.telmarktandroid.data.host.protocol.padLeft
import com.muxunav.telmarktandroid.data.host.protocol.padRight

// ── 070 ──────────────────────────────────────────────────────────────────────

data class SaleRequest(val sales: List<SaleDetail>) : Request() {

    override val commandCode = "070"

    init {
        require(sales.size <= 99) { "SaleRequest can hold at most 99 sales" }
        sales.forEach { s ->
            require(s.timestamp.length == 12 && s.timestamp.matches(TIMESTAMP_REGEX)) {
                "Invalid sale timestamp: ${s.timestamp}"
            }
            require(s.productCode.length <= 5) { "productCode must be ≤5 chars" }
            require(s.price in 0..99999999) { "price must be 0-99999999" }
            require(s.saleType in 0..9) { "saleType must be 0-9" }
            require(s.dniId == null || s.dniId.length == 7) { "dniId must be 7 chars" }
            require(s.authCode == null || s.authCode.length <= 6) { "authCode must be ≤6 chars" }
            if (s.saleType in listOf(5, 7, 8, 9)) {
                require(s.cardNumberFirst6Last4 != null) { "cardNumberFirst6Last4 required for type ${s.saleType}" }
                require(s.approvalCode != null) { "approvalCode required for type ${s.saleType}" }
                require(s.operationNumber != null) { "operationNumber required for type ${s.saleType}" }
            }
            if (s.saleType == 6) {
                require(s.approvalCode != null) { "approvalCode required for type 6" }
            }
            require(s.cardNumberFirst6Last4 == null || s.cardNumberFirst6Last4.length <= 10) {
                "cardNumberFirst6Last4 must be ≤10 chars"
            }
            require(s.approvalCode == null || s.approvalCode.length <= 2) {
                "approvalCode must be ≤2 chars"
            }
            require(s.operationNumber == null || s.operationNumber.length <= 6) {
                "operationNumber must be ≤6 chars"
            }
        }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(sales.size.padLeft(2))
        sales.forEach { s ->
            sb.append(s.timestamp.padRight(12))
            sb.append(s.productCode.padRight(5))
            sb.append(s.price.padLeft(8))
            sb.append(s.saleType)
            sb.append((s.dniId ?: "0").padRight(7))
            sb.append(s.authCode?.padRight(6) ?: "")
            when (s.saleType) {
                5, 7, 8, 9 -> {
                    sb.append((s.cardNumberFirst6Last4 ?: "").padRight(10))
                    sb.append((s.approvalCode ?: "").padRight(2))
                    sb.append((s.operationNumber ?: "").padRight(6))
                }
                6 -> {
                    sb.append("".padRight(10))
                    sb.append((s.approvalCode ?: "").padRight(2))
                    sb.append("".padRight(6))
                }
                // types 0-4: no extra fields
            }
            sb.append(SEPARATOR)
        }
        return sb.toString().toByteArray(ASCII_CHARSET)
    }

    data class SaleDetail(
        val timestamp: String,              // YYMMDDHHMMSS
        val productCode: String,            // ≤5
        val price: Int,                     // cents, no decimal point
        val saleType: Int,                  // 0-9 — see SaleType enum
        val dniId: String?,                 // 7 chars; null→ "0" on wire
        val authCode: String?,              // ≤6; required for types 1,3-5,7-9
        val cardNumberFirst6Last4: String? = null, // ≤10; types 5,7-9
        val approvalCode: String? = null,   // ≤2; types 5-9
        val operationNumber: String? = null, // ≤6; types 5,7-9
    )
}

// ── 071 ──────────────────────────────────────────────────────────────────────

data class SaleResponse(
    val flagsByteCount: Int,
    val updateRa2l1Needed: Boolean?,
    val currentTimestamp: String,
) : Response() {

    override val commandCode = "071"

    companion object {
        fun deserialize(bytes: ByteArray): SaleResponse {
            require(bytes.size >= 14) { "SaleResponse payload too short" }
            val s = bytes.toString(ASCII_CHARSET)
            val flagsCount = s.take(2).toIntOrNull() ?: 0
            var idx = 2
            val updateNeeded = if (flagsCount >= 1) (s[idx++] == '1') else null
            val timestamp = s.substring(idx, idx + 12)
            require(timestamp.matches(TIMESTAMP_REGEX)) { "Invalid timestamp in SaleResponse" }
            return SaleResponse(flagsCount, updateNeeded, timestamp)
        }
    }
}

// ── Domain enums ─────────────────────────────────────────────────────────────

enum class SaleType(val value: Int) {
    CASH(0),
    WALLET_OR_PREPAID(3),
    CARD_APPROVED(5),
    CARD_DENIED(6),
    REFUND(7),
    LOST_SALE(8),
    ACCEPTED_NOT_CONFIRMED(9),
}

/** Denial reason codes, sent as approvalCode in SaleDetail for denied/failed sales. */
enum class ApprovalCode(val value: Int) {
    ERRONEOUS_CARD(2),
    CARD_NOT_VALID(5),
    TRANSACTION_DENIED_OFFLINE(6),
    TRANSACTION_NOT_FINALISED_ONLINE(7),
    TRANSACTION_CANCELLED_OR_TIMEOUT(11),
    TERMINAL_NOT_INITIALIZED(13),
    POS_RESET_DURING_PAYMENT(16),
    NO_RESPONSE_NO_PROCESSOR_CONNECTION(17),
    NO_RESPONSE_AFTER_PROCESSOR_CONNECTION(18),
    TRANSACTION_CANCELLED_WITH_CONNECTION(19),
    APPROVED(90),
}
