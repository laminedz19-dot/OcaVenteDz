package com.example

import android.app.Application
import android.util.Log
import com.parse.Parse

class OcaVenteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("OcaVenteCrashHandler", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        try {
            Parse.initialize(
                Parse.Configuration.Builder(this)
                    .applicationId(BuildConfig.BACK4APP_APPLICATION_ID)
                    .clientKey(BuildConfig.BACK4APP_CLIENT_KEY)
                    .server(BuildConfig.BACK4APP_SERVER_URL)
                    .build()
            )
            Log.i("OcaVenteApplication", "Back4App Parse initialized")
        } catch (e: Exception) {
            Log.e("OcaVenteApplication", "Back4App Parse initialization failed", e)
        }
    }
}
