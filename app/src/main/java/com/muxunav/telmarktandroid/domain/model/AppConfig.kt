package com.muxunav.telmarktandroid.domain.model

data class AppConfig(
    val paymentMode: PaymentMode,
    val ageControlMethod: AgeControlMethod,
    val nextCommMinutes: Int,
    val mdbInUse: Boolean,
    val useMdbLevel3: Boolean,
    val maxMobileCredit: Int,
    val rebootTime: String?,          // HH:MM:SS or null
    val updateProductsNeeded: Boolean,
    val updatePulsesNeeded: Boolean,
    val updateLcdsNeeded: Boolean,
    val lastSyncAt: Long,             // epoch millis; 0 = nunca sincronizado
) {
    enum class PaymentMode {
        NO_PAYMENT, MDB_NORMAL, PAYMENT_PROTOCOL,
        PULSES_SINGLE_PRICE, MDB_WITH_AGE_CONTROL, PULSES_MULTI_PRICE
    }

    enum class AgeControlMethod { NONE, AGE_VALIDATOR_APP }

    companion object {
        /** Config de arranque segura cuando el servidor nunca ha respondido. */
        val DEFAULT = AppConfig(
            paymentMode = PaymentMode.MDB_NORMAL,
            ageControlMethod = AgeControlMethod.NONE,
            nextCommMinutes = 60,
            mdbInUse = true,
            useMdbLevel3 = false,
            maxMobileCredit = 0,
            rebootTime = null,
            updateProductsNeeded = false,
            updatePulsesNeeded = false,
            updateLcdsNeeded = false,
            lastSyncAt = 0L,
        )
    }
}
