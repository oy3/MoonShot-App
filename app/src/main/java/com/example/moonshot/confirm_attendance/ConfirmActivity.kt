package com.example.moonshot.confirm_attendance

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.example.moonshot.BLEAdapter
import com.example.moonshot.R
import com.example.moonshot.confirm_attendance.ConfirmDetailsActivity.Companion.EXTRA_ID
import com.example.moonshot.utils.BaseActivity
import com.example.moonshot.utils.BluetoothConstants.REQUEST_ENABLE_BT
import com.example.moonshot.utils.MoonshotApplication
import kotlinx.android.synthetic.main.activity_device_list.*

class ConfirmActivity : BaseActivity(), BLEAdapter.OnDeviceClickListener {

    //    var TAG = "ConfirmActivity"
    private lateinit var recyclerView: RecyclerView
    private val manager by lazy { MoonshotApplication.getBleManager(this) }


    override fun onDeviceClicked(device: BluetoothDevice) {
        stopScan()
        manager.connectGattServer(device)
        Log.i(TAG, "Connecting to -> " + device.name + " :: " + device.address)

        val view = layoutInflater.inflate(R.layout.layout_loading, null)
        val text = "Connecting to " + device.name
        loadingDialog(view, text)

        Thread(Runnable {
            try {
                Thread.sleep(5000)

                val intent = Intent(this, ConfirmDetailsActivity::class.java)
                intent.putExtra(EXTRA_ID, device)
                startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            dialog.dismiss()
        }).start()


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        Log.i(TAG, "onCreate called")

        supportActionBar?.title = getString(R.string.confirm_friday_attendance)

        mSwipeRefreshLayout = findViewById(R.id.pullToRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = BLEAdapter(this, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val mBluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBTAdapter = mBluetoothManager.adapter

        mSwipeRefreshLayout.setOnRefreshListener {
            scanLeDevice(true)
        }

        if (Build.VERSION.SDK_INT >= 23) {
            // Marshmallow+ Permission APIs
            checkMarshMallow()
        }

        btSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The toggle is enabled
                btOn()
            } else {
                // The toggle is disabled
                btOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume called")

        mBTAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBTAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

        } else {
            btOn()
        }


        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE Support.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
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
        Log.i(TAG, "onDestroy called")
    }




}