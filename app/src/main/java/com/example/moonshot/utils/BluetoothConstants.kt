package com.example.moonshot.utils

import java.util.*

object BluetoothConstants {
    const val SCAN_PERIOD: Long = 10000
    const val REQUEST_ENABLE_BT = 1
    const val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1

    val NOTIFY_SERVICE = UUID.fromString("23edd8d1-70be-477d-b4e3-0fda81aa8d62")
    val NOTIFY_CHARACTERISTIC = UUID.fromString("dbb9219f-8c07-4f69-a70b-2aabde4a3675")
    val NOTIFY_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val WRITE_SERVICE = UUID.fromString("23edd8d1-70be-477d-b4e3-0fda81aa8d62")
    val WRITE_CHARACTERISTIC = UUID.fromString("781ea64d-950f-488a-9682-e81d2a279e47")
    val WRITE_DESCRIPTOR = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")


}