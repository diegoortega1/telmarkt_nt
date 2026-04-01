package com.muxunav.telmarktandroid.data.host.protocol.commands

import com.muxunav.telmarktandroid.data.host.protocol.ASCII_CHARSET
import com.muxunav.telmarktandroid.data.host.protocol.DATE_REGEX
import com.muxunav.telmarktandroid.data.host.protocol.Request
import com.muxunav.telmarktandroid.data.host.protocol.Response
import com.muxunav.telmarktandroid.data.host.protocol.SEPARATOR
import com.muxunav.telmarktandroid.data.host.protocol.padRight

// ── 058 ──────────────────────────────────────────────────────────────────────

data class DniDataRequest(
    val dateOfBirth: String,        // YYMMDD
    val sex: Char,                  // 'M' or 'F'
    val moreInfo: Boolean,
    // Optional — required only when moreInfo = true
    val name: String? = null,       // ≤40
    val surnames: String? = null,   // ≤80
    val nationality: String? = null, // 3 chars
    val expiryDate: String? = null, // YYMMDD
    val documentCode: String? = null, // 9 chars
    val documentType: String? = null, // 2 chars
    val documentCountry: String? = null, // 3 chars
    val dniNumber: String? = null,  // 9 chars
) : Request() {

    override val commandCode = "058"

    init {
        require(dateOfBirth.length == 6 && dateOfBirth.matches(DATE_REGEX)) {
            "dateOfBirth must be YYMMDD format"
        }
        require(sex == 'M' || sex == 'F') { "sex must be 'M' or 'F'" }
        if (moreInfo) {
            require(name == null || name.length <= 40) { "name must be ≤40 chars" }
            require(surnames == null || surnames.length <= 80) { "surnames must be ≤80 chars" }
            require(nationality == null || nationality.length == 3) { "nationality must be 3 chars" }
            require(expiryDate == null || (expiryDate.length == 6 && expiryDate.matches(DATE_REGEX))) {
                "expiryDate must be YYMMDD format"
            }
            require(documentCode == null || documentCode.length == 9) { "documentCode must be 9 chars" }
            require(documentType == null || documentType.length == 2) { "documentType must be 2 chars" }
            require(documentCountry == null || documentCountry.length == 3) { "documentCountry must be 3 chars" }
            require(dniNumber == null || dniNumber.length == 9) { "dniNumber must be 9 chars" }
        }
    }

    override fun serialize(): ByteArray {
        val sb = StringBuilder()
        sb.append(dateOfBirth.padRight(6))
        sb.append(sex)
        sb.append(if (moreInfo) '1' else '0')
        if (moreInfo) {
            name?.let { sb.append(it) }; sb.append(SEPARATOR)
            surnames?.let { sb.append(it) }; sb.append(SEPARATOR)
            nationality?.let { sb.append(it) }
            expiryDate?.let { sb.append(it.padRight(6)) }
            documentCode?.let { sb.append(it.padRight(9)) }
            documentType?.let { sb.append(it.padRight(2)) }
            documentCountry?.let { sb.append(it.padRight(3)) }
            dniNumber?.let { sb.append(it.padRight(9)) }
        }
        return sb.toString().toByteArray(ASCII_CHARSET)
    }
}

// ── 059 ──────────────────────────────────────────────────────────────────────

data class DniDataResponse(
    val dniId: String, // 7 chars; "0" = unknown
) : Response() {

    override val commandCode = "059"

    companion object {
        fun deserialize(bytes: ByteArray): DniDataResponse {
            require(bytes.size >= 7) { "DniDataResponse too short" }
            val id = bytes.copyOfRange(0, 7).toString(ASCII_CHARSET).trim()
            return DniDataResponse(id)
        }
    }
}
