package com.example.moonshot.utils

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import com.example.moonshot.BLEAdapter
import com.example.moonshot.R
import com.github.ybq.android.spinkit.style.WanderingCubes
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.android.synthetic.main.layout_loading.view.*

open class BaseActivity : AppCompatActivity() {
    lateinit var dialog: AlertDialog
    private val list = arrayListOf<BluetoothDevice>()
     lateinit var adapter: BLEAdapter
     lateinit var mBTAdapter: BluetoothAdapter
     lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    val TAG = "BaseActivity"

    fun loadingDialog(view: View, text: String) {
        val builder = AlertDialog.Builder(this)
        builder.setView(view)
        builder.setCancelable(false)

        val wanderingCubes = WanderingCubes()

        view.txtMessage.text = text
        view.pBar.setIndeterminateDrawable(wanderingCubes)

        dialog = builder.create()
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun checkMarshMallow() {
        val permissionsNeeded = ArrayList<String>()

        val permissionsList = ArrayList<String>()
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION))
            permissionsNeeded.add("Show Location")

        if (permissionsList.size > 0) {
            if (permissionsNeeded.size > 0) {

                // Need Rationale
                var message = "App need access to " + permissionsNeeded[0]

                for (i in 1 until permissionsNeeded.size)
                    message = message + ", " + permissionsNeeded[i]

                showMessageOKCancel(message,
                    DialogInterface.OnClickListener { dialog, which ->
                        BluetoothConstants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                        requestPermissions(
                            permissionsList.toTypedArray(),
                            BluetoothConstants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                        )
                    })
                return
            }
            requestPermissions(
                permissionsList.toTypedArray(),
                BluetoothConstants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
            )
            return
        }

        Toast.makeText(
            this,
            "No new Permission Required-Launching App.",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }


    @TargetApi(Build.VERSION_CODES.M)
    private fun addPermission(permissionsList: MutableList<String>, permission: String): Boolean {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission)
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            BluetoothConstants.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {
                val perms = HashMap<String, Int>()
                // Initial
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] = PackageManager.PERMISSION_GRANTED


                // Fill with results
                for (i in permissions.indices)
                    perms[permissions[i]] = grantResults[i]

                // Check for ACCESS_FINE_LOCATION
                if (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted

                    // Permission Denied
                    Toast.makeText(this, "All Permission GRANTED !! Thank You :)", Toast.LENGTH_SHORT)
                        .show()


                } else {
                    // Permission Denied
                    Toast.makeText(
                        this,
                        "One or More Permissions are DENIED Exiting App :(",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun createDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.layout_error, null)
        builder.setView(view)
        builder.setCancelable(false)

        dialog = builder.create()
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun showLoadingDialog() {
        dialog.show()
    }

    private fun dismissLoadingDialog() {
        if (dialog.isShowing) dialog.dismiss()
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

    fun stopScan() {
        mBTAdapter.stopLeScan(leScanCallback)
        Log.i(TAG, "Stopped scan")
    }

    fun scanLeDevice(enable: Boolean) {
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

    fun btOn() {
        mBTAdapter.enable() // turn on
        btSwitch.isChecked = true
        Toast.makeText(applicationContext, "Bluetooth turned On", Toast.LENGTH_SHORT).show()
    }

    fun btOff() {
        stopScan()
        mBTAdapter.disable() // turn off
        Toast.makeText(applicationContext, "Bluetooth turned Off", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        when (requestCode) {

            BluetoothConstants.REQUEST_ENABLE_BT -> {
                Log.d(TAG, resultCode.toString())

                if (resultCode == 0) {
                    Toast.makeText(applicationContext, "You need to turn on Bluetooth to use this application", Toast.LENGTH_SHORT).show()
                    finish()
                }

            }
        }


    }

}