package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaleRequestTest {

    // ── serialize ─────────────────────────────────────────────────────────────

    @Test fun `venta simple tipo 0 sin campos opcionales`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail(
                timestamp = "240503123456", productCode = "PROD1",
                price = 1000, saleType = 0, dniId = null, authCode = null
            )
        ))
        val expected = "01240503123456PROD1000010000" + "0      " + "" + SEPARATOR
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `venta tipo 5 con todos los campos`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail(
                timestamp = "240503123456", productCode = "PROD2",
                price = 12345678, saleType = 5, dniId = "1234567",
                authCode = "AUTHCD", cardNumberFirst6Last4 = "1234567890",
                approvalCode = "AB", operationNumber = "OPNUMB"
            )
        ))
        val expected = "01240503123456PROD21234567851234567AUTHCD1234567890ABOPNUMB$SEPARATOR"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `venta tipo 6 denegada con approval code`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail(
                timestamp = "240503100500", productCode = "P0002",
                price = 2500, saleType = 6, dniId = null, authCode = null,
                approvalCode = "XY"
            )
        ))
        // tipo 6: cardNum=10 spaces, approvalCode padded, opNum=6 spaces; dniId=null→"0"
        val expected = "01240503100500P00020000250060                XY      $SEPARATOR"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `multiples ventas con tipos distintos`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail("240503100000", "P0001", 500, 1, "9876543", "AC0001"),
            SaleRequest.SaleDetail("240503100500", "P0002", 2500, 6, null, null, approvalCode = "XY"),
            SaleRequest.SaleDetail(
                "240503101000", "P0003", 10000, 9, "1122334", "AUTH99",
                "0000009999", "ZZ", "999999"
            )
        ))
        val expected = "03" +
            "240503100000P00010000050019876543AC0001$SEPARATOR" +
            "240503100500P00020000250060                XY      $SEPARATOR" +
            "240503101000P00030001000091122334AUTH990000009999ZZ999999$SEPARATOR"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `precio cero y productCode corto se paddean correctamente`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail("240503140000", "FREE", 0, 0, null, null)
        ))
        val expected = "01240503140000FREE 0000000000      $SEPARATOR"
        assertEquals(expected, req.serialize().decodeToString())
    }

    @Test fun `precio maximo y todos los campos al limite`() {
        val req = SaleRequest(listOf(
            SaleRequest.SaleDetail(
                "991231235959", "ABCDE", 99999999, 9, "9999999", "ZZZZZZ",
                "9999999999", "ZZ", "999999"
            )
        ))
        val expected = "01991231235959ABCDE9999999999999999ZZZZZZ9999999999ZZ999999$SEPARATOR"
        assertEquals(expected, req.serialize().decodeToString())
    }

    // ── validación ────────────────────────────────────────────────────────────

    @Test fun `timestamp invalido lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleRequest(listOf(
                SaleRequest.SaleDetail("BADTIMESTAMP", "P0001", 100, 0, null, null)
            ))
        }
    }

    @Test fun `saleType fuera de rango lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleRequest(listOf(
                SaleRequest.SaleDetail("240503123456", "P0001", 100, 10, null, null)
            ))
        }
    }

    @Test fun `tipo 5 sin cardNumber lanza excepcion`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaleRequest(listOf(
                SaleRequest.SaleDetail(
                    "240503123456", "P0001", 100, 5,
                    dniId = null, authCode = null,
                    cardNumberFirst6Last4 = null, // requerido para tipo 5
                    approvalCode = "AB", operationNumber = "123456"
                )
            ))
        }
    }

    @Test fun `mas de 99 ventas lanza excepcion`() {
        val sales = List(100) {
            SaleRequest.SaleDetail("240503123456", "P000$it".take(5), 100, 0, null, null)
        }
        assertThrows(IllegalArgumentException::class.java) { SaleRequest(sales) }
    }
}
