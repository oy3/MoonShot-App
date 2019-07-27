package com.example.moonshot.utils

import android.app.Application
import android.content.Context
import com.example.moonshot.manager.BLEManager

class MoonshotApplication :Application() {

    private val manager by lazy { BLEManager(this) }

    override fun onCreate() {
        super.onCreate()
        //MoonshotApplication.getBleManager(this)
    }

    companion object {
        @JvmStatic
        fun getBleManager(context: Context): BLEManager {
            return (context.applicationContext as MoonshotApplication).manager
        }
    }
}