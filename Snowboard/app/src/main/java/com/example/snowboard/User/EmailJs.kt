package com.example.snowboard.User

import com.example.snowboard.BuildConfig
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends an email from one of the EmailJS templates (IDs in local.properties, see app/build.gradle).
 * We have no backend on the Spark plan, so this is how the app emails anyone.
 */
object EmailJs {

    private const val SEND_URL = "https://api.emailjs.com/api/v1.0/email/send"
    private const val TIMEOUT_MS = 15_000

    /** Blocking network call: run it off the main thread. [params] fill the template's {{placeholders}}. */
    fun send(templateId: String, params: Map<String, String>) {
        if (BuildConfig.EMAILJS_SERVICE_ID.isEmpty() || templateId.isEmpty()) {
            throw IOException("EmailJS is not configured, see EMAILJS_* in app/build.gradle")
        }
        val body = JSONObject()
            .put("service_id", BuildConfig.EMAILJS_SERVICE_ID)
            .put("template_id", templateId)
            .put("user_id", BuildConfig.EMAILJS_PUBLIC_KEY)
            .put("template_params", JSONObject(params))

        val connection = URL(SEND_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                val reason = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("EmailJS returned $status: $reason")
            }
        } finally {
            connection.disconnect()
        }
    }
}
