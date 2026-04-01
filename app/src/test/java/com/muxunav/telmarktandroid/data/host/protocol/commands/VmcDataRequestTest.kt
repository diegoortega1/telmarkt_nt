package com.muxunav.telmarktandroid.data.host.protocol.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VmcDataRequestTest {

    @Test fun `serialize produce wire format correcto`() {
        val req = VmcDataRequest(
            manufacturer = "MUX", serialNumber = "SN0001",
            model = "TLM-01", firmwareVersion = "1.0",
            featureLevel = '1', displayColumns = 20, displayRows = 4,
            displayType = 0, maxPrice = 9999, minPrice = 100
        )
        val s = req.serialize().decodeToString()
        // manufacturer padRight(3) = "MUX"
        assertEquals("MUX", s.substring(0, 3))
        // serialNumber padLeft(12) = "      SN0001"
        assertEquals("      SN0001", s.substring(3, 15))
        // model padLeft(12) = "      TLM-01"
        assertEquals("      TLM-01", s.substring(15, 27))
        // firmwareVersion padLeft(5) = "  1.0"
        assertEquals("  1.0", s.substring(27, 32))
        // featureLevel = '1'
        assertEquals('1', s[32])
        // displayColumns padLeft(2) = "20"
        assertEquals("20", s.substring(33, 35))
        // displayRows padLeft(2) = "04"
        assertEquals("04", s.substring(35, 37))
        // displayType padLeft(2) = "00"
        assertEquals("00", s.substring(37, 39))
        // maxPrice padLeft(5) = "09999"
        assertEquals("09999", s.substring(39, 44))
        // minPrice padLeft(5) = "00100"
        assertEquals("00100", s.substring(44, 49))
    }

    @Test fun `featureLevel no digito lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            VmcDataRequest("MUX", "SN0001", "TLM-01", "1.0", 'X', 20, 4, 0, 9999, 100)
        }
    }

    @Test fun `precio maximo fuera de rango lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            VmcDataRequest("MUX", "SN0001", "TLM-01", "1.0", '1', 20, 4, 0, 100000, 100)
        }
    }
}
