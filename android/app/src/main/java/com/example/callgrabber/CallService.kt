package com.example.callgrabber

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.widget.Toast
import com.example.callgrabber.auth.AuthStorage
import com.example.callgrabber.helpers.toCallerTitle
import com.example.callgrabber.helpers.toDisplayText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d("CALL_GRABBER", "Service triggered")

        val phoneNumber = callDetails.handle?.schemeSpecificPart

        if (!phoneNumber.isNullOrEmpty()) {
            sendToServer(phoneNumber)
        }

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()
        respondToCall(callDetails, response)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                "Call: $phoneNumber",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun sendToServer(number: String) {
        serviceScope.launch {
            try {
                val token = AuthStorage.getToken(this@CallService)

                if (token.isNullOrBlank()) {
                    Log.e("CALL_GRABBER", "No Google token found. User needs to log in.")
                    showToast("No login token found")
                    return@launch
                }

                val api = ApiProvider.getCallApiService(this@CallService)
                val response = api.searchNumber(SearchNumberRequest(number = number))

                if (response.isSuccessful) {
                    val body = response.body()
                    val message = body?.result?.toDisplayText(number) ?: "Unknown caller: $number"
                    Log.d("CALL_GRABBER", "Server responded: $message")
                    val title = body?.result?.toCallerTitle(number) ?: "Unknown caller: $number"
                    showToast(title)
                } else {
                    val errorMessage = "Server error: ${response.code()}"
                    Log.e(
                        "CALL_GRABBER",
                        "$errorMessage ${response.errorBody()?.string()}"
                    )
                    showToast(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("CALL_GRABBER", "Network connection failed", e)
                showToast("Network connection failed")
            }
        }

    }
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroy() {
        Log.d("CALL_GRABBER", "Service destroyed")

        //TODO: Kills the service the moment it is launched. Disabled for now. Fix if time.
        //serviceScope.cancel()
        //super.onDestroy()
    }
}