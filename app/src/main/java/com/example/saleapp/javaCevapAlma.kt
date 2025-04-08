import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

fun sendRequest(callback: (result: String) -> Unit) {
    val client = OkHttpClient()
    val url = "http://192.168.1.38:5000"
    val json = """{"PaymentType":"Credit"}"""
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = RequestBody.create(mediaType, json)

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .header("Connection", "close")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("OkHttp", "İstek başarısız: ${e.message}")
            callback("Exception: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val result = if (response.isSuccessful) {
                response.body?.string() ?: "Empty response"
            } else {
                "Error: ${response.code} - ${response.message}"
            }
            Log.d("OkHttp", "Sunucudan gelen yanıt: $result")
            callback(result)
        }
    })
}
