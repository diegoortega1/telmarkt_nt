package com.muxunav.telmarktandroid.domain.model

sealed class MdbState {
    object Idle : MdbState()                    // Reader deshabilitado por el VMC
    object ReaderEnabled : MdbState()           // Lector activo, esperando usuario
    object SessionActive : MdbState()           // Begin Session enviado, usuario seleccionando
    data class VendPending(                     // Vend Request recibido del VMC
        val itemPrice: UShort,
        val itemNumber: UShort
    ) : MdbState()
    object VendSuccess : MdbState()             // VMC confirmó dispensado
    object VendDenied : MdbState()              // Venta denegada por el operador
    data class Error(val message: String) : MdbState()
}
