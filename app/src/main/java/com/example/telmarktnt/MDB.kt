package com.example.testmdb

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.testmdb.ui.theme.TestMDBTheme
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import pax.util.MDBManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {

  private var isReaderEnabled by mutableStateOf(false)
  private var isPendingVendApproval by mutableStateOf(false)  // muestra botón aprobar venta
  private var fd: Int = -1
  private var mdbManager: MDBManager? = null
  private val pendingBeginSession = AtomicBoolean(false)
  private val pendingVendApproved = AtomicReference<ShortArray?>(null) // cola del vend approved

  private val highPriorityExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "MDB-Thread").apply {
      priority = Thread.MAX_PRIORITY
    }
  }

  private val mdbScope = CoroutineScope(
    highPriorityExecutor.asCoroutineDispatcher() +
            SupervisorJob() +
            CoroutineName("MDB-Hardware")
  )

  private val readerConfigData: ShortArray by lazy {
    val data = shortArrayOf(0x01, 0x02, 0x19, 0x78, 0x01, 0x02, 0xB4, 0x09)
    data + shortArrayOf(calculateChecksum(data))
  }
  private val beginSessionData: ShortArray by lazy {
    val data = shortArrayOf(
      0x03,
      0x03, 0xE8,
      0x00, 0xFF,
      0x00, 0xFF,
      0x00,
      0x00, 0x00
    )
    data + shortArrayOf(calculateChecksum(data))
  }
  private val revalueLimitAmount: ShortArray by lazy {
    val data = shortArrayOf(0x0F, 0x00, 0xFF)
    data + shortArrayOf(calculateChecksum(data))
  }
  private fun buildVendApproved(amountHigh: Short, amountLow: Short): ShortArray {
    val data = shortArrayOf(0x05, amountHigh, amountLow)
    return data + shortArrayOf(calculateChecksum(data))
  }
  private val vendDenied: ShortArray by lazy {
    val data = shortArrayOf(0x06)
    data + shortArrayOf(calculateChecksum(data))
  }
  private val vendEndSession: ShortArray by lazy {
    val data = shortArrayOf(0x07)
    data + shortArrayOf(calculateChecksum(data))
  }
  private val peripheralId: ShortArray by lazy {
    val data = shortArrayOf(
      0x09,
      'M'.code.toShort(), 'U'.code.toShort(), 'X'.code.toShort(),
      '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(), '0'.code.toShort(),
      'T'.code.toShort(), 'E'.code.toShort(), 'L'.code.toShort(), 'M'.code.toShort(), 'A'.code.toShort(), 'R'.code.toShort(), 'K'.code.toShort(), 'T'.code.toShort(), '_'.code.toShort(), 'N'.code.toShort(), 'T'.code.toShort(), '_'.code.toShort(),
      0x00, 0x01
    )
    data + shortArrayOf(calculateChecksum(data))
  }
  private val ackData = shortArrayOf(0x100)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    mdbScope.launch {
      mdbManager = MDBManager(applicationContext)
      val readBuffer = ShortArray(256)

      fd = mdbManager!!.mdbOpen("/dev/mdb_slave")
      if (fd < 0) {
        Log.e("MDB", "Error abriendo /dev/mdb_slave")
        return@launch
      }

      mdbManager!!.mdbSetMode(fd, 1)
      val addressBytes = byteArrayOf(16.toByte())
      mdbManager!!.mdbpSetAddr(fd, addressBytes, addressBytes.size)

      while (true) {
        val wordsRead = mdbManager!!.mdbRead(fd, readBuffer, readBuffer.size, 50000, 1500, 0)
        if (wordsRead > 0) {
          val ts = System.currentTimeMillis()
          handleFrame(fd, mdbManager!!, readBuffer, wordsRead, ts)
        }
      }
    }

    enableEdgeToEdge()
    setContent {
      TestMDBTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainScreen(
            isReaderEnabled = isReaderEnabled,
            isPendingVendApproval = isPendingVendApproval,
            onVendClick = { pendingBeginSession.set(true) },
            onApproveVend = { approveVend() },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  // Encola el vend approved con el precio recibido en el Vend Request
  private fun approveVend() {
    val current = pendingVendApproved.get() ?: return
    Log.i("MDB", "Vend Approved encolado")
    // ya está en la cola, solo ocultamos el botón
    isPendingVendApproval = false
  }

  private fun handleFrame(fd: Int, mdbManager: MDBManager, buffer: ShortArray, wordsRead: Int, ts: Long) {
    when {
      isSetupConfigData(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, readerConfigData, readerConfigData.size)
        Log.i("MDB", "SETUP response: ${System.currentTimeMillis() - ts}ms")
      }

      isPoll(buffer, wordsRead) -> {
        when {
          pendingBeginSession.getAndSet(false) -> {
            mdbManager.mdbWrite(fd, beginSessionData, beginSessionData.size)
            Log.i("MDB", "Begin Session sent on POLL: ${System.currentTimeMillis() - ts}ms")
          }
          !isPendingVendApproval && pendingVendApproved.get() != null -> {
            // el usuario ya pulsó aprobar, enviamos en este POLL
            val va = pendingVendApproved.getAndSet(null)!!
            mdbManager.mdbWrite(fd, va, va.size)
            Log.i("MDB", "Vend Approved sent on POLL: ${System.currentTimeMillis() - ts}ms")
          }
          else -> {
            mdbManager.mdbWrite(fd, ackData, ackData.size)
          }
        }
      }

      isReaderEnable(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        isReaderEnabled = true
        Log.i("MDB", "Reader ENABLE response: ${System.currentTimeMillis() - ts}ms")
      }

      isReaderDisable(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        isReaderEnabled = false
        Log.i("MDB", "Reader DISABLE response: ${System.currentTimeMillis() - ts}ms")
      }

      isRevalueRequestLimit(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, revalueLimitAmount, revalueLimitAmount.size)
        Log.i("MDB", "Revalue Limit response: ${System.currentTimeMillis() - ts}ms")
      }

      isPeripheralId(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, peripheralId, peripheralId.size)
        Log.i("MDB", "Peripheral ID response: ${System.currentTimeMillis() - ts}ms")
      }

      isSetUpMinMaxPrices(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        Log.i("MDB", "Min/Max prices response: ${System.currentTimeMillis() - ts}ms")
      }

      isVendRequest(buffer, wordsRead) -> {
        // 1. Contestamos ACK inmediatamente
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        // 2. Guardamos el vend approved con el precio del request
        val va = buildVendApproved(buffer[2], buffer[3])
        pendingVendApproved.set(va)
        // 3. Mostramos botón en UI para que el usuario apruebe
        isPendingVendApproval = true
        Log.i("MDB", "Vend Request → ACK, esperando aprobación usuario: ${System.currentTimeMillis() - ts}ms")
      }

      isVendCancel(buffer, wordsRead) -> {
        pendingVendApproved.set(null)
        isPendingVendApproval = false
        mdbManager.mdbWrite(fd, vendDenied, vendDenied.size)
        Log.i("MDB", "Vend Cancel response: ${System.currentTimeMillis() - ts}ms")
      }

      isVendSuccess(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        Log.i("MDB", "Vend Success response: ${System.currentTimeMillis() - ts}ms")
      }

      isVendFailure(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        Log.i("MDB", "Vend Failure response: ${System.currentTimeMillis() - ts}ms")
      }

      isReset(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, ackData, ackData.size)
        Log.i("MDB", "Reset response: ${System.currentTimeMillis() - ts}ms")
      }

      isVendSessionComplete(buffer, wordsRead) -> {
        mdbManager.mdbWrite(fd, vendEndSession, vendEndSession.size)
        isReaderEnabled = true
        Log.i("MDB", "Session Complete response: ${System.currentTimeMillis() - ts}ms")
      }

      isACK(buffer, wordsRead) -> {
        Log.i("MDB", "ACK received")
      }

      else -> {
        // Log.i("MDB", "TRAMA desconocida: ${buffer.toHex(wordsRead)}")
      }
    }
  }

  private fun isReset(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x110 && buf[1].toInt() == 0x10
  private fun isACK(buf: ShortArray, len: Int) =
    len == 1 && buf[0].toInt() == 0x00
  private fun isSetupConfigData(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x111 && buf[1].toInt() == 0x00
  private fun isPoll(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x112 && buf[1].toInt() == 0x12
  private fun isReaderEnable(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x114 && buf[1].toInt() == 0x01
  private fun isReaderDisable(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x114 && buf[1].toInt() == 0x00
  private fun isVendRequest(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x00
  private fun isRevalueRequestLimit(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x115 && buf[1].toInt() == 0x01
  private fun isPeripheralId(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x117 && buf[1].toInt() == 0x00
  private fun isSetUpMinMaxPrices(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x111 && buf[1].toInt() == 0x01
  private fun isVendCancel(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x01
  private fun isVendSuccess(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x02
  private fun isVendFailure(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x03
  private fun isVendSessionComplete(buf: ShortArray, len: Int) =
    len >= 2 && buf[0].toInt() == 0x113 && buf[1].toInt() == 0x04

  private fun calculateChecksum(bytes: ShortArray): Short =
    (bytes.fold(0.toShort()) { acc, byte -> (acc + byte).toShort() }.toInt() or 0x100).toShort()

  private fun ShortArray.toHex(len: Int): String =
    take(len).joinToString(" ") { s ->
      val value = s.toInt() and 0xFF
      val isAddress = (s.toInt() and 0x100) != 0
      if (isAddress) "[${String.format("%02Xh", value)}*]" else String.format("%02Xh", value)
    }
}

@Composable
fun MainScreen(
  isReaderEnabled: Boolean,
  isPendingVendApproval: Boolean,
  onVendClick: () -> Unit,
  onApproveVend: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    when {
      isPendingVendApproval -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text("Venta pendiente de aprobación")
          Button(onClick = onApproveVend) {
            Text("APROBAR VENTA")
          }
        }
      }
      isReaderEnabled -> {
        Button(onClick = onVendClick) {
          Text("INICIAR VENTA")
        }
      }
      else -> {
        Text("Esperando habilitación del lector...")
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  TestMDBTheme {
    MainScreen(
      isReaderEnabled = true,
      isPendingVendApproval = false,
      onVendClick = {},
      onApproveVend = {}
    )
  }
}