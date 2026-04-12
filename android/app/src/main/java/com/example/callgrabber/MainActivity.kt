package com.example.callgrabber

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Request required permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                ),
            1
        )

        val enableButton = findViewById<Button>(R.id.enableButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        enableButton.setOnClickListener {
            openCallerIdSettings()
            requestBatteryOptimizationDisable()
        }

        updateStatus(statusText)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        updateStatus(statusText)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateStatus(statusText: TextView) {

        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val active = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

        statusText.text = if (active) {
            "Status: ACTIVE"
        } else {
            "Status: INACTIVE"
        }
    }

    private fun openCallerIdSettings() {
        try {
            // Opens "Caller ID & spam app" settings
            val intent = Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    fun requestBatteryOptimizationDisable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}