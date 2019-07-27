package com.example.moonshot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Html
import android.util.Log
import android.widget.Toast
import com.example.moonshot.confirm_attendance.ConfirmActivity
import com.example.moonshot.enroll.EnrollActivity
import com.example.moonshot.meal_ticket.TicketActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i(TAG, "onCreate called")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val moon = "Moon"
        val shot = "Shot"

        val sourceString = "<b>$moon</b>$shot"
        toolbar_title.text = Html.fromHtml(sourceString)

        mealTicketBtn.setOnClickListener {
            val intent = Intent(this, TicketActivity::class.java)
            startActivity(intent)
        }

        enrollBtn.setOnClickListener {
            val intent = Intent(this, EnrollActivity::class.java)
            startActivity(intent)
        }

        attendaceBtn.setOnClickListener{
            val intent = Intent(this, ConfirmActivity::class.java)
            startActivity(intent)
        }
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, getString(R.string.tap_back_to_exit), Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

}
