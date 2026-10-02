package com.example

import android.app.Application
import android.util.Log
import com.example.data.remote.supabase.SupabaseSessionStore

class OcaVenteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseSessionStore.initialize(this)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("OcaVenteCrashHandler", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.i("OcaVenteApplication", "Supabase admin session initialized")
    }
}
