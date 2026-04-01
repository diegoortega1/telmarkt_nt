package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.DATE_REGEX
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import com.muxunav.telmarktandroid.data.host.protocol.TIME_REGEX
import com.muxunav.telmarktandroid.data.host.protocol.TIMESTAMP_REGEX
import com.muxunav.telmarktandroid.data.host.protocol.padLeft
import com.muxunav.telmarktandroid.data.host.protocol.part

// ── 010 ──────────────────────────────────────────────────────────────────────

data class ServerInfoRequest(
    val icc: String,           // ≤20 hex chars
    val imei: String,          // ≤15 hex chars
    val signalLevel: Int,      // 0-31 or 99 (no signal)
    val specialInfo: SpecialInfo,
    val ipAddress: String,     // 7-15 chars
    val frequencyBand: String? = null,
    val operator: String? = null,
    val renesasFirmware: String? = null,
) : Request() {

    override val commandCode = "010"

    init {
        require(icc.length <= 20) { "icc must be ≤20 chars" }
        require(imei.length <= 15) { "imei must be ≤15 chars" }
        require(ipAddress.length in 7..15) { "ipAddress must be 7-15 chars" }
        require(frequencyBand == null || frequencyBand.length <= 50) { "frequencyBand must be ≤50 chars" }
        require(operator == null || operator.length <= 50) { "operator must be ≤50 chars" }
        require(renesasFirmware == null || renesasFirmware.length <= 4) { "renesasFirmware must be ≤4 chars" }
        require(signalLevel == 99 || signalLevel in 0..31) { "signalLevel must be 0-31 or 99" }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(icc.padLeft(20))
        sb.append(imei.padLeft(15))
        sb.append(signalLevel.padLeft(2))
        sb.append(('0' + specialInfo.byteValue.toInt()).toChar())
        sb.append(ipAddress)
        sb.append(SEPARATOR)
        frequencyBand?.let { sb.append(it) }
        sb.append(SEPARATOR)
        operator?.let { sb.append(it) }
        sb.append(SEPARATOR)
        renesasFirmware?.let { sb.append(it) }
        sb.append(SEPARATOR)
        return sb.toString().toByteArray(ASCII_CHARSET)
    }

    enum class SpecialInfo(val byteValue: Byte) {
        NORMAL(0), START_UP(1), READER_INIT(2), READER_RESET(3)
    }
}

// ── 011 ──────────────────────────────────────────────────────────────────────

data class ServerInfoResponse(
    val timestamp: String,              // YYMMDDHHMMSS
    val nextCommMinutes: Int,
    val rebootTime: String?,            // HH:MM:SS or null
    val telemetryInUse: Boolean,
    val telemetryMachineInitialized: Boolean,
    val telemetryReadDelayMinutes: Int,
    val telemetryBaudRateCode: String?,
    val telemetryOperatorCode: String?,
    val telemetryProtocol: String?,
    val tvInUse: Boolean,
    val mdbInUse: Boolean,
    val updateLcdsNeeded: Boolean,
    val updateGcisNeeded: Boolean,
    val updateTvNeeded: Boolean,
    val updateProductsNeeded: Boolean,
    val updateLuminosityNeeded: Boolean,
    val updateTftFirmwareNeeded: Boolean,
    val updateFirmwareNeeded: Boolean,
    val maxMobileCredit: Int,
    val paymentMode: PaymentMode,
    val ageControlMethod: AgeControlMethod,
    val dataphoneAction: DataphoneAction,
    val updatePulsesNeeded: Boolean,
    val useMdbLevel3: Boolean,
    val timeZone: String?,
) : Response() {

    override val commandCode = "011"

    /** Empty/default instance used before the first server sync. */
    constructor() : this(
        timestamp = "", nextCommMinutes = 0, rebootTime = null,
        telemetryInUse = false, telemetryMachineInitialized = false,
        telemetryReadDelayMinutes = 0, telemetryBaudRateCode = null,
        telemetryOperatorCode = null, telemetryProtocol = null,
        tvInUse = false, mdbInUse = false,
        updateLcdsNeeded = false, updateGcisNeeded = false,
        updateTvNeeded = false, updateProductsNeeded = false,
        updateLuminosityNeeded = false, updateTftFirmwareNeeded = false,
        updateFirmwareNeeded = false, maxMobileCredit = 0,
        paymentMode = PaymentMode.NO_PAYMENT,
        ageControlMethod = AgeControlMethod.NONE,
        dataphoneAction = DataphoneAction.NONE,
        updatePulsesNeeded = false, useMdbLevel3 = false, timeZone = null,
    )

    companion object {
        fun deserialize(bytes: ByteArray): ServerInfoResponse {
            val s = bytes.toString(ASCII_CHARSET)
            val parts = s.split(SEPARATOR)

            val timestamp = parts[0].take(12)
            require(timestamp.matches(TIMESTAMP_REGEX)) { "Invalid timestamp: $timestamp" }
            val nextCommMinutes = parts[0].substring(12, 16).toIntOrNull() ?: 0
            val rebootTime = parts[0].substring(16).trim().ifEmpty { null }
            rebootTime?.let { require(it.matches(TIME_REGEX)) { "Invalid rebootTime: $it" } }

            // Part 1: telemetry block
            var partIdx = 1
            val telFlag = parts.part(partIdx)?.firstOrNull()
            val telemetryInUse = telFlag == '1' || telFlag == '2'

            var telInit = false; var telDelay = 0
            var telBaud: String? = null; var telOp: String? = null; var telProto: String? = null

            if (telemetryInUse) {
                val tel = parts.part(partIdx) ?: ""
                telInit = tel.getOrNull(1) == '1'
                if (tel.length >= 6) telDelay = tel.substring(2, 6).toInt()
                if (tel.length >= 12) telBaud = tel.substring(6, 12).trim().ifEmpty { null }
                if (tel.length >= 18) telOp = tel.substring(12, 18).trim().ifEmpty { null }
                if (tel.length >= 19) telProto = tel.substring(18).trim().ifEmpty { null }
            }
            partIdx++

            // Part 2: flags + config
            val p = parts.part(partIdx)
            var i = 0
            fun nextChar() = p?.get(i++) ?: '0'
            fun nextBool() = nextChar() == '1'

            val tvInUse = nextBool()
            val mdbInUse = nextBool()
            val updateLcds = nextBool()
            val updateGcis = nextBool()
            val updateTv = nextBool()
            val updateProducts = nextBool()
            val updateLuminosity = nextBool()
            val updateTftFw = nextBool()
            val updateFw = nextBool()
            val maxCredit = p?.substring(i, i + 5)?.trim()?.toIntOrNull() ?: 0
            i += 5
            val payMode = PaymentMode.fromChar(nextChar())
            val ageMethod = AgeControlMethod.fromChar(nextChar())
            val dfAction = DataphoneAction.fromChar(nextChar())
            val updatePulses = nextBool()
            val mdbLevel3 = nextBool()
            partIdx++

            val timeZone = parts.part(partIdx)?.trim()?.ifEmpty { null }

            return ServerInfoResponse(
                timestamp = timestamp, nextCommMinutes = nextCommMinutes,
                rebootTime = rebootTime, telemetryInUse = telemetryInUse,
                telemetryMachineInitialized = telInit, telemetryReadDelayMinutes = telDelay,
                telemetryBaudRateCode = telBaud, telemetryOperatorCode = telOp,
                telemetryProtocol = telProto, tvInUse = tvInUse, mdbInUse = mdbInUse,
                updateLcdsNeeded = updateLcds, updateGcisNeeded = updateGcis,
                updateTvNeeded = updateTv, updateProductsNeeded = updateProducts,
                updateLuminosityNeeded = updateLuminosity, updateTftFirmwareNeeded = updateTftFw,
                updateFirmwareNeeded = updateFw, maxMobileCredit = maxCredit,
                paymentMode = payMode, ageControlMethod = ageMethod,
                dataphoneAction = dfAction, updatePulsesNeeded = updatePulses,
                useMdbLevel3 = mdbLevel3, timeZone = timeZone,
            )
        }
    }

    enum class PaymentMode(val code: Char) {
        NO_PAYMENT('0'), MDB_NORMAL('1'), PAYMENT_PROTOCOL('2'),
        PULSES_SINGLE_PRICE('3'), MDB_WITH_AGE_CONTROL('4'), PULSES_MULTI_PRICE('5');
        companion object { fun fromChar(c: Char) = entries.find { it.code == c } ?: NO_PAYMENT }
    }

    enum class AgeControlMethod(val code: Char) {
        NONE('0'), AGE_VALIDATOR_APP('1');
        companion object { fun fromChar(c: Char) = entries.find { it.code == c } ?: NONE }
    }

    enum class DataphoneAction(val code: Char) {
        NONE('0'), INITIALIZE('1');
        companion object { fun fromChar(c: Char) = entries.find { it.code == c } ?: NONE }
    }
}
