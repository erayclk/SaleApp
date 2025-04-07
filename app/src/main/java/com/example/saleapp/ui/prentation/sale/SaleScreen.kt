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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product

@SuppressLint("UnrememberedMutableState")
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    navHostController: NavHostController,
    onSubmit: (Product) -> Unit
) {

    val currentEntry = navHostController.currentBackStackEntry
    Log.d("NAVIGATION_DEBUG", "Current backstack entry: ${currentEntry?.destination?.route}")

    val responseCode by currentEntry?.savedStateHandle?.getStateFlow<Int?>("responseCode", null)
        ?.collectAsState() ?: mutableStateOf(null)


    LaunchedEffect(responseCode) {

        if (responseCode != null) {
            viewModel.errorMessage = when (responseCode) {
                99 -> "Payment canceled"
                1 -> "Payment was made in cash"
                else -> ""
            }
            viewModel.resetAllFields()
            currentEntry?.savedStateHandle?.remove<Int>("responseCode")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sale Screen",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = viewModel.productId,
            onValueChange = {viewModel.productId=it},
            label = { Text("Product ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.productName,
            onValueChange = {viewModel.productName=it},
            label = { Text("Product Name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.price,
            onValueChange = {viewModel.price=it},
            label = { Text("Price") })

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.vatRate,
            onValueChange = {viewModel.vatRate=it},
            label = { Text("VAT Rate") })

        Spacer(modifier = Modifier.height(16.dp))

        Row (horizontalArrangement = Arrangement.SpaceEvenly,
            ){

            Button(onClick = {viewModel.clearFields()}) {
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
        Spacer(modifier = Modifier.height(16.dp))
        if(viewModel.errorMessage.isNotEmpty()){
            Text(text = viewModel.errorMessage)
        }
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
