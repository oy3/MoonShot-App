package com.example.moonshot.manager

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Vibrator
import android.support.annotation.UiThread
import android.util.Log
import com.example.moonshot.R
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_DESCRIPTOR
import com.example.moonshot.utils.BluetoothConstants.NOTIFY_SERVICE
import com.example.moonshot.utils.BluetoothConstants.WRITE_CHARACTERISTIC
import com.example.moonshot.utils.BluetoothConstants.WRITE_SERVICE
import okio.ByteString.Companion.decodeHex
import java.io.File


class BLEManager(private val context: Context) {
    val TAG = "BLEManager"

    private var globalGatt: BluetoothGatt? = null

    lateinit var deviceConnected: BluetoothDevice

    private var hasGottenCardUUID = false

    lateinit var managerCallback: BluetoothManagerCallback

    private var fingerprintBytes: String = ""

    private lateinit var uuid: String
    private val fingerPrintFile by lazy { File(context.cacheDir, uuid) }

    inner class BLEGattClientCallback(private val device: BluetoothDevice) : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.i(TAG, "Connection successful ====== ${newState == BluetoothProfile.STATE_CONNECTED}")

            if (status == BluetoothGatt.GATT_FAILURE) {
                managerCallback.onConnectionDisconnected()
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
                managerCallback.onConnectionDisconnected()
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

                stringValue.contains("#100C") -> {
                    val image = R.drawable.error_finger
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("BAD FINGER")
                    writeToSensor(false)
                }

                stringValue.contains("#100D") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("ENROLMENT FAILURE, TRY AGAIN")
                    writeToSensor(false)
                }

                stringValue.contains("#1012") -> {
                    val image = R.drawable.error_finger
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("FINGER IS NOT PRESSED")
                    writeToSensor(false)
                }

                stringValue.contains("#1001") -> {
                    val image = R.drawable.error_finger
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("CAPTURE TIMEOUT, TRY AGAIN")
                    writeToSensor(false)
                }

                stringValue.contains("#1007") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("VERIFICATION FAILED")
                    writeToSensor(false)
                }

                stringValue.contains("#1003") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("PRIOR BAD ENROLLMENT")
                    writeToSensor(false)
                }

                stringValue.contains("#1006") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("MODULE ERROR")
                    writeToSensor(false)
                }


                stringValue.contains("##100f") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("DEVICE ERROR")
                    writeToSensor(false)
                }

                stringValue.contains("##1011") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("Invalid parameter, try again")
                    writeToSensor(false)
                }

                stringValue.contains("VERIFY SUCCESS") -> {
                    val vibratorService = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibratorService.vibrate(500)
                    val image = R.drawable.done
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("VERIFY SUCCESS")
                    managerCallback.sendBioID()
                    writeToSensor(false)
                }

                stringValue.contains("ERROR VERIFY CHECK #") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("VERIFY ERROR")
                    writeToSensor(false)
                }

                stringValue.contains("Unknown error") -> {
                    val image = R.drawable.error
                    managerCallback.scannerImage(image)
                    managerCallback.toastScannerMessage("UNKNOWN ERROR, CHECK NETWORK CONNECTION")
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

    fun writeToService(bytesToBeWritten: ByteArray) {
        val characteristic: BluetoothGattCharacteristic? =
            globalGatt?.getService(WRITE_SERVICE)?.getCharacteristic(WRITE_CHARACTERISTIC)
        characteristic!!.value = bytesToBeWritten
        globalGatt?.writeCharacteristic(characteristic)
    }


    fun verifySensor(template: String, uiThread: Handler) {
        //Load progress bar
        uiThread.post {
            managerCallback.startLoading()


            val handlerThread = HandlerThread("MyClass.Handler")
            handlerThread.start()
            val backgroundHandler = Handler(handlerThread.looper)

            backgroundHandler.post {
                val raw = template.decodeHex().toByteArray()


                var index = 0
                while (index < raw.size) {
                    val chunkSize: IntRange = if (index + 19 > raw.lastIndex) {
                        IntRange(index, raw.lastIndex)
                    } else {
                        IntRange(index, index + 19)
                    }

                    val chunkList = raw.slice(chunkSize)

                    val chunk = chunkList.toByteArray()
                    Thread.sleep(350)
                    index += 20

                    writeToService(chunk)
                }
                Thread.sleep(350)
                writeToService("VERIFY".toByteArray())

                managerCallback.stopLoading()
            }
            //Stop progress bar
        }


    }

    abstract class BluetoothManagerCallback {
        abstract fun toastScannerMessage(message: String)
        abstract fun scannerImage(img: Int)
        open fun fingerPrintScanCompleted(hexData: String, uuid: String) {}
        open fun fingerPrintScanFailed(errorMessage: String) {}
        open fun onConnected(device: BluetoothDevice) {}
        open fun onConnectionDisconnected() {}
        open fun cardScanCompleted(uuid: String) {}
        open fun sendBioID() {}

        open fun startLoading() {}
        open fun stopLoading() {}
    }

}