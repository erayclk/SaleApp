package com.example.saleapp.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.io.InputStream
import java.io.OutputStream

suspend fun handleServerResponse(
    serverIp: String,
    serverPort: Int,
    requestData: ByteArray,
    onSuccess: (response: String, bytesRead: Int) -> Unit,
    onError: (Exception?) -> Unit
) {
    withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            // Socket bağlantısını kur
            socket = Socket(serverIp, serverPort)
            
            // Veriyi gönder
            val outputStream: OutputStream = socket.getOutputStream()
            outputStream.write(requestData)
            outputStream.flush()
            
            // Yanıtı oku
            val inputStream: InputStream = socket.getInputStream()
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)

            if (bytesRead > 0) {
                val rawResponse = String(buffer, 0, bytesRead)
                Log.d("ServerResponse", "Raw server response: $rawResponse (Bytes: $bytesRead)")

                // JSON formatını düzelt
                val formattedResponse = if (!rawResponse.trim().startsWith("{")) {
                    """{"ResponseCode":"${rawResponse.trim()}"}"""
                } else {
                    rawResponse
                }

                withContext(Dispatchers.Main) {
                    onSuccess(formattedResponse, bytesRead)
                }
            } else {
                Log.e("ServerResponse", "No data received from server")
                withContext(Dispatchers.Main) {
                    onError(null)
                }
            }
        } catch (e: Exception) {
            Log.e("ServerResponse", "Error in server communication: ${e.message}")
            withContext(Dispatchers.Main) {
                onError(e)
            }
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                Log.e("ServerResponse", "Error closing socket: ${e.message}")
            }
        }
    }
}
