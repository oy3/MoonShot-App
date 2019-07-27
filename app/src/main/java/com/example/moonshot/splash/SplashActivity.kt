package com.example.moonshot.splash

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.moonshot.MainActivity
import com.example.moonshot.R
import java.lang.Exception

class SplashActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val background = object : Thread() {
            override fun run() {
                try {
                    sleep(5000)

                    val intent = Intent(baseContext, MainActivity::class.java)
                    startActivity(intent)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        background.start()
    }
}