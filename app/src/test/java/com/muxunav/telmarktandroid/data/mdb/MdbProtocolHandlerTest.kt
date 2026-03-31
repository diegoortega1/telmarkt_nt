package com.muxunav.telmarktandroid.data.mdb

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MdbProtocolHandlerTest {

    private lateinit var handler: MdbProtocolHandler

    @Before fun setUp() { handler = MdbProtocolHandler() }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateChecksum
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `checksum de array vacío es 0x100`() {
        assertEquals(0x100.toShort(), handler.calculateChecksum(shortArrayOf()))
    }

    @Test fun `checksum de un byte activa el bit 0x100`() {
        // 0x06 or 0x100 = 0x106
        assertEquals(0x106.toShort(), handler.calculateChecksum(shortArrayOf(0x06)))
    }

    @Test fun `checksum es suma de bytes OR 0x100`() {
        // 0x03 + 0xE8 + 0x01 = 0xEC → 0xEC or 0x100 = 0x1EC
        val result = handler.calculateChecksum(shortArrayOf(0x03, 0xE8, 0x01))
        assertEquals((0x03 + 0xE8 + 0x01 or 0x100).toShort(), result)
    }

    @Test fun `checksum del ACK command vendDenied es correcto`() {
        assertEquals(0x106.toShort(), handler.calculateChecksum(shortArrayOf(0x06)))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // parseFrame — trama por trama
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `parseFrame reconoce Reset`() {
        val buf = shortArrayOf(0x110, 0x10)
        assertEquals(MdbFrame.Reset, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce ACK`() {
        val buf = shortArrayOf(0x00)
        assertEquals(MdbFrame.Ack, handler.parseFrame(buf, 1))
    }

    @Test fun `parseFrame reconoce SetupConfig`() {
        val buf = shortArrayOf(0x111, 0x00)
        assertEquals(MdbFrame.SetupConfig, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce SetupMinMaxPrices`() {
        val buf = shortArrayOf(0x111, 0x01)
        assertEquals(MdbFrame.SetupMinMaxPrices, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce Poll`() {
        val buf = shortArrayOf(0x112, 0x12)
        assertEquals(MdbFrame.Poll, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce ReaderEnable`() {
        val buf = shortArrayOf(0x114, 0x01)
        assertEquals(MdbFrame.ReaderEnable, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce ReaderDisable`() {
        val buf = shortArrayOf(0x114, 0x00)
        assertEquals(MdbFrame.ReaderDisable, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce RevalueRequestLimit`() {
        val buf = shortArrayOf(0x115, 0x01)
        assertEquals(MdbFrame.RevalueRequestLimit, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce PeripheralId`() {
        val buf = shortArrayOf(0x117, 0x00)
        assertEquals(MdbFrame.PeripheralId, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce VendCancel`() {
        val buf = shortArrayOf(0x113, 0x01)
        assertEquals(MdbFrame.VendCancel, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce VendSuccess`() {
        val buf = shortArrayOf(0x113, 0x02)
        assertEquals(MdbFrame.VendSuccess, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce VendFailure`() {
        val buf = shortArrayOf(0x113, 0x03)
        assertEquals(MdbFrame.VendFailure, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame reconoce VendSessionComplete`() {
        val buf = shortArrayOf(0x113, 0x04)
        assertEquals(MdbFrame.VendSessionComplete, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame retorna Unknown para trama desconocida`() {
        val buf = shortArrayOf(0x999, 0x99)
        assertEquals(MdbFrame.Unknown, handler.parseFrame(buf, 2))
    }

    @Test fun `parseFrame retorna Unknown para buffer vacío`() {
        assertEquals(MdbFrame.Unknown, handler.parseFrame(shortArrayOf(), 0))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VendRequest — extracción de precio y número de ítem
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `parseFrame extrae precio correctamente de VendRequest`() {
        // precio 0x03E8 = 1000 centavos
        val buf = shortArrayOf(0x113, 0x00, 0x03, 0xE8, 0x00, 0x01)
        val frame = handler.parseFrame(buf, 6) as MdbFrame.VendRequest
        assertEquals(1000.toUShort(), frame.itemPrice)
        assertEquals(0x03.toShort(),  frame.amountHigh)
        assertEquals(0xE8.toShort(),  frame.amountLow)
    }

    @Test fun `parseFrame extrae número de ítem correctamente de VendRequest`() {
        val buf = shortArrayOf(0x113, 0x00, 0x00, 0x64, 0x00, 0x2A)
        val frame = handler.parseFrame(buf, 6) as MdbFrame.VendRequest
        assertEquals(0x2A.toUShort(), frame.itemNumber)
    }

    @Test fun `VendRequest no se reconoce con menos de 6 bytes`() {
        val buf = shortArrayOf(0x113, 0x00, 0x03, 0xE8, 0x00)
        assertEquals(MdbFrame.Unknown, handler.parseFrame(buf, 5))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Integridad de respuestas pre-construidas
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `readerConfigData tiene checksum válido al final`() {
        val data = handler.readerConfigData
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `beginSessionData tiene checksum válido al final`() {
        val data = handler.beginSessionData
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `vendDenied tiene checksum válido al final`() {
        val data = handler.vendDenied
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `vendEndSession tiene checksum válido al final`() {
        val data = handler.vendEndSession
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `revalueLimitAmount tiene checksum válido al final`() {
        val data = handler.revalueLimitAmount
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `peripheralId tiene checksum válido al final`() {
        val data = handler.peripheralId
        val payload = data.copyOf(data.size - 1)
        assertEquals(data.last(), handler.calculateChecksum(payload))
    }

    @Test fun `ackData es exactamente 0x100`() {
        assertArrayEquals(shortArrayOf(0x100), handler.ackData)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildVendApproved
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `buildVendApproved incluye command byte 0x05`() {
        val result = handler.buildVendApproved(0x03, 0xE8.toShort())
        assertEquals(0x05.toShort(), result[0])
    }

    @Test fun `buildVendApproved preserva amountHigh y amountLow`() {
        val result = handler.buildVendApproved(0x03, 0xE8.toShort())
        assertEquals(0x03.toShort(),       result[1])
        assertEquals(0xE8.toShort(),       result[2])
    }

    @Test fun `buildVendApproved tiene checksum válido al final`() {
        val result  = handler.buildVendApproved(0x03, 0xE8.toShort())
        val payload = result.copyOf(result.size - 1)
        assertEquals(result.last(), handler.calculateChecksum(payload))
    }

    @Test fun `buildVendApproved tiene checksum diferente para distintos importes`() {
        val va1 = handler.buildVendApproved(0x01, 0x00)
        val va2 = handler.buildVendApproved(0x02, 0x00)
        assertTrue(va1.last() != va2.last())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consistencia del protocolo MDB: el bit 0x100 del checksum
    // ─────────────────────────────────────────────────────────────────────────

    @Test fun `todo checksum tiene el bit 0x100 activado`() {
        listOf(
            handler.readerConfigData,
            handler.beginSessionData,
            handler.vendDenied,
            handler.vendEndSession,
            handler.revalueLimitAmount,
            handler.buildVendApproved(0x01, 0x00),
        ).forEach { data ->
            assertTrue(
                "Checksum en ${data.toList()} no tiene bit 0x100",
                data.last().toInt() and 0x100 != 0,
            )
        }
    }
}
