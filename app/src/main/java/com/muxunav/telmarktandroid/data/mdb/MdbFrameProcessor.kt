package com.muxunav.telmarktandroid.data.mdb

import com.muxunav.telmarktandroid.domain.model.MdbState
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Máquina de estados MDB — pura Kotlin, sin dependencias Android.
 *
 * Recibe tramas del VMC a través de [processFrame] y produce:
 *   - Cambios de estado de dominio (vía [onStateChange])
 *   - Respuestas de protocolo al hardware (vía [onWrite])
 *   - Mensajes de log (vía [onLog]) — llamado DESPUÉS de [onWrite],
 *     fuera del path crítico de 5ms. El caller decide cómo manejarlos
 *     (canal asíncrono, no-op en tests, etc.).
 *
 * Las flags de operación pendiente son atómicas para soportar llamadas
 * concurrentes desde el hilo MDB (processFrame) y el hilo principal
 * (beginSession/approveVend/denyVend). Cada flag se lee y resetea
 * atómicamente dentro del handler del POLL para garantizar que cada
 * respuesta se envía exactamente una vez.
 */
class MdbFrameProcessor(
    private val handler: MdbProtocolHandler,
    private val onStateChange: (MdbState) -> Unit,
    private val onWrite: (ShortArray) -> Unit,
    private val onLog: (String) -> Unit = {},
) {

    private val pendingBeginSession  = AtomicBoolean(false)
    private val pendingVendApproved  = AtomicReference<ShortArray?>(null)
    private val awaitingUserApproval = AtomicBoolean(false)
    private val pendingVendDenied    = AtomicBoolean(false)

    fun beginSession() { pendingBeginSession.set(true) }

    fun approveVend() { awaitingUserApproval.set(false) }

    fun denyVend() {
        pendingVendApproved.set(null)
        awaitingUserApproval.set(false)
        pendingVendDenied.set(true)
    }

    fun processFrame(buffer: ShortArray, wordsRead: Int) {
        when (val frame = handler.parseFrame(buffer, wordsRead)) {
            is MdbFrame.SetupConfig -> {
                onWrite(handler.readerConfigData)
                onLog("← SETUP_CONFIG  →  readerConfig")
            }
            is MdbFrame.Poll -> {
                when {
                    pendingBeginSession.getAndSet(false) -> {
                        onWrite(handler.beginSessionData)
                        onStateChange(MdbState.SessionActive)
                        onLog("← POLL  →  BEGIN_SESSION  [state: SessionActive]")
                    }
                    pendingVendDenied.getAndSet(false) -> {
                        onWrite(handler.vendDenied)
                        onStateChange(MdbState.VendDenied)
                        onLog("← POLL  →  VEND_DENIED  [state: VendDenied]")
                    }
                    !awaitingUserApproval.get() && pendingVendApproved.get() != null -> {
                        val va = pendingVendApproved.getAndSet(null)!!
                        onWrite(va)
                        onLog("← POLL  →  VEND_APPROVED")
                    }
                    else -> {
                        onWrite(handler.ackData)
                    }
                }
            }
            is MdbFrame.ReaderEnable -> {
                onWrite(handler.ackData)
                onStateChange(MdbState.ReaderEnabled)
                onLog("← READER_ENABLE  →  ACK  [state: ReaderEnabled]")
            }
            is MdbFrame.ReaderDisable -> {
                onWrite(handler.ackData)
                onStateChange(MdbState.Idle)
                onLog("← READER_DISABLE  →  ACK  [state: Idle]")
            }
            is MdbFrame.RevalueRequestLimit -> {
                onWrite(handler.revalueLimitAmount)
                onLog("← REVALUE_REQUEST_LIMIT  →  revalueLimit")
            }
            is MdbFrame.PeripheralId -> {
                onWrite(handler.peripheralId)
                onLog("← PERIPHERAL_ID  →  peripheralId")
            }
            is MdbFrame.SetupMinMaxPrices -> {
                onWrite(handler.ackData)
                onLog("← SETUP_MIN_MAX_PRICES  →  ACK")
            }
            is MdbFrame.VendRequest -> {
                onWrite(handler.ackData)
                val va = handler.buildVendApproved(frame.amountHigh, frame.amountLow)
                pendingVendApproved.set(va)
                awaitingUserApproval.set(true)
                onStateChange(MdbState.VendPending(
                    itemPrice  = frame.itemPrice,
                    itemNumber = frame.itemNumber,
                ))
                onLog("← VEND_REQUEST  price=${frame.itemPrice}  item=${frame.itemNumber}  →  ACK  [state: VendPending]")
            }
            is MdbFrame.VendCancel -> {
                pendingVendApproved.set(null)
                awaitingUserApproval.set(false)
                onWrite(handler.vendDenied)
                onStateChange(MdbState.ReaderEnabled)
                onLog("← VEND_CANCEL  →  VEND_DENIED  [state: ReaderEnabled]")
            }
            is MdbFrame.VendSuccess -> {
                onWrite(handler.ackData)
                onStateChange(MdbState.VendSuccess)
                onLog("← VEND_SUCCESS  →  ACK  [state: VendSuccess]")
            }
            is MdbFrame.VendFailure -> {
                onWrite(handler.ackData)
                onStateChange(MdbState.ReaderEnabled)
                onLog("← VEND_FAILURE  →  ACK  [state: ReaderEnabled]")
            }
            is MdbFrame.Reset -> {
                onWrite(handler.ackData)
                onStateChange(MdbState.Idle)
                onLog("← RESET  →  ACK  [state: Idle]")
            }
            is MdbFrame.VendSessionComplete -> {
                onWrite(handler.vendEndSession)
                onStateChange(MdbState.ReaderEnabled)
                onLog("← VEND_SESSION_COMPLETE  →  END_SESSION  [state: ReaderEnabled]")
            }
            is MdbFrame.Ack     -> onLog("← ACK (ignored)")
            is MdbFrame.Unknown -> onLog("← UNKNOWN frame (ignored)")
        }
    }
}
