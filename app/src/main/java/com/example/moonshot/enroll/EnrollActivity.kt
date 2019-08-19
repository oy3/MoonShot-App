package com.example.moonshot.enroll

import android.Manifest
import android.annotation.TargetApi
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.moonshot.BLEAdapter
import com.example.moonshot.R
import com.example.moonshot.enroll.EnrollDetailsActivity.Companion.EXTRA_ID
import com.example.moonshot.utils.BaseActivity
import com.example.moonshot.utils.BluetoothConstants
import com.example.moonshot.utils.MoonshotApplication
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.android.synthetic.main.toolbar.*

class EnrollActivity : BaseActivity(), BLEAdapter.OnDeviceClickListener {
    val TAG = "EnrollActivity"
    private lateinit var adapter: BLEAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var mBTAdapter: BluetoothAdapter
    private val list = arrayListOf<BluetoothDevice>()
    private val manager by lazy { MoonshotApplication.getBleManager(this) }
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    override fun onDeviceClicked(device: BluetoothDevice) {
        stopScan()
        manager.connectGattServer(device)
        Log.i(TAG, "Connecting to -> " + device.name + " :: " + device.address)
        loadingDialog(device)

        Thread(Runnable {
            try {
                Thread.sleep(2500)
                val intent = Intent(this, EnrollDetailsActivity::class.java)
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


        supportActionBar?.title = getString(R.string.enroll_finger_print)


        mSwipeRefreshLayout = findViewById(R.id.pullToRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        adapter = BLEAdapter(this, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val mBluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBTAdapter = mBluetoothManager.adapter

        btRequired()

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
                btRequired()
            } else {
                // The toggle is disabled
                btOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume called")

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No BLE Support.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        scanLeDevice(true)
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause called")
        if (mBTAdapter.startDiscovery()) {
            stopScan()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop called")
        if (mBTAdapter.startDiscovery()) {
            stopScan()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy called")
        if (mBTAdapter.startDiscovery()) {
            stopScan()
        }
    }

    private fun btRequired() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBTAdapter.isEnabled) {
            errorBt.visibility = View.GONE
            btSwitch.isChecked = true
        } else {
            errorBt.visibility = View.VISIBLE
            btSwitch.isChecked = false
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, BluetoothConstants.REQUEST_ENABLE_BT)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                finish()
            }
        }
    }

    private fun btOff() {
        mBTAdapter.disable() // turn off
        Toast.makeText(applicationContext, "Bluetooth turned Off", Toast.LENGTH_SHORT).show()
        btRequired()
    }

    private fun scanLeDevice(enable: Boolean) {
        Log.i(TAG, "Started scan")
        list.clear()
        adapter.notifyDataSetChanged()
        Handler().postDelayed({
            mSwipeRefreshLayout.isRefreshing = false
        }, 2000)
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                Handler().postDelayed({
                    stopScan()
                }, BluetoothConstants.SCAN_PERIOD)
                mBTAdapter.startLeScan(leScanCallback)
            }
            else -> {

                stopScan()
            }
        }
    }

    private fun stopScan() {
        mBTAdapter.stopLeScan(leScanCallback)
        Log.i(TAG, "Stopped scan")
    }

    private val leScanCallback: BluetoothAdapter.LeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            Log.i(TAG, "New BLE device:" + device.name + "@ $rssi")


            if (device.name != null) {
                if (!list.contains(device)) {
                    list.add(device)
                    adapter.bleList = list
                    adapter.notifyDataSetChanged()
                }

            }

        }


}