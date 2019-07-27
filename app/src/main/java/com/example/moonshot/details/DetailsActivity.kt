package com.example.moonshot.details

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.moonshot.ApiService
import com.example.moonshot.BLEManager
import com.example.moonshot.MoonshotApplication
import com.example.moonshot.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException

class DetailsActivity : AppCompatActivity() {

    val TAG = "DetailsActivity"
    private val manager by lazy { MoonshotApplication.getBleManager(this) }

    private val service by lazy { ApiService.provideRetrofit().create(ApiService::class.java) }

    private val disposable = CompositeDisposable()

    private val bluetoothManagerCallback by lazy {
        object : BLEManager.BluetoothManagerCallback() {
            override fun toastScannerMessage(message: String) {
                runOnUiThread {
                    updateTxt.text = message
                }
            }

            override fun fingerPrintScanCompleted(hexData: String, uuid: String) {
//                val fingerprintRequest = RequestBody.create(MediaType.parse("*/*"), file)
//                val fingerprintUUID = RequestBody.create(MediaType.parse("text/plain"), uuid)

                val data = hashMapOf(
                    "fingerprintTemplate" to hexData,
                    "UUID" to uuid
                )

                disposable.add(
                    service.uploadFingerprint(
                        data = data
                    ).observeOn(AndroidSchedulers.mainThread()).onErrorReturn {
                        when (it) {
                            is HttpException -> {
                                val message = JSONObject(it.response().errorBody()?.string()).getString("message")

                                Log.i(TAG, "Message from the server ===== $message")

                                ApiService.Response(
                                    false,
                                    message,
                                    null
                                )
                            }
                            is ConnectException -> {
                                ApiService.Response (
                                    false, "No internet connection", null
                                )
                            }
                            else -> {
                                it.printStackTrace()
                                ApiService.Response (
                                    false, "Unknown error", null
                                )
                            }
                        }

                    }.doOnSuccess {
                        if (!it.success) {

                        } else {
                            manager.writeToSensor(false)
                        }
                        runOnUiThread {
                            updateTxt.text = it.message
                        }
                        Toast.makeText(this@DetailsActivity, it.message, Toast.LENGTH_SHORT).show()
                    }.subscribe()
                )
            }
        }
    }

    companion object {
        const val EXTRA_ID = "DEVICE_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        manager.managerCallback = bluetoothManagerCallback
        Log.i(TAG, "onCreate called")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        moonTxt.text = ""
        shotTxt.text = getString(R.string.meal_ticket)


        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_ID)

        deviceStatus.text = "Connected"
        deviceName.text = device.name
        deviceAdd.text = device.address

        enrolBtn.setOnClickListener {
            manager.enableSensor()
        }

    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy called")
//        if (manager.isConnected()) manager.disconnectGattServer()
        manager.disconnectGattServer()
        super.onDestroy()
    }

    override fun onBackPressed() {
        manager.disconnectGattServer()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_disconnect -> {
//                disconnectDialog()
//                updateTxt.text = ""
                manager.disconnectGattServer()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun disconnectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Do you want to disconnect from service?")
        builder.setPositiveButton("Yes") { dialog, id ->
            manager.writeToSensor(false)
            Toast.makeText(this@DetailsActivity, "Disconnected from service", Toast.LENGTH_SHORT).show()
        }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }.create().show()

    }

}