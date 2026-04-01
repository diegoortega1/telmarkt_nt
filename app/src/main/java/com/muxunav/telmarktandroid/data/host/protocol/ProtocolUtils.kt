package com.muxunav.telmarktandroid.data.host.protocol

import java.nio.charset.StandardCharsets

val ASCII_CHARSET: java.nio.charset.Charset = StandardCharsets.US_ASCII

/** ASCII Unit Separator (0x1F) — field delimiter in this protocol. */
const val SEPARATOR = 0x1F.toChar()

/** YYMMDDHHMMSS — timestamp format used in all protocol frames. */
val TIMESTAMP_REGEX = "^([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])([01][0-9]|2[0-3])([0-5][0-9])([0-5][0-9])$".toRegex()

/** YYMMDD — date-only format. */
val DATE_REGEX = "^(\\d{2})(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$".toRegex()

/** HH:MM:SS */
val TIME_REGEX = "^(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$".toRegex()

fun String.padRight(length: Int, char: Char = ' '): String = padEnd(length, char)
fun String.padLeft(length: Int, char: Char = '0'): String = padStart(length, char)
fun Int.padLeft(length: Int, char: Char = '0'): String = toString().padStart(length, char)
fun isAscii(char: Char): Boolean = char.code in 0..127

/** Safe access to a split() part — returns null if index is out of bounds. */
fun List<String>.part(index: Int): String? = getOrNull(index)
