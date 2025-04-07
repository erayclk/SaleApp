package com.example.saleapp.ui.prentation.payment

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product
import com.example.saleapp.service.RegistryService

@Composable
fun PaymentScreen(navController: NavHostController) {
    val context = LocalContext.current
    val product = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Product>("product")

    LaunchedEffect(Unit) {
        if (product != null) {

            val intent = Intent(context, RegistryService::class.java)
            intent.putExtra("productId", product.id)
            intent.putExtra("productName", product.name)
            intent.putExtra("price", product.price)
            intent.putExtra("vatRate", product.vatRate)

            context.startService(intent)

        }
    }
    Column (modifier = Modifier.fillMaxSize()
        .padding(15.dp),
        verticalArrangement = Arrangement.Center,
    ){

        Column (modifier = Modifier.fillMaxSize()
            .padding(15.dp),
            verticalArrangement = Arrangement.Center
            ,
            ){
            if (product != null) {
                ProductInfo(product)
            }
            else{
                Text(text = "No product selected")
            }
        }
    }

}

@Composable
private fun ProductInfo(product: Product){
    Column (modifier = Modifier.fillMaxSize()
        .padding(15.dp),
    verticalArrangement = Arrangement.Center)
    {
        Text(text = "Product ID: ${product.id}")
        Text(text = "Product Name: ${product.name}")
        Text(text = "Price: ${product.price}")
        Text(text = "VAT Rate: ${product.vatRate}")

    }
}