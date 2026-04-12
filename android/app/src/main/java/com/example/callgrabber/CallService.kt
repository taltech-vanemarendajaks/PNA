package com.example.callgrabber

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import android.widget.Toast

class CallService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d("CALL_GRABBER", "Service triggered")

        val number = callDetails.handle?.schemeSpecificPart
        val extras = callDetails.extras

        Log.d("CALL_GRABBER", "Incoming call from: $number")
        Log.d("CALL_GRABBER", "Extras: $extras")

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                "Call: $number\nExtras: $extras",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}