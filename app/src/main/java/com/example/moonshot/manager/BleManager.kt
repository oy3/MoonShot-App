package com.example.moonshot.manager

import android.bluetooth.*
import android.content.Context
import android.os.Vibrator
import android.util.Log
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_DESCRIPTOR
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_SERVICE
import com.example.moonshot.utils.BluetoothConstants.WRITE_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.WRITE_SERVICE
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class BLEManager(private val context: Context) {
    val TAG = "BLEManager"
    private var globalGatt: BluetoothGatt? = null
    lateinit var deviceConnected: BluetoothDevice
    private var characteristic: BluetoothGattCharacteristic? = null

    private var hasGottenCardUUID = false

    lateinit var managerCallback: BluetoothManagerCallback

    private val mainFolder: File? = null

    private var fingerprintBytes: String = ""

    private lateinit var uuid: String
    private val fingerPrintFile by lazy { File(context.cacheDir, uuid) }

    inner class BLEGattClientCallback(private val device: BluetoothDevice) : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.i(TAG, "Connection successful ====== ${newState == BluetoothProfile.STATE_CONNECTED}")

            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer()
                return

            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer()
                return

            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                deviceConnected = device
                Log.i(TAG, "Discovering services...")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from ${gatt?.device?.name ?: gatt?.device?.address}")
                disconnectGattServer()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        Log.i(
                            TAG,
                            "Service name: ${service.uuid}\tCharacteristic: ${characteristic.uuid}\t Descriptor: ${characteristic.descriptors.map { it.uuid.toString() + " " }}"
                        )
                    }
                }

            } else {
                Log.i(TAG, "Didn't connect to GATT services")
                Log.i(TAG, "No services found.$status")

            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (WRITE_CHARACTERISTIC == characteristic!!.uuid) {
                Log.i(TAG, "onCharacteristicRead :: " + characteristic.uuid + ", " + characteristic.value)
            }

        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val data = characteristic?.value
            val fingerPrintData = bytesToHex3(data!!)
            val stringValue = String(data, Charsets.UTF_8)

            Log.i(
                TAG,
                "Message from ${characteristic.uuid} is $data" + ", " + "FingerPrintData :: $fingerPrintData" + ", " + "StringValue :: $stringValue"
            )
            when {
                stringValue.contains("UUID") -> {
                    hasGottenCardUUID = true
                    uuid = stringValue.split("::")[1] //UUID::e2b371e3
                }
                stringValue.contains("PLACE CARD") -> managerCallback.toastScannerMessage(stringValue)
                stringValue.contains("PLACE FINGER") -> managerCallback.toastScannerMessage(stringValue)
                stringValue.contains("PROCESS COMPLETE") -> {
                    val vibratorService = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibratorService.vibrate(500)
                    managerCallback.toastScannerMessage(stringValue)
                    managerCallback.fingerPrintScanCompleted(fingerprintBytes, uuid)
                    writeToSensor(false)
                }
                stringValue.contains("ENROLMENT FAILURE") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }
                stringValue.contains("BAD FINGER") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }
                stringValue.contains("NFC FAILED") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }

                stringValue.contains("Finger is not presse") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }


                hasGottenCardUUID -> {
                    fingerprintBytes += fingerPrintData
                    Log.i(TAG, "Finger print path ====== ${fingerPrintFile.absoluteFile}")
                }
            }

            if (WRITE_CHARACTERISTIC == characteristic.uuid) {
                Log.i(TAG, "onCharacteristicChanged :: " + characteristic.uuid + ", " + characteristic.value)
            }

        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val data = characteristic?.value

            val fingerPrintData = bytesToHex3(data!!)
            val stringValue = String(data, Charsets.UTF_8)

            Log.i(
                TAG,
                "onCharacteristicWrite :: " + characteristic.uuid + ", " + data + ", " + "FingerPrintData :: $fingerPrintData" + ", " + "StringValue :: $stringValue"
            )

            globalGatt = gatt!!
            writeToSensor(true)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        }

    }

    private fun bytesToHex3(hashInBytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in hashInBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }


    fun connectGattServer(device: BluetoothDevice) {
        Log.i(TAG, "Trying to connect...")
        globalGatt = device.connectGatt(context, false, BLEGattClientCallback(device))
    }

    fun  disconnectGattServer() {
        Log.i(TAG, "Disconnected from globalGatt server")
        globalGatt?.let {
            it.disconnect()
            it.close()
        }
    }

    interface Enroll {
        fun writeSuccessful()
    }

    interface Verify {
        fun verificationSuccessful()
    }

    fun enableSensor() {

        val characteristic: BluetoothGattCharacteristic? =
            globalGatt?.getService(WRITE_SERVICE)?.getCharacteristic(WRITE_CHARACTERISTIC)
        val bytesToBeWritten = "ENROL".toByteArray()
        characteristic!!.value = bytesToBeWritten
        globalGatt?.writeCharacteristic(characteristic)
        Log.i(TAG, "Enabling service")
    }

    private fun readSensor(gatt: BluetoothGatt) {

        val characteristic: BluetoothGattCharacteristic? =
            gatt.getService(WRITE_SERVICE)
                .getCharacteristic(WRITE_CHARACTERISTIC)

        gatt.readCharacteristic(characteristic)
        Log.i(TAG, "Reading service")
    }

    fun writeToSensor(on: Boolean) {
        Log.i(TAG, "Writing service")
        hasGottenCardUUID = !on
        fingerprintBytes = ""

        val characteristic: BluetoothGattCharacteristic? =
            globalGatt!!.getService(NOTIFY_SERVICE)
                .getCharacteristic(NOTIFY_CHARACTERISTIC)

        globalGatt!!.setCharacteristicNotification(characteristic, on)
        val descriptor =
            characteristic?.getDescriptor(NOTIFY_DESCRIPTOR)
        descriptor?.value =
            if (on) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        globalGatt!!.writeDescriptor(descriptor)

    }

    fun writeToFingerPrintFile(file: File, bytes: ByteArray) {
        val outputStream = FileOutputStream(file, true)
        try {
            outputStream.write(bytes)
        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
        } finally {
            outputStream.close()
        }
    }

    abstract class BluetoothManagerCallback {
        abstract fun toastScannerMessage(message: String)
        open fun writeFile(data: ByteArray?) {}
        open fun fingerPrintScanCompleted(hexData: String, uuid: String) {}
        open fun fingerPrintScanFailed(errorMessage: String) {}
        open fun onConnected(device: BluetoothDevice) {}
        open fun onConnectionDisconnected() {}
    }

}