package com.example.moonshot.meal_ticket

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.example.moonshot.R
import com.example.moonshot.data.ApiService
import com.example.moonshot.utils.MoonshotApplication
import io.reactivex.disposables.CompositeDisposable

class TicketDetailsActivity : AppCompatActivity() {

    val TAG = "TicketDetailsActivity"

    companion object {
        const val EXTRA_ID = "DEVICE_ID"
    }

    private val manager by lazy { MoonshotApplication.getBleManager(this) }

    private val service by lazy { ApiService.provideRetrofit().create(ApiService::class.java) }

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)
        Log.i(TAG, "onCreate called")
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