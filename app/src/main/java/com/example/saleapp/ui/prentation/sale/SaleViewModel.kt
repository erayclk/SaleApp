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


    // Update methods
    fun updatePaymentResponseCode(code: Int) {
        Log.d("SaleViewModel", "Updating response code to: $code")
        
        // Eski ve yeni değerleri karşılaştıralım
        val oldValue = _paymentResponseCode.value
        
        // StateFlow'un değerini güncelleyin
        _paymentResponseCode.value = code
        
        // Değişiklik sonrası onaylamak için debug log
        Log.d("SaleViewModel", "Response code updated: old=$oldValue, new=$code, current=${_paymentResponseCode.value}")
        
        // Hemen bu değişikliği test etmek için
        val currentValue = _paymentResponseCode.value
        if (currentValue != code) {
            Log.e("SaleViewModel", "ERROR: Value didn't update correctly! expected=$code, actual=$currentValue")
        }
    }

    fun updatePaymentResponse(responseType: String, amount: Double) {
        Log.d("SaleViewModel", "Updating payment type to: $responseType, amount: $amount")
        paymentType = responseType
        paymentAmount = amount.toString()
        _paymentData.value = """{"PaymentType":"$responseType","Amount":"$amount"}"""
    }

    fun updateRawResponse(response: String) {
        Log.d("SaleViewModel", "Updating raw response to: $response")
        paymentResponseRaw = response
    }
    // Payment Type ve Amount için ayrı state'ler
    var paymentType by mutableStateOf("")
    var paymentAmount by mutableStateOf("")

   // val transactionDao = TransactionDatabase.getDatabase(application).transactionDao()



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


}