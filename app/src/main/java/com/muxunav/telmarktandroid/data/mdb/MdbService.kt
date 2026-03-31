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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pax.util.MDBManager
import java.util.concurrent.Executors

class MdbService : Service() {

    private val _state = MutableStateFlow<MdbState>(MdbState.Idle)
    val state: StateFlow<MdbState> = _state.asStateFlow()

    private var fd: Int = -1
    private var mdbManager: MDBManager? = null

    // Canal de log asíncrono — el thread MDB hace trySend() (no-blocking) y un
    // coroutine separado en Dispatchers.IO drena y escribe a Logcat.
    // Así ningún Log.d() bloquea el path crítico de 5ms.
    private val logChannel = Channel<String>(
        capacity        = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val processor = MdbFrameProcessor(
        handler       = MdbProtocolHandler(),
        onStateChange = { _state.value = it },
        onWrite       = { data -> mdbManager?.mdbWrite(fd, data, data.size) },
        onLog         = { msg -> logChannel.trySend(msg) },
    )

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
        logScope.launch { for (msg in logChannel) Log.d("MDB", msg) }
        Log.i("MDB", "Service created — starting MDB loop")
        startMdbLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mdbManager?.let { if (fd >= 0) it.mdbClose(fd) }
        highPriorityExecutor.shutdown()
        logChannel.close()
        logScope.cancel()
        Log.i("MDB", "Service destroyed")
    }

    // ── API pública ───────────────────────────────────────────────────────────

    fun beginSession() = processor.beginSession()
    fun approveVend()  = processor.approveVend()
    fun denyVend()     = processor.denyVend()

    // ── Loop MDB ──────────────────────────────────────────────────────────────

    private fun startMdbLoop() {
        mdbScope.launch {
            mdbManager = MDBManager(applicationContext)
            val readBuffer = ShortArray(256)

            Log.i("MDB", "Opening /dev/mdb_slave...")
            fd = mdbManager!!.mdbOpen("/dev/mdb_slave")
            if (fd < 0) {
                Log.e("MDB", "Failed to open /dev/mdb_slave (fd=$fd)")
                _state.value = MdbState.Error("No se pudo abrir el puerto MDB")
                return@launch
            }

            mdbManager!!.mdbSetMode(fd, 1)
            val addressBytes = byteArrayOf(0x10.toByte())
            mdbManager!!.mdbpSetAddr(fd, addressBytes, addressBytes.size)
            Log.i("MDB", "Port open (fd=$fd) — loop running")

            while (true) {
                val ts = System.currentTimeMillis()
                val wordsRead = mdbManager!!.mdbRead(fd, readBuffer, readBuffer.size, 50000, 1500, 0)
                if (wordsRead > 0) {
                    processor.processFrame(readBuffer, wordsRead)
                    // trySend es no-blocking: el timing log se escribe fuera del thread MDB
                    //logChannel.trySend("⏱ response: ${System.currentTimeMillis() - ts}ms")
                }
            }
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
