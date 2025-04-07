package com.example.saleapp.ui.prentation.sale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.saleapp.model.Product

class SaleViewModel :ViewModel(){
    var productId by mutableStateOf("")
    var productName by mutableStateOf("")
    var price by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var vatRate by mutableStateOf("")

    fun clearFields(){
        productId=""
        productName=""
        price=""
        vatRate=""
        errorMessage=""

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

}