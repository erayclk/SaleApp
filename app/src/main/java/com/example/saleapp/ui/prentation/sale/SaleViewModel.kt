package com.example.saleapp.ui.prentation.sale

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleapp.model.Product
import com.example.saleapp.model.Transaction
import com.example.saleapp.room.TransactionDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import org.json.JSONObject

class SaleViewModel (application: Application): AndroidViewModel(application){
    var productId by mutableStateOf("")
    var productName by mutableStateOf("")
    var price by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var vatRate by mutableStateOf("")

    // Payment Data için StateFlow ekleyin


    private val _paymentResponseType = MutableStateFlow("")
    val paymentResponseType: StateFlow<String> = _paymentResponseType

    private val _paymentResponseCode = MutableStateFlow(-1)
    val paymentResponseCode: StateFlow<Int> = _paymentResponseCode


    private val _paymentData = MutableStateFlow("")
    val paymentData: StateFlow<String> = _paymentData

    var paymentResponseRaw by mutableStateOf("")


    // Payment Type ve Amount için ayrı state'ler
    var paymentType by mutableStateOf("")
    var paymentAmount by mutableStateOf("")

    // Enable the Room database
    val transactionDao = TransactionDatabase.getDatabase(application).transactionDao()

    // Payment data state for the latest transaction
    private val _latestTransaction = MutableStateFlow<Transaction?>(null)
    val latestTransaction: StateFlow<Transaction?> = _latestTransaction

    // Method to save payment data to Room DB
    fun savePaymentResponseToDatabase(responseData: String) {
        viewModelScope.launch {
            try {
                // Parse the JSON response
                val jsonObject = JSONObject(responseData)
                
                // Extract values from JSON
                val productId = jsonObject.optInt("ProductId", -1)
                val productName = jsonObject.optString("ProductName", "")
                val paymentTypeStr = jsonObject.optString("PaymentType", "Unknown")
                val amount = jsonObject.optString("Amount", "0.0").toDoubleOrNull() ?: 0.0
                val vatRate = jsonObject.optInt("VatRate", 0)
                val responseCode = jsonObject.optString("ResponseCode", "-1")
                
                // Convert payment type string to int
                val paymentTypeInt = when(paymentTypeStr) {
                    "Cash" -> 1
                    "Credit" -> 2
                    "QR" -> 3
                    else -> -1
                }
                
                // Convert response code to status
                val status = when(responseCode) {
                    "00" -> 0
                    "01" -> 1
                    "02" -> 2
                    "03" -> 3
                    "99" -> 99
                    else -> -1
                }
                
                // Create transaction object
                val transaction = Transaction(
                    productId = productId,
                    productName = productName,
                    price = amount,
                    vatRate = vatRate,
                    status = status,
                    paymentType = paymentTypeInt
                )
                
                // Save to database
                transactionDao.insert(transaction)
                
                Log.d("SaleViewModel", "Transaction saved to database: $transaction")
            } catch (e: Exception) {
                Log.e("SaleViewModel", "Error saving transaction to database: ${e.message}")
            }
        }
    }

    // Update raw response and save to DB
    fun updateRawResponse(response: String) {
        Log.d("SaleViewModel", "Updating raw response to: $response")
        paymentResponseRaw = response
        
        // Save to database if response is not empty
        if (response.isNotEmpty()) {
            savePaymentResponseToDatabase(response)
        }
    }

    // Update methods
    fun updatePaymentResponseCode(code: Int) {
        Log.d("SaleViewModel", "Updating response code to: $code")
        
        // StateFlow güncellemesini viewModelScope içinde yaparak ana thread'de gerçekleşmesini sağla
        viewModelScope.launch {
            // Eski ve yeni değerleri karşılaştıralım
            val oldValue = _paymentResponseCode.value
            
            // StateFlow'un değerini güncelleyin
            _paymentResponseCode.value = code
            
            // Değişiklik sonrası onaylamak için debug log
            Log.d("SaleViewModel", "Response code updated: old=$oldValue, new=$code, current=${_paymentResponseCode.value}")
        }
    }

    fun updatePaymentResponse(responseType: String, amount: Double) {
        Log.d("SaleViewModel", "Updating payment type to: $responseType, amount: $amount")
        paymentType = responseType
        paymentAmount = amount.toString()
        _paymentData.value = """{"PaymentType":"$responseType","Amount":"$amount"}"""
    }

    fun clearFields(){
        productId=""
        productName=""
        price=""
        vatRate=""
        errorMessage=""
        // Reset payment response code as well
        _paymentResponseCode.value = -1
        Log.d("SaleViewModel", "All fields cleared, response code reset to -1")
    }
    fun resetAllFields(){
        productId=""
        productName=""
        price=""
        vatRate=""

    }
    fun validate(): Product?{
        val id = productId.toIntOrNull()
        val priceDouble = price.toDoubleOrNull()
        val vat = vatRate.toIntOrNull()

        if(id==null || id !in 1..9999){
            errorMessage="Invalid product id"
            return null
        }
        if(productName.length !in 1..20){
            errorMessage="Product name must be 1 to 20 characters"
            return null
        }
        if(priceDouble==null || priceDouble !in 0.01..99.99){
            errorMessage="Price must be between 0.01 and 99.99"
            return null
        }
        if(vat == null || vat !in 0..99){
            errorMessage = "VAT rate must be between 0 and 99"
            return null
        }




        return Product(id,productName,priceDouble,vat)



    }    fun insertTransaction(product: Product, status: Int, paymentType: Int) {
        val transaction = Transaction(
            productId = product.id,
            productName = product.name,
            price = product.price,
            vatRate = product.vatRate,
            status = status,
            paymentType = paymentType
        )

    }

    // Load the latest transaction from the database
    fun loadLatestTransaction() {
        viewModelScope.launch {
            try {
                val transaction = transactionDao.getLastTransaction()
                _latestTransaction.value = transaction
                Log.d("SaleViewModel", "Latest transaction loaded: $transaction")
            } catch (e: Exception) {
                Log.e("SaleViewModel", "Error loading latest transaction: ${e.message}")
            }
        }
    }
}