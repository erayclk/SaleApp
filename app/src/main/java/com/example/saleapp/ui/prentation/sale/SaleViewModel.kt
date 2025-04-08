package com.example.saleapp.ui.prentation.sale

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleapp.model.Product
import com.example.saleapp.model.Transaction
import com.example.saleapp.room.TransactionDatabase
import kotlinx.coroutines.launch

class SaleViewModel (application: Application): AndroidViewModel(application){
    var productId by mutableStateOf("")
    var productName by mutableStateOf("")
    var price by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var vatRate by mutableStateOf("")

    val transactionDao = TransactionDatabase.getDatabase(application).transactionDao()


    fun clearFields(){
        productId=""
        productName=""
        price=""
        vatRate=""
        errorMessage=""

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



        errorMessage=""
        return Product(id,productName,priceDouble,vat)



    }
    fun insertTransaction(product: Product, status: Int, paymentType: Int) {
        val transaction = Transaction(
            productId = product.id,
            productName = product.name,
            price = product.price,
            vatRate = product.vatRate,
            status = status,
            paymentType = paymentType
        )
        viewModelScope.launch {
            transactionDao.insert(transaction)
        }
    }
    fun updateTransaction(id: Int, status: Int, paymentType: Int) {
        viewModelScope.launch {
            transactionDao.updateStatusAndType(id, status, paymentType)
        }
    }

}