package com.muxunav.telmarktandroid.data.mdb

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.muxunav.telmarktandroid.domain.model.MdbState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pax.util.MDBManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MdbService : Service() {

    private val _state = MutableStateFlow<MdbState>(MdbState.Idle)
    val state: StateFlow<MdbState> = _state.asStateFlow()

    private var fd: Int = -1
    private var mdbManager: MDBManager? = null
    private val handler = MdbProtocolHandler()

    private val pendingBeginSession  = AtomicBoolean(false)
    private val pendingVendApproved  = AtomicReference<ShortArray?>(null)
    private val awaitingUserApproval = AtomicBoolean(false)
    private val pendingVendDenied    = AtomicBoolean(false)

    private val highPriorityExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MDB-Thread").apply { priority = Thread.MAX_PRIORITY }
    }

    private val mdbScope = CoroutineScope(
        highPriorityExecutor.asCoroutineDispatcher() +
                SupervisorJob() +
                CoroutineName("MDB-Hardware")
    )

    // ── Binder ────────────────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): MdbService = this@MdbService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startMdbLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mdbManager?.let { if (fd >= 0) it.mdbClose(fd) }
        highPriorityExecutor.shutdown()
    }

    // ── API pública ───────────────────────────────────────────────────────────

    fun beginSession() { pendingBeginSession.set(true) }

    fun approveVend() { awaitingUserApproval.set(false) }

    fun denyVend() {
        pendingVendApproved.set(null)
        awaitingUserApproval.set(false)
        pendingVendDenied.set(true)
    }

    // ── Loop MDB ──────────────────────────────────────────────────────────────

    private fun startMdbLoop() {
        mdbScope.launch {
            mdbManager = MDBManager(applicationContext)
            val readBuffer = ShortArray(256)

            fd = mdbManager!!.mdbOpen("/dev/mdb_slave")
            if (fd < 0) {
                Log.e("MDB", "Error abriendo /dev/mdb_slave")
                _state.value = MdbState.Error("No se pudo abrir el puerto MDB")
                return@launch
            }

            mdbManager!!.mdbSetMode(fd, 1)
            val addressBytes = byteArrayOf(0x10.toByte())
            mdbManager!!.mdbpSetAddr(fd, addressBytes, addressBytes.size)

            while (true) {
                val wordsRead = mdbManager!!.mdbRead(fd, readBuffer, readBuffer.size, 50000, 1500, 0)
                if (wordsRead > 0) {
                    val ts = System.currentTimeMillis()
                    handleFrame(fd, mdbManager!!, readBuffer, wordsRead, ts)
                }
            }
        }
    }

    private fun handleFrame(fd: Int, mgr: MDBManager, buffer: ShortArray, wordsRead: Int, ts: Long) {
        when (val frame = handler.parseFrame(buffer, wordsRead)) {
            is MdbFrame.SetupConfig -> {
                mgr.mdbWrite(fd, handler.readerConfigData, handler.readerConfigData.size)
                Log.i("MDB", "SETUP response: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.Poll -> {
                when {
                    pendingBeginSession.getAndSet(false) -> {
                        mgr.mdbWrite(fd, handler.beginSessionData, handler.beginSessionData.size)
                        _state.value = MdbState.SessionActive
                        Log.i("MDB", "Begin Session sent on POLL: ${System.currentTimeMillis() - ts}ms")
                    }
                    pendingVendDenied.getAndSet(false) -> {
                        mgr.mdbWrite(fd, handler.vendDenied, handler.vendDenied.size)
                        _state.value = MdbState.VendDenied
                        Log.i("MDB", "Vend Denied sent on POLL: ${System.currentTimeMillis() - ts}ms")
                    }
                    !awaitingUserApproval.get() && pendingVendApproved.get() != null -> {
                        val va = pendingVendApproved.getAndSet(null)!!
                        mgr.mdbWrite(fd, va, va.size)
                        Log.i("MDB", "Vend Approved sent on POLL: ${System.currentTimeMillis() - ts}ms")
                    }
                    else -> mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                }
            }
            is MdbFrame.ReaderEnable -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                _state.value = MdbState.ReaderEnabled
                Log.i("MDB", "Reader ENABLE: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.ReaderDisable -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                _state.value = MdbState.Idle
                Log.i("MDB", "Reader DISABLE: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.RevalueRequestLimit -> {
                mgr.mdbWrite(fd, handler.revalueLimitAmount, handler.revalueLimitAmount.size)
                Log.i("MDB", "Revalue Limit: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.PeripheralId -> {
                mgr.mdbWrite(fd, handler.peripheralId, handler.peripheralId.size)
                Log.i("MDB", "Peripheral ID: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.SetupMinMaxPrices -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                Log.i("MDB", "Min/Max prices: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.VendRequest -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                val va = handler.buildVendApproved(frame.amountHigh, frame.amountLow)
                pendingVendApproved.set(va)
                awaitingUserApproval.set(true)
                _state.value = MdbState.VendPending(
                    itemPrice  = frame.itemPrice,
                    itemNumber = frame.itemNumber,
                )
                Log.i("MDB", "Vend Request → ACK: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.VendCancel -> {
                pendingVendApproved.set(null)
                awaitingUserApproval.set(false)
                mgr.mdbWrite(fd, handler.vendDenied, handler.vendDenied.size)
                _state.value = MdbState.ReaderEnabled
                Log.i("MDB", "Vend Cancel: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.VendSuccess -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                _state.value = MdbState.VendSuccess
                Log.i("MDB", "Vend Success: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.VendFailure -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                _state.value = MdbState.ReaderEnabled
                Log.i("MDB", "Vend Failure: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.Reset -> {
                mgr.mdbWrite(fd, handler.ackData, handler.ackData.size)
                _state.value = MdbState.Idle
                Log.i("MDB", "Reset: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.VendSessionComplete -> {
                mgr.mdbWrite(fd, handler.vendEndSession, handler.vendEndSession.size)
                _state.value = MdbState.ReaderEnabled
                Log.i("MDB", "Session Complete: ${System.currentTimeMillis() - ts}ms")
            }
            is MdbFrame.Ack     -> Log.i("MDB", "ACK received")
            is MdbFrame.Unknown -> Unit
        }
    }

    // ── Notificación foreground ───────────────────────────────────────────────

    private fun buildNotification() = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "MDB", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TelmarktNT")
            .setContentText("Comunicación MDB activa")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID      = "mdb_channel"
        private const val NOTIFICATION_ID = 1
    }
}
