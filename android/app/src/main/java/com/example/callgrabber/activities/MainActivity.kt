package com.example.callgrabber.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.callgrabber.AuthStorage
import com.example.callgrabber.BuildConfig
import com.example.callgrabber.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            ),
            1
        )

        val enableButton = findViewById<Button>(R.id.enableButton)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        updateLoginUi()
        updateStatus(statusText)

        enableButton.setOnClickListener {
            openCallerIdSettings()
            requestBatteryOptimizationDisable()
        }

        loginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        updateStatus(statusText)
        updateLoginUi()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateStatus(statusText: TextView) {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val active = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        statusText.text = if (active) "Status: ACTIVE" else "Status: INACTIVE"
    }

    private fun openCallerIdSettings() {
        try {
            val intent = Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationDisable() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = "package:$packageName".toUri()
        startActivity(intent)
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                val authorizedOption = GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(true)
                    .build()

                val authorizedRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(authorizedOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = authorizedRequest
                )

                handleGoogleCredential(result.credential)

            } catch (_: NoCredentialException) {
                try {
                    val anyAccountOption = GetGoogleIdOption.Builder()
                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()

                    val anyAccountRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(anyAccountOption)
                        .build()

                    val result = credentialManager.getCredential(
                        context = this@MainActivity,
                        request = anyAccountRequest
                    )

                    handleGoogleCredential(result.credential)

                } catch (e2: GetCredentialException) {
                    Log.e("CALL_GRABBER", "Google sign-in fallback failed", e2)
                } catch (e2: Exception) {
                    Log.e("CALL_GRABBER", "Unexpected fallback sign-in error", e2)
                }
            } catch (e: GetCredentialException) {
                Log.e("CALL_GRABBER", "Credential error", e)
            } catch (e: Exception) {
                Log.e("CALL_GRABBER", "Unexpected sign-in error", e)
            }
        }
    }

    private fun handleGoogleCredential(credential: Credential) {
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                        val idToken = googleIdTokenCredential.idToken
                        val displayName = googleIdTokenCredential.displayName

                        AuthStorage.saveLogin(this, idToken, displayName, email)
                        updateLoginUi()

                        Log.d("CALL_GRABBER", "Google sign-in success")
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("CALL_GRABBER", "Invalid Google ID token response", e)
                    }
                } else {
                    Log.e("CALL_GRABBER", "Unexpected custom credential type: ${credential.type}")
                }
            }

            else -> {
                Log.e("CALL_GRABBER", "Unexpected credential type")
            }
        }
    }

    private fun updateLoginUi() {
        val token = AuthStorage.getToken(this)
        val userName = AuthStorage.getUserName(this)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val userText = findViewById<TextView>(R.id.userText)

        val isLoggedIn = !token.isNullOrBlank()

        loginButton.visibility = if (isLoggedIn) View.GONE else View.VISIBLE

        userText.text = if (isLoggedIn) {
            when {
                !userName.isNullOrBlank() ->
                    "Logged in as: $userName"
                else ->
                    "Logged in as: Unknown user"
            }
        } else {
            "Not logged in"
        }
    }
}