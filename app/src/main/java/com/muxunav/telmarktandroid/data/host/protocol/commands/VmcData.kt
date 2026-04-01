package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.padLeft
import com.muxunav.telmarktandroid.data.host.protocol.padRight

// ── 050 ──────────────────────────────────────────────────────────────────────

data class VmcDataRequest(
    val manufacturer: String,    // ≤3
    val serialNumber: String,    // ≤12
    val model: String,           // ≤12
    val firmwareVersion: String, // ≤5
    val featureLevel: Char,      // digit '0'-'9'
    val displayColumns: Int,     // 0-99
    val displayRows: Int,        // 0-99
    val displayType: Int,        // 0-99
    val maxPrice: Int,           // 0-99999
    val minPrice: Int,           // 0-99999
) : Request() {

    override val commandCode = "050"

    init {
        require(manufacturer.length <= 3) { "manufacturer must be ≤3 chars" }
        require(serialNumber.length <= 12) { "serialNumber must be ≤12 chars" }
        require(model.length <= 12) { "model must be ≤12 chars" }
        require(firmwareVersion.length <= 5) { "firmwareVersion must be ≤5 chars" }
        require(featureLevel.isDigit()) { "featureLevel must be a digit" }
        require(displayColumns in 0..99) { "displayColumns must be 0-99" }
        require(displayRows in 0..99) { "displayRows must be 0-99" }
        require(displayType in 0..99) { "displayType must be 0-99" }
        require(maxPrice in 0..99999) { "maxPrice must be 0-99999" }
        require(minPrice in 0..99999) { "minPrice must be 0-99999" }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(manufacturer.padRight(3))
        sb.append(serialNumber.padLeft(12, ' '))
        sb.append(model.padLeft(12, ' '))
        sb.append(firmwareVersion.padLeft(5, ' '))
        sb.append(featureLevel)
        sb.append(displayColumns.padLeft(2))
        sb.append(displayRows.padLeft(2))
        sb.append(displayType.padLeft(2))
        sb.append(maxPrice.padLeft(5))
        sb.append(minPrice.padLeft(5))
        return sb.toString().toByteArray(ASCII_CHARSET)
    }
}

// ── 051 — ack, no payload fields ─────────────────────────────────────────────

object VmcDataResponse : Response() {
    override val commandCode = "051"
}
