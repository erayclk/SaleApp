package com.example.saleapp.ui.prentation.payment

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product
import com.example.saleapp.service.RegistryService
import com.example.saleapp.ui.prentation.sale.SaleViewModel

@Composable
fun PaymentScreen(navController: NavHostController,viewModel: SaleViewModel) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top =35.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = {
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "responseCode",
                    99
                )
                navController.popBackStack()
            },

        ) {
            Text("Cancel")
        }
        Button(
            onClick = { navController.previousBackStackEntry?.savedStateHandle?.set(
                "responseCode",
                1
            )
                navController.popBackStack() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("CashPayment")
        }
        Button(
            onClick = { TODO() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("CreditPayment")
        }
        Button(
            onClick = { TODO() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("“QRPayment")
        }


        if (product != null) {
            ProductInfo(product)
        } else {
            Text(text = "No product selected")
        }

    }
}


@Composable
private fun ProductInfo(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxSize(),

    )
    {
        Text(text = "Product ID: ${product.id}")
        Text(text = "Product Name: ${product.name}")
        Text(text = "Price: ${product.price}")
        Text(text = "VAT Rate: ${product.vatRate}")

    }
}