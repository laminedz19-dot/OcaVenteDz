package com.example.data.remote.back4app

import android.content.Context
import android.util.Log
import com.example.ui.model.Listing
import com.example.ui.model.RechargeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Back4AppClient {
    private const val TAG = "Back4AppClient"

    /**
     * Test connection to Back4App Parse Server using the saved App ID and REST API Key.
     */
    suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val appId = Back4AppConfig.getAppId(context)
        val restKey = Back4AppConfig.getRestKey(context)
        val serverUrl = Back4AppConfig.getServerUrl(context)

        if (appId.isBlank() || restKey.isBlank()) {
            return@withContext Result.failure(Exception("لم يتم ضبط Application ID أو REST API Key بعد."))
        }

        try {
            val url = URL("$serverUrl/classes/Listing?limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("X-Parse-Application-Id", appId)
            conn.setRequestProperty("X-Parse-REST-API-Key", restKey)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Result.success("تم الاتصال بسحابة Back4App بنجاح! كود الاستجابة: $responseCode")
            } else {
                val errorStream = conn.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).readText()
                } else "فشل الاتصال: كود $responseCode"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing connection", e)
            Result.failure(Exception("تعذر الاتصال بالسيرفر: ${e.localizedMessage ?: "تأكد من اتصالك بالإنترنت"}"))
        }
    }

    /**
     * Sync a listing to Back4App cloud class 'Listing'.
     */
    suspend fun syncListing(context: Context, listing: Listing): Result<String> = withContext(Dispatchers.IO) {
        if (!Back4AppConfig.isConfigured(context)) {
            return@withContext Result.success("وضع عدم الاتصال: تم الحفظ محلياً (Back4App غير مهيأ)")
        }

        try {
            val appId = Back4AppConfig.getAppId(context)
            val restKey = Back4AppConfig.getRestKey(context)
            val serverUrl = Back4AppConfig.getServerUrl(context)

            val json = JSONObject().apply {
                put("localId", listing.id)
                put("title", listing.title)
                put("price", listing.price)
                put("isNegotiable", listing.isNegotiable)
                put("categoryId", listing.categoryId)
                put("categoryName", listing.categoryName)
                put("wilayaCode", listing.wilayaCode)
                put("wilayaName", listing.wilayaName)
                put("commune", listing.commune)
                put("description", listing.description)
                put("condition", listing.condition.name)
                put("sellerId", listing.sellerId)
                put("sellerName", listing.sellerName)
                put("sellerPhone", listing.sellerPhone)
                put("viewsCount", listing.viewsCount)
                put("status", listing.status.name)
            }

            val url = URL("$serverUrl/classes/Listing")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("X-Parse-Application-Id", appId)
            conn.setRequestProperty("X-Parse-REST-API-Key", restKey)
            conn.setRequestProperty("Content-Type", "application/json")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val obj = JSONObject(resp)
                val objectId = obj.optString("objectId", "")
                Result.success(objectId)
            } else {
                val err = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() } ?: "HTTP $code"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing listing", e)
            Result.failure(e)
        }
    }

    /**
     * Submit a recharge request to Back4App cloud class 'RechargeRequest'
     * with destination account 007999990008761821 Clé 94 and receipt proof.
     */
    suspend fun submitRechargeRequest(context: Context, req: RechargeRequest): Result<String> = withContext(Dispatchers.IO) {
        if (!Back4AppConfig.isConfigured(context)) {
            return@withContext Result.success("تم تسجيل طلب الشحن محلياً (سيتم رفعه عند ضبط Back4App)")
        }

        try {
            val appId = Back4AppConfig.getAppId(context)
            val restKey = Back4AppConfig.getRestKey(context)
            val serverUrl = Back4AppConfig.getServerUrl(context)

            val json = JSONObject().apply {
                put("localId", req.id)
                put("amount", req.amount)
                put("method", req.method)
                put("status", req.status)
                put("date", req.date)
                put("transactionRef", req.transactionRef)
                put("receiptImageUrl", req.receiptImageUrl)
                put("destinationAccount", req.destinationAccount)
                put("manualVerificationRequired", true)
                put("verifierAdmin", "راهم محمد لمين")
            }

            val url = URL("$serverUrl/classes/RechargeRequest")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("X-Parse-Application-Id", appId)
            conn.setRequestProperty("X-Parse-REST-API-Key", restKey)
            conn.setRequestProperty("Content-Type", "application/json")

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val obj = JSONObject(resp)
                Result.success(obj.optString("objectId", ""))
            } else {
                val err = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() } ?: "HTTP $code"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting recharge request", e)
            Result.failure(e)
        }
    }
}
