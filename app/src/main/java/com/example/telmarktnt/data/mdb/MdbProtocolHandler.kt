package com.example.telmarktnt.data.mdb

// ── Tipos de trama MDB recibidas del VMC ─────────────────────────────────────

sealed class MdbFrame {
    object Unknown             : MdbFrame()
    object Reset               : MdbFrame()
    object Ack                 : MdbFrame()
    object SetupConfig         : MdbFrame()
    object SetupMinMaxPrices   : MdbFrame()
    object Poll                : MdbFrame()
    object ReaderEnable        : MdbFrame()
    object ReaderDisable       : MdbFrame()
    object RevalueRequestLimit : MdbFrame()
    object PeripheralId        : MdbFrame()
    object VendCancel          : MdbFrame()
    object VendSuccess         : MdbFrame()
    object VendFailure         : MdbFrame()
    object VendSessionComplete : MdbFrame()
    data class VendRequest(
        val amountHigh: Short,
        val amountLow:  Short,
        val itemPrice:  UShort,
        val itemNumber: UShort,
    ) : MdbFrame()
}

// ── Manejador de protocolo (pure Kotlin — sin dependencias Android) ───────────

class MdbProtocolHandler {

    // ── Checksum ──────────────────────────────────────────────────────────────

    /**
     * Suma todos los bytes y activa el bit 8 (0x100) para marcar el byte como
     * checksum en el protocolo MDB. El acumulador se trunca a Short en cada
     * paso para replicar el comportamiento del hardware.
     */
    fun calculateChecksum(bytes: ShortArray): Short =
        (bytes.fold(0.toShort()) { acc, b ->
            (acc + b).toShort()
        }.toInt() or 0x100).toShort()

    // ── Detección de tramas ───────────────────────────────────────────────────

    fun parseFrame(buf: ShortArray, len: Int): MdbFrame = when {
        len >= 2 && buf[0].toInt() == 0x110 && buf[1].toInt() == 0x10 -> MdbFrame.Reset
        len == 1 && buf[0].toInt() == 0x00                             -> MdbFrame.Ack
        len >= 2 && buf[0].toInt() == 0x111 && buf[1].toInt() == 0x00 -> MdbFrame.SetupConfig
        len >= 2 && buf[0].toInt() == 0x111 && buf[1].toInt() == 0x01 -> MdbFrame.SetupMinMaxPrices
        len >= 2 && buf[0].toInt() == 0x112 && buf[1].toInt() == 0x12 -> MdbFrame.Poll
        len >= 2 && buf[0].toInt() == 0x114 && buf[1].toInt() == 0x01 -> MdbFrame.ReaderEnable
        len >= 2 && buf[0].toInt() == 0x114 && buf[1].toInt() == 0x00 -> MdbFrame.ReaderDisable
        len >= 2 && buf[0].toInt() == 0x115 && buf[1].toInt() == 0x01 -> MdbFrame.RevalueRequestLimit
        len >= 2 && buf[0].toInt() == 0x117 && buf[1].toInt() == 0x00 -> MdbFrame.PeripheralId
        len >= 6 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x00 -> MdbFrame.VendRequest(
            amountHigh = buf[2],
            amountLow  = buf[3],
            itemPrice  = ((buf[2].toInt() shl 8) or buf[3].toInt()).toUShort(),
            itemNumber = ((buf[4].toInt() shl 8) or buf[5].toInt()).toUShort(),
        )
        len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x01 -> MdbFrame.VendCancel
        len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x02 -> MdbFrame.VendSuccess
        len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x03 -> MdbFrame.VendFailure
        len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x04 -> MdbFrame.VendSessionComplete
        else -> MdbFrame.Unknown
    }

    // ── Respuestas pre-construidas ────────────────────────────────────────────

    val ackData: ShortArray = shortArrayOf(0x100)

    val readerConfigData: ShortArray by lazy {
        val d = shortArrayOf(0x01, 0x02, 0x19, 0x78, 0x01, 0x02, 0xB4, 0x09)
        d + shortArrayOf(calculateChecksum(d))
    }

    val beginSessionData: ShortArray by lazy {
        val d = shortArrayOf(0x03, 0x03, 0xE8, 0x00, 0xFF, 0x00, 0xFF, 0x00, 0x00, 0x00)
        d + shortArrayOf(calculateChecksum(d))
    }

    val revalueLimitAmount: ShortArray by lazy {
        val d = shortArrayOf(0x0F, 0x00, 0xFF)
        d + shortArrayOf(calculateChecksum(d))
    }

    val vendDenied: ShortArray by lazy {
        val d = shortArrayOf(0x06)
        d + shortArrayOf(calculateChecksum(d))
    }

    val vendEndSession: ShortArray by lazy {
        val d = shortArrayOf(0x07)
        d + shortArrayOf(calculateChecksum(d))
    }

    val peripheralId: ShortArray by lazy {
        val d = shortArrayOf(
            0x09,
            'M'.code.toShort(), 'U'.code.toShort(), 'X'.code.toShort(),
            '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(),
            '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(),
            '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(),
            'T'.code.toShort(), 'E'.code.toShort(), 'L'.code.toShort(), 'M'.code.toShort(),
            'A'.code.toShort(), 'R'.code.toShort(), 'K'.code.toShort(), 'T'.code.toShort(),
            '_'.code.toShort(), 'N'.code.toShort(), 'T'.code.toShort(), '_'.code.toShort(),
            0x00, 0x01,
        )
        d + shortArrayOf(calculateChecksum(d))
    }

    fun buildVendApproved(amountHigh: Short, amountLow: Short): ShortArray {
        val d = shortArrayOf(0x05, amountHigh, amountLow)
        return d + shortArrayOf(calculateChecksum(d))
    }
}
