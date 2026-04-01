package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.isAscii
import com.muxunav.telmarktandroid.data.host.protocol.padLeft
import com.muxunav.telmarktandroid.data.host.protocol.padRight

// ── 054 ──────────────────────────────────────────────────────────────────────

data class Cashless1DataRequest(
    val manufacturer: String,       // ≤3
    val serialNumber: String,       // ≤12
    val model: String,              // ≤12
    val firmwareVersion: String,    // ≤5
    val featureLevel: Char,         // ASCII
    val countryCode: String,        // ≤5
    val scaleFactor: Int,           // 0-99
    val decimals: Int,              // 0-99
    val maxResponseTime: Int,       // 0-999 seconds
    val supportsRefund: Boolean,
    val supportsMultisale: Boolean,
    val hasDisplay: Boolean,
    val acceptsCashSaleInfo: Boolean,
) : Request() {

    override val commandCode = "054"

    init {
        require(manufacturer.length <= 3) { "manufacturer must be ≤3 chars" }
        require(serialNumber.length <= 12) { "serialNumber must be ≤12 chars" }
        require(model.length <= 12) { "model must be ≤12 chars" }
        require(firmwareVersion.length <= 5) { "firmwareVersion must be ≤5 chars" }
        require(isAscii(featureLevel)) { "featureLevel must be ASCII" }
        require(countryCode.length <= 5) { "countryCode must be ≤5 chars" }
        require(scaleFactor in 0..99) { "scaleFactor must be 0-99" }
        require(decimals in 0..99) { "decimals must be 0-99" }
        require(maxResponseTime in 0..999) { "maxResponseTime must be 0-999" }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(manufacturer.padRight(3))
        sb.append(serialNumber.padRight(12))
        sb.append(model.padRight(12))
        sb.append(firmwareVersion.padRight(5))
        sb.append(featureLevel)
        sb.append(countryCode.padRight(5))
        sb.append(scaleFactor.padLeft(2))
        sb.append(decimals.padLeft(2))
        sb.append(maxResponseTime.padLeft(3))
        sb.append(if (supportsRefund) '1' else '0')
        sb.append(if (supportsMultisale) '1' else '0')
        sb.append(if (hasDisplay) '1' else '0')
        sb.append(if (acceptsCashSaleInfo) '1' else '0')
        return sb.toString().toByteArray(ASCII_CHARSET)
    }
}

// ── 055 — ack, no payload fields ─────────────────────────────────────────────

object Cashless1DataResponse : Response() {
    override val commandCode = "055"
}
