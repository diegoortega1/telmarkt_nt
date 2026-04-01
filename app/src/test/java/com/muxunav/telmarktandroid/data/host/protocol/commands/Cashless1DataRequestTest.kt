package com.muxunav.telmarktandroid.data.host.protocol.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Cashless1DataRequestTest {

    @Test fun `serialize produce wire format correcto`() {
        val req = Cashless1DataRequest(
            manufacturer = "MUX", serialNumber = "SN000000001",
            model = "CSH-01", firmwareVersion = "1.00",
            featureLevel = 'A', countryCode = "EUR",
            scaleFactor = 1, decimals = 2, maxResponseTime = 5,
            supportsRefund = true, supportsMultisale = false,
            hasDisplay = true, acceptsCashSaleInfo = false
        )
        val s = req.serialize().decodeToString()
        assertEquals("MUX", s.substring(0, 3))                  // manufacturer padRight(3)
        assertEquals("SN000000001 ", s.substring(3, 15))         // serialNumber padRight(12)
        assertEquals("CSH-01      ", s.substring(15, 27))        // model padRight(12)
        assertEquals("1.00 ", s.substring(27, 32))               // firmwareVersion padRight(5)
        assertEquals('A', s[32])                                  // featureLevel
        assertEquals("EUR  ", s.substring(33, 38))               // countryCode padRight(5)
        assertEquals("01", s.substring(38, 40))                  // scaleFactor padLeft(2)
        assertEquals("02", s.substring(40, 42))                  // decimals padLeft(2)
        assertEquals("005", s.substring(42, 45))                 // maxResponseTime padLeft(3)
        assertEquals('1', s[45])                                  // supportsRefund
        assertEquals('0', s[46])                                  // supportsMultisale
        assertEquals('1', s[47])                                  // hasDisplay
        assertEquals('0', s[48])                                  // acceptsCashSaleInfo
        assertEquals(49, s.length)
    }

    @Test fun `scaleFactor fuera de rango lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            Cashless1DataRequest(
                "MUX", "SN", "MDL", "1.0", 'A', "EUR",
                scaleFactor = 100, decimals = 2, maxResponseTime = 5,
                supportsRefund = false, supportsMultisale = false,
                hasDisplay = false, acceptsCashSaleInfo = false
            )
        }
    }

    @Test fun `featureLevel no ASCII lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            Cashless1DataRequest(
                "MUX", "SN", "MDL", "1.0", '\u0080', "EUR",
                1, 2, 5, false, false, false, false
            )
        }
    }
}
