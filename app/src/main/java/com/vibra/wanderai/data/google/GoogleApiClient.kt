package com.vibra.wanderai.data.google

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class GoogleApiException(val httpCode: Int, message: String) : IOException(message)

/**
 * HTTP client for Google Maps Platform web services (Routes, Places New).
 *
 * The project's MAPS_API_KEY is restricted to Android apps, so every request carries
 * `X-Android-Package` and `X-Android-Cert` (SHA-1 of the APK signing certificate, read
 * at runtime). Debug and release builds therefore work as long as both SHA-1s are
 * registered for the key in Google Cloud.
 */
class GoogleApiClient(
    context: Context,
    private val apiKey: String,
) {
    private val appContext = context.applicationContext
    private val packageName: String = appContext.packageName
    private val certificateSha1: String? by lazy { signingCertificateSha1() }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** POSTs [body] (JSON) to [url] asking only for [fieldMask]; returns the response body. */
    suspend fun post(url: String, fieldMask: String, body: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("X-Goog-Api-Key", apiKey)
            setRequestProperty("X-Goog-FieldMask", fieldMask)
            setRequestProperty("X-Android-Package", packageName)
            certificateSha1?.let { setRequestProperty("X-Android-Cert", it) }
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code from $url: ${text.take(300)}")
                throw GoogleApiException(code, "HTTP $code")
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun signingCertificateSha1(): String? = runCatching {
        val pm = appContext.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val certificate = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
        MessageDigest.getInstance("SHA-1").digest(certificate).joinToString("") { "%02X".format(it) }
    }.onFailure { Log.w(TAG, "Could not read the signing certificate", it) }.getOrNull()

    private companion object {
        const val TAG = "GoogleApi"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
    }
}
