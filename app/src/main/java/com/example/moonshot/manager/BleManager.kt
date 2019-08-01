package com.example.moonshot.manager

import android.bluetooth.*
import android.content.Context
import android.media.Image
import android.os.Vibrator
import android.util.Log
import android.widget.ImageView
import com.example.moonshot.R
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_DESCRIPTOR
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_SERVICE
import com.example.moonshot.utils.BluetoothConstants.WRITE_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.WRITE_SERVICE
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset


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

                    managerCallback.cardScanCompleted(uuid)
                    Log.i(TAG, "For verify :: $uuid")

                }
                stringValue.contains("PLACE CARD") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    val image = R.drawable.card
                    managerCallback.scannerImage(image)
                }
                stringValue.contains("PLACE FINGER") -> {
                    managerCallback.toastScannerMessage(stringValue)
                    val image = R.drawable.fingerprint

                    managerCallback.scannerImage(image)
                }
                stringValue.contains("PROCESS COMPLETE") -> {

                    val vibratorService = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibratorService.vibrate(500)
                    managerCallback.toastScannerMessage(stringValue)
                    managerCallback.fingerPrintScanCompleted(fingerprintBytes, uuid)
                    val image = R.drawable.done
                    managerCallback.scannerImage(image)
                    writeToSensor(false)
                }
                stringValue.contains("ENROLMENT FAILURE") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }
                stringValue.contains("BAD FINGER") -> {
                    val image = R.drawable.error_finger
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }
                stringValue.contains("NFC FAILED") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }

                stringValue.contains("Finger is not presse") -> {
                    val image = R.drawable.error_finger
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage(stringValue)
                    writeToSensor(false)
                }

                stringValue.contains("COULD NOT READ CARD") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
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

    fun disconnectGattServer() {
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

    fun writeToSensor(on: Boolean) {
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
        Log.i(TAG, "Writing service")

    }

    private fun readSensor(gatt: BluetoothGatt) {

        val characteristic: BluetoothGattCharacteristic? =
            gatt.getService(WRITE_SERVICE)
                .getCharacteristic(WRITE_CHARACTERISTIC)

        gatt.readCharacteristic(characteristic)
        Log.i(TAG, "Reading service")
    }

    fun enableVerify() {

        val characteristic: BluetoothGattCharacteristic? =
            globalGatt?.getService(WRITE_SERVICE)?.getCharacteristic(WRITE_CHARACTERISTIC)
        val bytesToBeWritten = "READ_CARD".toByteArray()
        characteristic!!.value = bytesToBeWritten
        globalGatt?.writeCharacteristic(characteristic)
        Log.i(TAG, "Enabling Verify service")
    }

    fun verifySensor(template: String) {

        var previousValue = 0

        val iterationTimes = template.length / 20
        for (value in 1 until iterationTimes) {
            val templateByte = if (value == iterationTimes) {
                template.substring(previousValue until template.length - 1).toByteArray(Charsets.UTF_8)
            } else {
                template.substring(previousValue until (20 * value)).toByteArray(Charsets.UTF_8)
            }
            //Write the bytes to the sensor here
            val characteristic: BluetoothGattCharacteristic? =
                globalGatt?.getService(WRITE_SERVICE)?.getCharacteristic(WRITE_CHARACTERISTIC)
            characteristic!!.value = templateByte
            globalGatt?.writeCharacteristic(characteristic)

            previousValue += 20
        }
        Log.i(TAG, "Verify sensor")

    }

    abstract class BluetoothManagerCallback {
        abstract fun toastScannerMessage(message: String)
        abstract fun scannerImage(img: Int)
        open fun writeFile(data: ByteArray?) {}
        open fun fingerPrintScanCompleted(hexData: String, uuid: String) {}
        open fun fingerPrintScanFailed(errorMessage: String) {}
        open fun onConnected(device: BluetoothDevice) {}
        open fun onConnectionDisconnected() {}
        open fun cardScanCompleted(uuid: String) {}
    }

}