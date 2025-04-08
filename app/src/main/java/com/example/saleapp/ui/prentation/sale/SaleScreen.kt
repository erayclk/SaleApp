package com.example.saleapp.ui.prentation.sale

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product
import org.json.JSONObject


@SuppressLint("UnrememberedMutableState")
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    navController: NavHostController,
    onSubmit: (Product) -> Unit
) {



    val paymentType = remember { mutableStateOf("") }
    val paymentAmount = remember { mutableStateOf("") }

    val responseCode by navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<Int?>("responseCode", null)
        ?.collectAsState() ?: mutableStateOf(null)

    LaunchedEffect(responseCode) {
        responseCode?.let { code ->
            if (code == 99) { // Cancel kodu
                viewModel.clearFields()
                // State'i sıfırla (tekrar tetiklenmemesi için)
                navController.previousBackStackEntry?.savedStateHandle?.remove<Int>("responseCode")
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.paymentData.collect { data ->
            try {
                val jsonObject = JSONObject(data)
                paymentType.value = jsonObject.optString("PaymentType", "Unknown")
                paymentAmount.value = jsonObject.optString("Amount", "0.00")

                // Gelen veriyi logla
                Log.d(
                    "PAYMENT_DATA",
                    "Payment Type: ${paymentType.value}, Amount: ${paymentAmount.value}"
                )
            } catch (e: Exception) {
                Log.e("PAYMENT_DATA", "Error parsing payment data", e)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sale Screen",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = viewModel.productId,
            onValueChange = { viewModel.productId = it },
            label = { Text("Product ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.productName,
            onValueChange = { viewModel.productName = it },
            label = { Text("Product Name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.price,
            onValueChange = { viewModel.price = it },
            label = { Text("Price") })

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.vatRate,
            onValueChange = { viewModel.vatRate = it },
            label = { Text("VAT Rate") })

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {

            Button(onClick = { viewModel.clearFields() }) {
                Text("Clear")
            }

            Button(onClick = {
                val product = viewModel.validate()
                if (product != null) {
                    onSubmit(product)
                }
            }) {
                Text("Submit")
            }
        }
        Text(viewModel.errorMessage)

    }

}

/*
@Preview(showBackground = true)
@Composable
fun SaleScreenPreview() {
    SaleScreen(viewModel = SaleViewModel(), onSubmit = {},
        navHostController = navHostController))
}
*/
