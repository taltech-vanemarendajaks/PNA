package com.example.callgrabber.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.callgrabber.R
import com.example.callgrabber.BuildConfig
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = "Settings"

        val serverAddressEditText = findViewById<EditText>(R.id.serverAddressEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        serverAddressEditText.setText(prefs.getString("server_url", BuildConfig.DEFAULT_SERVER_URL))

        saveButton.setOnClickListener {
            val newValue = serverAddressEditText.text.toString().trim()

            if (newValue.isBlank()) {
                Toast.makeText(this, "Please enter a server address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit { putString("server_ip", newValue) }

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}