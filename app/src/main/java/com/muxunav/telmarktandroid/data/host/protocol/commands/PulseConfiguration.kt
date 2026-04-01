package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response

// ── 080 — empty request ───────────────────────────────────────────────────────

class PulseConfigurationRequest : Request() {
    override val commandCode = "080"
    override fun serialize(): ByteArray = ByteArray(0)
}

// ── 081 ──────────────────────────────────────────────────────────────────────

data class PulseConfigurationResponse(
    val workMode: WorkMode,
    val inhibition: Inhibition,
    val pulseDurationMillis: Int,
    val pauseDurationMillis: Int,
    val prices: List<Price>,
) : Response() {

    override val commandCode = "081"

    companion object {
        fun deserialize(bytes: ByteArray): PulseConfigurationResponse {
            val s = bytes.toString(ASCII_CHARSET)
            var offset = 0

            val workMode = WorkMode.fromChar(s.getOrNull(offset++))
            val inhibition = Inhibition.fromChar(s.getOrNull(offset++))
            val pulseDuration = s.substring(offset, offset + 4).toInt(); offset += 4
            val pauseDuration = s.substring(offset, offset + 4).toInt(); offset += 4
            val priceCount = s.substring(offset, offset + 2).toInt(); offset += 2

            val prices = buildList {
                repeat(priceCount) {
                    val amount = s.substring(offset, offset + 5).toInt(); offset += 5
                    val pulses = s.substring(offset, offset + 2).toInt(); offset += 2
                    val io1 = s.getOrNull(offset++) == '1'
                    val io2 = s.getOrNull(offset++) == '1'
                    val io3 = s.getOrNull(offset++) == '1'
                    val io4 = s.getOrNull(offset++) == '1'
                    val offerWithTelmarkt = s.getOrNull(offset++) == '1'
                    add(Price(amount, pulses, io1, io2, io3, io4, offerWithTelmarkt))
                }
            }

            return PulseConfigurationResponse(workMode, inhibition, pulseDuration, pauseDuration, prices)
        }
    }

    data class Price(
        val amountCents: Int,
        val pulseCount: Int,
        val io1Enabled: Boolean,
        val io2Enabled: Boolean,
        val io3Enabled: Boolean,
        val io4Enabled: Boolean,
        val offerWithTelmarkt: Boolean,
    )

    enum class WorkMode(val code: Char) {
        SERIE('0'), PARALELO('1'), BINARIO('2');
        companion object { fun fromChar(c: Char?) = entries.find { it.code == c } ?: SERIE }
    }

    enum class Inhibition(val code: Char) {
        NONE('0'),
        LOW_LEVEL_INTERNAL_PULLUP('1'),
        HIGH_LEVEL_INTERNAL_PULLUP('2'),
        LOW_LEVEL_NO_PULLUP('3'),
        HIGH_LEVEL_NO_PULLUP('4');
        companion object { fun fromChar(c: Char?) = entries.find { it.code == c } ?: NONE }
    }
}
