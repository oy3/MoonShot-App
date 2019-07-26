package com.example.moonshot

import android.app.Application
import android.content.Context

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