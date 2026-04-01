package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseConfigurationResponseTest {

    @Test fun `deserializa configuracion con dos precios`() {
        // WorkMode=SERIE('0') Inhibition=NONE('0') pulse=0100 pause=0050 count=02
        // Price1: amount=00100 pulses=03 io1=1 io2=0 io3=0 io4=0 telmarkt=1
        // Price2: amount=00250 pulses=05 io1=0 io2=1 io3=1 io4=0 telmarkt=0
        val wire = "000100005002" +
            "001000310001002500501100"
        val r = PulseConfigurationResponse.deserialize(wire.toByteArray(ASCII_CHARSET))

        assertEquals(PulseConfigurationResponse.WorkMode.SERIE, r.workMode)
        assertEquals(PulseConfigurationResponse.Inhibition.NONE, r.inhibition)
        assertEquals(100, r.pulseDurationMillis)
        assertEquals(50, r.pauseDurationMillis)
        assertEquals(2, r.prices.size)

        val p1 = r.prices[0]
        assertEquals(100, p1.amountCents)
        assertEquals(3, p1.pulseCount)
        assertTrue(p1.io1Enabled)
        assertFalse(p1.io2Enabled)
        assertFalse(p1.io3Enabled)
        assertFalse(p1.io4Enabled)
        assertTrue(p1.offerWithTelmarkt)

        val p2 = r.prices[1]
        assertEquals(250, p2.amountCents)
        assertEquals(5, p2.pulseCount)
        assertFalse(p2.io1Enabled)
        assertTrue(p2.io2Enabled)
        assertTrue(p2.io3Enabled)
        assertFalse(p2.io4Enabled)
        assertFalse(p2.offerWithTelmarkt)
    }

    @Test fun `sin precios`() {
        val wire = "120200030000"
        val r = PulseConfigurationResponse.deserialize(wire.toByteArray(ASCII_CHARSET))
        assertEquals(PulseConfigurationResponse.WorkMode.PARALELO, r.workMode)
        assertEquals(PulseConfigurationResponse.Inhibition.HIGH_LEVEL_INTERNAL_PULLUP, r.inhibition)
        assertEquals(0, r.prices.size)
    }

    @Test fun `WorkMode desconocido cae en SERIE por defecto`() {
        val wire = "X00100000000"
        val r = PulseConfigurationResponse.deserialize(wire.toByteArray(ASCII_CHARSET))
        assertEquals(PulseConfigurationResponse.WorkMode.SERIE, r.workMode)
    }
}
