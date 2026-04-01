package com.muxunav.telmarktandroid.data.host

import javax.inject.Inject

/**
 * Proporciona los datos de identificación del dispositivo que se incluyen
 * en cada cabecera del protocolo y en el payload del ServerInfoRequest.
 *
 * TODO: leer valores reales:
 *   - icc / imei / signalLevel → TelephonyManager (requiere READ_PHONE_STATE)
 *   - ipAddress                → ConnectivityManager / LinkProperties
 *   - serialNumber             → Build.getSerial() o Settings.Secure.ANDROID_ID
 */
class DeviceInfoProvider @Inject constructor() {
    val icc          = "00000000000000000000"   // 20 chars (ICC del SIM)
    val imei         = "000000000000000"         // 15 chars (IMEI del módem)
    val signalLevel  = 99                        // 99 = desconocido
    val ipAddress    = "0.0.0.0"                 // IP asignada al SIM
    val serialNumber = ""                        // se padea a 16 en la cabecera
}
