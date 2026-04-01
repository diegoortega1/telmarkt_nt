package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerInfoRequestTest {

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test fun `todos los campos completos`() {
        val req = ServerInfoRequest(
            icc = "12345678901234567890", imei = "987654321098765",
            signalLevel = 15, specialInfo = ServerInfoRequest.SpecialInfo.READER_INIT,
            ipAddress = "192.168.1.100",
            frequencyBand = "Band1", operator = "OperatorA", renesasFirmware = "1.23"
        )
        val expected = "12345678901234567890987654321098765152192.168.1.100" +
            "${SEPARATOR}Band1${SEPARATOR}OperatorA${SEPARATOR}1.23${SEPARATOR}"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `campos opcionales null generan separadores vacios`() {
        val req = ServerInfoRequest(
            icc = "ICC12345678901234567", imei = "IMEI98765432109",
            signalLevel = 99, specialInfo = ServerInfoRequest.SpecialInfo.START_UP,
            ipAddress = "192.168.0.1"
        )
        val expected = "ICC12345678901234567IMEI98765432109991192.168.0.1" +
            "${SEPARATOR}${SEPARATOR}${SEPARATOR}${SEPARATOR}"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `icc e imei cortos se paddean con ceros a la izquierda`() {
        val req = ServerInfoRequest(
            icc = "1", imei = "2", signalLevel = 6,
            specialInfo = ServerInfoRequest.SpecialInfo.START_UP,
            ipAddress = "127.0.0.1",
            frequencyBand = "3", operator = "4", renesasFirmware = "5"
        )
        val expected = "00000000000000000001000000000000002061127.0.0.1" +
            "${SEPARATOR}3${SEPARATOR}4${SEPARATOR}5${SEPARATOR}"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `signalLevel 99 (sin señal) se serializa correctamente`() {
        val req = ServerInfoRequest(
            icc = "ICC", imei = "IMEI", signalLevel = 99,
            specialInfo = ServerInfoRequest.SpecialInfo.READER_RESET,
            ipAddress = "127.0.0.1"
        )
        val serialized = req.serialize().decodeToString()
        // signalLevel=99 → "99", specialInfo READER_RESET=3 → byte '\u0003'
        assertEquals("99", serialized.substring(35, 37))
    }

    @Test fun `todos los SpecialInfo generan el digito ASCII correcto`() {
        fun charAt(info: ServerInfoRequest.SpecialInfo): Char {
            val req = ServerInfoRequest("ICC", "IMEI", 0, info, "127.0.0.1")
            return req.serialize().decodeToString()[37]
        }
        assertEquals('0', charAt(ServerInfoRequest.SpecialInfo.NORMAL))
        assertEquals('1', charAt(ServerInfoRequest.SpecialInfo.START_UP))
        assertEquals('2', charAt(ServerInfoRequest.SpecialInfo.READER_INIT))
        assertEquals('3', charAt(ServerInfoRequest.SpecialInfo.READER_RESET))
    }

    // ── validación ────────────────────────────────────────────────────────────

    @Test fun `icc demasiado larga lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerInfoRequest("A".repeat(21), "IMEI", 0, ServerInfoRequest.SpecialInfo.NORMAL, "127.0.0.1")
        }
    }

    @Test fun `signalLevel invalido lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerInfoRequest("ICC", "IMEI", 32, ServerInfoRequest.SpecialInfo.NORMAL, "127.0.0.1")
        }
    }

    @Test fun `ipAddress demasiado corta lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerInfoRequest("ICC", "IMEI", 0, ServerInfoRequest.SpecialInfo.NORMAL, "1.2.3") // <7 chars
        }
    }
}
