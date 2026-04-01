package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR

// ── 052 ──────────────────────────────────────────────────────────────────────

data class HardwareDataRequest(
    val piModel: String? = null,              // ≤50
    val piHardware: String? = null,           // ≤50
    val piRevision: String? = null,           // ≤50
    val osVersion: String? = null,            // ≤50
    val modemManufacturer: String? = null,    // ≤20
    val modemModel: String? = null,           // ≤20
    val modemImei: String? = null,            // ≤20
    val modemFirmware: String? = null,        // ≤20
    val connectionType: ConnectionType? = null,
    val renesasPartNumber: String? = null,    // ≤16
    val renesasHardwareVersion: String? = null, // ≤3
    val rtcStatus: Char? = null,              // '0','1' or '4'-'9'
    val sdCapacityKB: Long? = null,           // ≤10 digits
    val sdFreeKB: Long? = null,               // ≤10 digits
) : Request() {

    override val commandCode = "052"

    init {
        require((piModel?.length ?: 0) <= 50) { "piModel must be ≤50 chars" }
        require((piHardware?.length ?: 0) <= 50) { "piHardware must be ≤50 chars" }
        require((piRevision?.length ?: 0) <= 50) { "piRevision must be ≤50 chars" }
        require((osVersion?.length ?: 0) <= 50) { "osVersion must be ≤50 chars" }
        require((modemManufacturer?.length ?: 0) <= 20) { "modemManufacturer must be ≤20 chars" }
        require((modemModel?.length ?: 0) <= 20) { "modemModel must be ≤20 chars" }
        require((modemImei?.length ?: 0) <= 20) { "modemImei must be ≤20 chars" }
        require((modemFirmware?.length ?: 0) <= 20) { "modemFirmware must be ≤20 chars" }
        require((renesasPartNumber?.length ?: 0) <= 16) { "renesasPartNumber must be ≤16 chars" }
        require((renesasHardwareVersion?.length ?: 0) <= 3) { "renesasHardwareVersion must be ≤3 chars" }
        require(rtcStatus?.toString()?.matches(Regex("[014-9]")) ?: true) {
            "rtcStatus must be '0', '1', or '4'-'9'"
        }
        require((sdCapacityKB?.toString()?.length ?: 0) <= 10) { "sdCapacityKB must be ≤10 digits" }
        require((sdFreeKB?.toString()?.length ?: 0) <= 10) { "sdFreeKB must be ≤10 digits" }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        piModel?.let { sb.append(it) };           sb.append(SEPARATOR)
        piHardware?.let { sb.append(it) };        sb.append(SEPARATOR)
        piRevision?.let { sb.append(it) };        sb.append(SEPARATOR)
        osVersion?.let { sb.append(it) };         sb.append(SEPARATOR)
        modemManufacturer?.let { sb.append(it) }; sb.append(SEPARATOR)
        modemModel?.let { sb.append(it) };        sb.append(SEPARATOR)
        modemImei?.let { sb.append(it) };         sb.append(SEPARATOR)
        modemFirmware?.let { sb.append(it) };     sb.append(SEPARATOR)
        connectionType?.let { sb.append(it.code) }
        renesasPartNumber?.let { sb.append(it) }; sb.append(SEPARATOR)
        renesasHardwareVersion?.let { sb.append(it) }; sb.append(SEPARATOR)
        rtcStatus?.let { sb.append(it) };         sb.append(SEPARATOR)
        sdCapacityKB?.let { sb.append(it) };      sb.append(SEPARATOR)
        sdFreeKB?.let { sb.append(it) };          sb.append(SEPARATOR)
        return sb.toString().toByteArray(ASCII_CHARSET)
    }
}

// ── 053 — ack, no payload fields ─────────────────────────────────────────────

object HardwareDataResponse : Response() {
    override val commandCode = "053"
}

// ── Shared enum ───────────────────────────────────────────────────────────────

enum class ConnectionType(val code: Char) {
    MODEM_UART('0'), MODEM_USB('1'), ETHERNET('2'), WIFI('3')
}
