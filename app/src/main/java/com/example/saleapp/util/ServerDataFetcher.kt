package com.example.saleapp.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.Socket
import java.io.OutputStream
import java.io.InputStream

class ServerDataFetcher {
    companion object {
        private const val TAG = "ServerDataFetcher"
        private const val SERVER_IP = "192.168.1.38" // Local network IP
        private const val SERVER_PORT = 5000 // Java server port
    }

    // Payment type constants
    object PaymentType {
        const val CREDIT = "Credit"
        const val QR = "QR"
        const val CASH = "Cash"
    }

    suspend fun fetchDataFromServer(
        requestData: ByteArray,
        onSuccess: (response: String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket(SERVER_IP, SERVER_PORT)
                Log.d(TAG, "Socket connected to $SERVER_IP:$SERVER_PORT")

                val outputStream: OutputStream = socket.getOutputStream()
                val dataWithNewline = requestData.plus('\n'.code.toByte())
                outputStream.write(dataWithNewline)
                outputStream.flush()
                Log.d(TAG, "Data sent to server: ${String(requestData)}")

                val inputStream: InputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                inputStream.read(buffer)

                val rawResponse = String(buffer).trim()
                Log.d(TAG, "Raw server response: $rawResponse")
                
                withContext(Dispatchers.Main) {
                    onSuccess(rawResponse)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in server communication: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError("Connection error: ${e.message}")
                }
            } finally {
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing socket: ${e.message}")
                }
            }
        }
    }

    suspend fun sendPaymentData(
        amount: String,
        paymentType: String,
        onSuccess: (response: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val paymentData = """
            {
                "PaymentType": "$paymentType",
                "Amount": "$amount"
            }
        """.trimIndent()

        fetchDataFromServer(
            requestData = paymentData.toByteArray(),
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
