package com.example.moonshot.enroll

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.moonshot.R
import com.example.moonshot.data.ApiService
import com.example.moonshot.manager.BLEManager
import com.example.moonshot.utils.MoonshotApplication
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_confirm.*
import kotlinx.android.synthetic.main.activity_enroll.*
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException

class EnrollDetailsActivity : AppCompatActivity() {

    val TAG = "EnrollDetailsActivity"

    companion object {
        const val EXTRA_ID = "DEVICE_ID"
    }

    private val manager by lazy { MoonshotApplication.getBleManager(this) }

    private val service by lazy { ApiService.provideRetrofit().create(ApiService::class.java) }

    private val disposable = CompositeDisposable()

    private val bluetoothManagerCallback by lazy {
        object : BLEManager.BluetoothManagerCallback() {
            override fun onConnectionDisconnected(device: BluetoothDevice) {
                Toast.makeText(this@EnrollDetailsActivity, "Disconnected from ${device.name}", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun scannerImage(img: Int) {
                runOnUiThread {
                    imgEnrollImg.setImageResource(img)
                }
            }

            override fun toastScannerMessage(message: String) {
                runOnUiThread {
                    txtEnrollUpdate.text = message
                }
            }

            override fun fingerPrintScanCompleted(hexData: String, uuid: String) {

                val data = hashMapOf(
                    "fingerprintTemplate" to hexData,
                    "UUID" to uuid
                )

                disposable.add(
                    service.uploadFingerprint(
                        data = data
                    ).observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn {
                            when (it) {
                                is HttpException -> {
                                    val message = JSONObject(it.response().errorBody()?.string()).getString("message")

                                    Log.i(TAG, "Message from the server ===== $message")

                                    ApiService.Response(
                                        success = false,
                                        message = message,
                                        error = null
                                    )
                                }
                                is ConnectException -> {
                                    ApiService.Response(
                                        false, "No internet connection", null
                                    )
                                }
                                else -> {
                                    it.printStackTrace()
                                    ApiService.Response(
                                        false, "Unknown error", null
                                    )
                                }
                            }

                        }
                        .doOnSuccess {
                            if (!it.success) {
                                txtEnrollUpdate.text = it.message
                            } else {
                                manager.writeToSensor(false)
                                txtEnrollUpdate.text = it.message
                            }

                        }.subscribe()
                )
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enroll)
        manager.managerCallback = bluetoothManagerCallback
        Log.i(TAG, "onCreate called")

        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_ID)
        val dName = device.name

        supportActionBar?.title = dName

        enrollBtn.setOnClickListener {
            manager.enableSensor()
        }

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.disconnectGattServer()
        Log.i(TAG, "onDestroy called")
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
                manager.disconnectGattServer()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}