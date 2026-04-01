package com.muxunav.telmarktandroid.data.host.protocol

/** Base type for all protocol messages. */
abstract class Payload {
    abstract val commandCode: String
}

/** Client → Server. Must provide a binary serialization. */
abstract class Request : Payload() {
    abstract fun serialize(): ByteArray
}

/** Server → Client. Deserialized by companion objects in each subclass. */
abstract class Response : Payload()
