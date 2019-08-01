package com.example.moonshot.confirm_attendance

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.moonshot.R
import com.example.moonshot.data.ApiService
import com.example.moonshot.enroll.EnrollDetailsActivity
import com.example.moonshot.manager.BLEManager
import com.example.moonshot.utils.MoonshotApplication
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_confirm.*
import kotlinx.android.synthetic.main.activity_enroll.*
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException

class ConfirmDetailsActivity : AppCompatActivity() {
    val TAG = "ConfirmDetailsActivity"

    companion object {
        const val EXTRA_ID = "DEVICE_ID"
    }


    private val manager by lazy { MoonshotApplication.getBleManager(this) }

    private val service by lazy { ApiService.provideRetrofit().create(ApiService::class.java) }

    private val disposable = CompositeDisposable()

    private val bluetoothManagerCallback by lazy {
        object : BLEManager.BluetoothManagerCallback() {
            override fun scannerImage(img: Int) {
                runOnUiThread {
                    imgConfirm.setImageResource(img)
                }
            }

            override fun toastScannerMessage(message: String) {
                runOnUiThread {
                    txtUpdate.text = message
                }
            }

            override fun cardScanCompleted(uuid: String) {

                Log.i(TAG, "UUID>> $uuid")

                disposable.add(
                    service.verifyFingerPrint(uuid)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnError {
                            when (it) {

                                is HttpException -> {
                                    val message = JSONObject(it.response().errorBody()?.string()).getString("message")

                                    Log.i(TAG, "Message from the server ===== $message")
                                    Toast.makeText(this@ConfirmDetailsActivity, message, Toast.LENGTH_LONG).show()
//                                val data = ApiService.VerifyResponse()
//                                ApiService.VerifyResponse(
//                                    false,
//                                    data = ,
//                                )
                                }
                                is ConnectException -> {
                                    Toast.makeText(
                                        this@ConfirmDetailsActivity,
                                        "No internet connection",
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
//                                ApiService.VerifyResponse(
//                                    false, "No internet connection", null
//                                )
                                }
                                else -> {
                                    it.printStackTrace()
                                    Toast.makeText(this@ConfirmDetailsActivity, "Unknown error", Toast.LENGTH_LONG)
                                        .show()
//                                ApiService.VerifyResponse(
//                                    false, "Unknown error", null
//                                )
                                }
                            }

                        }
                        .doOnSuccess {
                            if (!it.success) {

                            } else {


                                val item = it.data
                                it.data?.UUID

                                val _id = item?._id
                                val template = item?.fingerprintTemplate
                                val uuId = item?.UUID
                                val cAt = item?.createdAt
                                val uAt = item?.updatedAt
                                val v = item?.__v
                                manager.verifySensor(template!!)

                                runOnUiThread {
                                    txtUpdate.text = "Your UUID is :: " + it.data?.UUID
                                }
                                Log.i(TAG, _id)
                                Toast.makeText(this@ConfirmDetailsActivity, it.data?.UUID, Toast.LENGTH_SHORT).show()
                            }
                        }.subscribe()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)
        manager.managerCallback = bluetoothManagerCallback
        Log.i(TAG, "onCreate called")

        val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_ID)
        val dName = device.name

        supportActionBar?.title = dName

        confirmBtn.setOnClickListener {
            manager.enableVerify()
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
        disposable.clear()
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