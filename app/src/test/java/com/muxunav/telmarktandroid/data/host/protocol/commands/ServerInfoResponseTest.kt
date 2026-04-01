package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerInfoResponseTest {

    // Input wire format:
    // Part0: TIMESTAMP(12) + NEXT_COMM(4) + REBOOT_TIME(0-8)
    // Part1: TELEMETRY_FLAG(1) [+ INIT(1) + DELAY(4) + BAUD(6) + OP(6) + PROTO(?)]
    // Part2: 9 bool flags + MAX_CREDIT(5) + PAYMENT_MODE(1) + AGE_METHOD(1) + DATAPHONE_ACTION(1) + UPDATE_PULSES(1) + MDB_LEVEL3(1)
    // Part3: TIMEZONE (optional)

    @Test fun `todos los campos completos con telemetria activa`() {
        val input = "231027143000001512:34:56${SEPARATOR}" +
            "111234115200OPCODEPROTO   ${SEPARATOR}" +
            "1111111111234520111${SEPARATOR}" +
            "Europe/Madrid"
        val r = ServerInfoResponse.deserialize(input.toByteArray(ASCII_CHARSET))

        assertEquals("231027143000", r.timestamp)
        assertEquals(15, r.nextCommMinutes)
        assertEquals("12:34:56", r.rebootTime)
        assertTrue(r.telemetryInUse)
        assertTrue(r.telemetryMachineInitialized)
        assertEquals(1234, r.telemetryReadDelayMinutes)
        assertEquals("115200", r.telemetryBaudRateCode)
        assertEquals("OPCODE", r.telemetryOperatorCode)
        assertEquals("PROTO", r.telemetryProtocol)
        assertTrue(r.tvInUse)
        assertTrue(r.mdbInUse)
        assertTrue(r.updateLcdsNeeded)
        assertTrue(r.updateGcisNeeded)
        assertTrue(r.updateTvNeeded)
        assertTrue(r.updateProductsNeeded)
        assertTrue(r.updateLuminosityNeeded)
        assertTrue(r.updateTftFirmwareNeeded)
        assertTrue(r.updateFirmwareNeeded)
        assertEquals(12345, r.maxMobileCredit)
        assertEquals(ServerInfoResponse.PaymentMode.PAYMENT_PROTOCOL, r.paymentMode)
        assertEquals(ServerInfoResponse.AgeControlMethod.NONE, r.ageControlMethod)
        assertEquals(ServerInfoResponse.DataphoneAction.INITIALIZE, r.dataphoneAction)
        assertTrue(r.updatePulsesNeeded)
        assertTrue(r.useMdbLevel3)
        assertEquals("Europe/Madrid", r.timeZone)
    }

    @Test fun `sin telemetria ni reboot ni timezone`() {
        val input = "2310271000000000${SEPARATOR}0${SEPARATOR}0000000000000000000"
        val r = ServerInfoResponse.deserialize(input.toByteArray(ASCII_CHARSET))

        assertEquals("231027100000", r.timestamp)
        assertEquals(0, r.nextCommMinutes)
        assertNull(r.rebootTime)
        assertFalse(r.telemetryInUse)
        assertFalse(r.telemetryMachineInitialized)
        assertEquals(0, r.telemetryReadDelayMinutes)
        assertNull(r.telemetryBaudRateCode)
        assertNull(r.telemetryOperatorCode)
        assertNull(r.telemetryProtocol)
        assertFalse(r.tvInUse)
        assertFalse(r.mdbInUse)
        assertEquals(0, r.maxMobileCredit)
        assertEquals(ServerInfoResponse.PaymentMode.NO_PAYMENT, r.paymentMode)
        assertEquals(ServerInfoResponse.AgeControlMethod.NONE, r.ageControlMethod)
        assertEquals(ServerInfoResponse.DataphoneAction.NONE, r.dataphoneAction)
        assertNull(r.timeZone)
    }

    @Test fun `telemetria activa con reboot programado`() {
        val input = "231027123000001010:00:00${SEPARATOR}0${SEPARATOR}0000000000000000000"
        val r = ServerInfoResponse.deserialize(input.toByteArray(ASCII_CHARSET))

        assertEquals("10:00:00", r.rebootTime)
        assertFalse(r.telemetryInUse)
    }

    @Test fun `paymentMode MDB_WITH_AGE_CONTROL y ageMethod AGE_VALIDATOR_APP`() {
        val input = "2310271545000002${SEPARATOR}111234${SEPARATOR}1010101011234541000"
        val r = ServerInfoResponse.deserialize(input.toByteArray(ASCII_CHARSET))

        assertEquals(ServerInfoResponse.PaymentMode.MDB_WITH_AGE_CONTROL, r.paymentMode)
        assertEquals(ServerInfoResponse.AgeControlMethod.AGE_VALIDATOR_APP, r.ageControlMethod)
        assertEquals(ServerInfoResponse.DataphoneAction.NONE, r.dataphoneAction)
        assertEquals(12345, r.maxMobileCredit)
    }

    @Test fun `constructor vacio produce valores por defecto`() {
        val r = ServerInfoResponse()
        assertEquals(ServerInfoResponse.PaymentMode.NO_PAYMENT, r.paymentMode)
        assertFalse(r.mdbInUse)
        assertNull(r.timeZone)
    }
}
