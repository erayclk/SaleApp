package com.example.saleapp.ui.prentation.sale

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("UnrememberedMutableState")
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    navController: NavHostController,
    onSubmit: (Product) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe payment response code changes
    val responseCode by viewModel.paymentResponseCode.collectAsState()

    // Show success message when payment is completed
    LaunchedEffect(responseCode) {
        if (responseCode > 0) {
            when (responseCode) {
                1 -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Cash payment completed successfully",
                        duration = SnackbarDuration.Short
                    )
                }
                2 -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Credit card payment completed successfully",
                        duration = SnackbarDuration.Short
                    )
                }
                3 -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "QR payment completed successfully",
                        duration = SnackbarDuration.Short
                    )
                }
                99 -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Payment cancelled",
                        duration = SnackbarDuration.Short
                    )
                }
            }
            // Reset payment response code
            viewModel.clearFields()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Sale Entry",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = viewModel.productId,
                onValueChange = { viewModel.productId = it },
                label = { Text("Product ID") })

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.productName,
                onValueChange = { viewModel.productName = it },
                label = { Text("Product Name") })

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
                modifier = Modifier.fillMaxWidth()
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
            
            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
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
