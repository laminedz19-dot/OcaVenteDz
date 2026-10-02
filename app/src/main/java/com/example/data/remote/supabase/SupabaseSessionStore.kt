package com.example.data.remote.supabase

import android.content.Context

object SupabaseSessionStore {
    private const val PREFS = "supabase_session"
    private var context: Context? = null
    fun initialize(value: Context) { context = value.applicationContext }
    fun accessToken(): String? = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString("access_token", null)
    fun refreshToken(): String? = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString("refresh_token", null)
    fun save(access: String, refresh: String?) { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString("access_token", access)?.putString("refresh_token", refresh ?: "")?.apply() }
    fun clear() { context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply() }
}
