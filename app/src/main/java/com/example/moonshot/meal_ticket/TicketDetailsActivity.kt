package com.example.moonshot.meal_ticket

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import com.example.moonshot.R
import com.example.moonshot.data.ApiService
import com.example.moonshot.manager.BLEManager
import com.example.moonshot.utils.MoonshotApplication
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_ticket.*
import kotlinx.android.synthetic.main.layout_loading.view.*
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException

class TicketDetailsActivity : AppCompatActivity() {

    val TAG = "TicketDetailsActivity"

    companion object {
        const val EXTRA_ID = "DEVICE_ID"
    }

    private val manager by lazy { MoonshotApplication.getBleManager(this) }

    private val service by lazy { ApiService.provideRetrofit().create(ApiService::class.java) }

    lateinit var _id: String

    private val disposable = CompositeDisposable()

    private lateinit var dialog: AlertDialog


    private val bluetoothManagerCallback by lazy {
        object : BLEManager.BluetoothManagerCallback() {
            override fun startLoading() {
                runOnUiThread {
                    showLoadingDialog()
                }
            }

            override fun stopLoading() {
                runOnUiThread {
                    dismissLoadingDialog()
                }
            }

            override fun onConnectionDisconnected(device: BluetoothDevice) {
                Toast.makeText(this@TicketDetailsActivity, "Disconnected from ${device.name}", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun scannerImage(img: Int) {
                runOnUiThread {
                    imgConfirm.setImageResource(img)
                }
            }

            override fun toastScannerMessage(message: String) {
                runOnUiThread {
                    txtVerifyUpdate.text = message
                }
            }

            override fun cardScanCompleted(uuid: String) {

                Log.i(TAG, "UUID>> $uuid")

                disposable.add(
                    service.verifyFingerPrint(uuid)
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn {
                            it.printStackTrace()
                            when (it) {

                                is HttpException -> {
                                    val message = JSONObject(it.response().errorBody()?.string()).getString("message")

                                    Log.i(TAG, "Message from the server ===== $message")

                                    ApiService.VerifyResponse(
                                        success = false,
                                        message = "HttpException"
                                    )
                                }
                                is ConnectException -> {

                                    ApiService.VerifyResponse(
                                        success = false,
                                        message = "No internet connection"
                                    )
                                }
                                else -> {
                                    it.printStackTrace()

                                    ApiService.VerifyResponse(
                                        success = false,
                                        message = "Unknown error"
                                    )
                                }
                            }

                        }
                        .doOnSuccess {
                            if (!it.success) {
                                //Handle error here
                                txtVerifyUpdate.text = it.message
                            } else {
                                manager.writeToSensor(false)

                                val item = it.data
                                it.data?.UUID

                                _id = item!!._id
                                val template = item.fingerprintTemplate


                                val uuId = item.UUID
                                val cAt = item.createdAt
                                val uAt = item.updatedAt
                                val v = item.__v

                                manager.verifySensor(template, Handler())

                                txtVerifyUpdate.text = it.message
                                Log.i(TAG, "biometricId:: $_id")
                            }
                        }.subscribe()
                )
            }

            override fun sendBioID() {

                val data = hashMapOf(
                    "biometricId" to _id,
                    "action" to "MEAL"
                )

                disposable.add(
                    service.sendBiometricId(data = data)
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn {
                            it.printStackTrace()
                            when (it) {
                                is HttpException -> {
                                    val message = JSONObject(it.response().errorBody()?.string()).getString("message")

                                    Log.i(TAG, "Message from the server ===== $message")

                                    ApiService.RecordResponse(
                                        success = false,
                                        message = message
                                    )
                                }
                                is ConnectException -> {
                                    ApiService.RecordResponse(
                                        success = false,
                                        message = "No internet connection"
                                    )
                                }
                                else -> {

                                    ApiService.RecordResponse(
                                        success = false,
                                        message = "Unknown error"
                                    )

                                }
                            }
                        }
                        .doOnSuccess {
                            if (!it.success) {
                                txtVerifyUpdate.text = it.message
                            } else {
                                manager.writeToSensor(false)
                                txtVerifyUpdate.text = it.message
                            }
                        }
                        .subscribe()
                )

            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)
        manager.managerCallback = bluetoothManagerCallback
        Log.i(TAG, "onCreate called")

        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_ID)
        val dName = device.name

        supportActionBar?.title = dName

        verifyBtn.setOnClickListener {
            manager.writeToService(bytesToBeWritten = "READ_CARD".toByteArray(Charsets.UTF_8))
        }
        createDialog()
    }

    private fun createDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.layout_loading, null)
        builder.setView(view)
        builder.setCancelable(false)

        view.txtMessage.text = "Please wait..."

        dialog = builder.create()
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun showLoadingDialog() {
        dialog.show()
    }

    fun dismissLoadingDialog() {
        if (dialog.isShowing) dialog.dismiss()
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
        disposable.clear()
        manager.disconnectGattServer()
        Log.i(TAG, "onDestroy called")
    }

    override fun onBackPressed() {
        manager.disconnectGattServer()
        finish()
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