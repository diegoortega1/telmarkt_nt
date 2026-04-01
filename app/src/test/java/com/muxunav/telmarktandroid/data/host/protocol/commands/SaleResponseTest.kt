package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SaleResponseTest {

    @Test fun `sin flag de actualizacion`() {
        val r = SaleResponse.deserialize("00231231123456".toByteArray(ASCII_CHARSET))
        assertEquals(0, r.flagsByteCount)
        assertNull(r.updateRa2l1Needed)
        assertEquals("231231123456", r.currentTimestamp)
    }

    @Test fun `con flag de actualizacion en true`() {
        val r = SaleResponse.deserialize("011231231123456".toByteArray(ASCII_CHARSET))
        assertEquals(1, r.flagsByteCount)
        assertEquals(true, r.updateRa2l1Needed)
        assertEquals("231231123456", r.currentTimestamp)
    }

    @Test fun `con flag de actualizacion en false`() {
        val r = SaleResponse.deserialize("010231231123456".toByteArray(ASCII_CHARSET))
        assertEquals(1, r.flagsByteCount)
        assertEquals(false, r.updateRa2l1Needed)
        assertEquals("231231123456", r.currentTimestamp)
    }

    @Test fun `payload demasiado corto lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleResponse.deserialize("short".toByteArray(ASCII_CHARSET))
        }
    }

    @Test fun `payload vacio lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleResponse.deserialize(ByteArray(0))
        }
    }
}
