package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DniDataTest {

    // ── DniDataRequest serialize ───────────────────────────────────────────────

    @Test fun `moreInfo false solo serializa fecha y sexo`() {
        val req = DniDataRequest(dateOfBirth = "900115", sex = 'M', moreInfo = false)
        val s = req.serialize().decodeToString()
        assertEquals("900115", s.substring(0, 6))
        assertEquals('M', s[6])
        assertEquals('0', s[7])   // moreInfo = false → '0'
        assertEquals(8, s.length)
    }

    @Test fun `moreInfo true serializa bloque completo`() {
        val req = DniDataRequest(
            dateOfBirth = "900115", sex = 'F', moreInfo = true,
            name = "MARIA", surnames = "GARCIA LOPEZ",
            nationality = "ESP", expiryDate = "270101",
            documentCode = "123456789", documentType = "ID",
            documentCountry = "ESP", dniNumber = "12345678Z"
        )
        val s = req.serialize().decodeToString()
        assertEquals("900115", s.substring(0, 6))
        assertEquals('F', s[6])
        assertEquals('1', s[7])   // moreInfo = true → '1'
        // nombre + SEPARATOR + apellidos + SEPARATOR + datos fijos
        val rest = s.substring(8)
        val parts = rest.split(SEPARATOR)
        assertEquals("MARIA", parts[0])
        assertEquals("GARCIA LOPEZ", parts[1])
    }

    @Test fun `sexo invalido lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            DniDataRequest(dateOfBirth = "900115", sex = 'X', moreInfo = false)
        }
    }

    @Test fun `fecha de nacimiento invalida lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            DniDataRequest(dateOfBirth = "900132", sex = 'M', moreInfo = false) // día 32
        }
    }

    // ── DniDataResponse deserialize ───────────────────────────────────────────

    @Test fun `deserializa dniId correctamente`() {
        val r = DniDataResponse.deserialize("1234567".toByteArray(ASCII_CHARSET))
        assertEquals("1234567", r.dniId)
    }

    @Test fun `deserializa con espacios trailing recortados`() {
        val r = DniDataResponse.deserialize("1234   ".toByteArray(ASCII_CHARSET))
        assertEquals("1234", r.dniId)
    }

    @Test fun `payload demasiado corto lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            DniDataResponse.deserialize("123456".toByteArray(ASCII_CHARSET)) // 6 bytes, necesita 7
        }
    }
}
