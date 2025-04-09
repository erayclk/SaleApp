package com.example.saleapp.ui.prentation.sale

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun SaleScreen(
    viewModel: SaleViewModel,
    navController: NavHostController,
    onSubmit: (Product) -> Unit
) {
    // ViewModel hash'ini logla
    val viewModelHash = viewModel.hashCode()
    Log.d("SaleScreen", "Screen: Hash=$viewModelHash | Composable instance")
    
    val paymentType = remember { mutableStateOf("") }
    val paymentAmount = remember { mutableStateOf("") }

    // Log on screen mount
    LaunchedEffect(Unit) {
        Log.d("SaleScreen", "Screen: Hash=$viewModelHash | Mounted, viewModel.paymentResponseCode.value=${viewModel.paymentResponseCode.value}")
    }

    // Sadece ViewModel'deki değere odaklan
    val responseCodeValue = viewModel.paymentResponseCode.collectAsState().value
    Log.d("SaleScreen", "Screen: Hash=$viewModelHash | Rendering with responseCodeValue=$responseCodeValue")

    // Snackbar için CoroutineScope ve SnackbarHostState
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Response code değiştiğinde Snackbar göster
    LaunchedEffect(responseCodeValue) {
        if (responseCodeValue != -1) {
            val message = when (responseCodeValue) {
                0 -> "Ödeme başarılı (0)"
                1 -> "Ödeme nakit yapıldı"
                2 -> "Ödeme kredi kartı ile yapıldı"
                3 -> "Ödeme QR kod ile yapıldı"
                99 -> "İptal Edildi"
                else -> "Bilinmeyen durum: $responseCodeValue"
            }
            Log.d("SaleScreen", "Showing Snackbar with message: $message for responseCode=$responseCodeValue")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long // Snackbar'ın daha uzun görünmesini sağla
                )
            }
            // Debug için responseCode'u sıfırla - Sonraki ödeme için temiz başlangıç
            // Bunu kaldırırsanız, değer değişmediği sürece Snackbar bir daha gösterilmez
            if (responseCodeValue != 99) { // İptal durumunda zaten temizleniyor
                // Kısa bir gecikme ekleyerek Snackbar'ın görünmesine izin ver
                kotlinx.coroutines.delay(3000) // 3 saniye bekle
                viewModel.updatePaymentResponseCode(-1)
                Log.d("SaleScreen", "Reset responseCode to -1 after showing Snackbar")
            }
        }
    }

    // Cancel durumunda alanları temizlemek için ayrı bir LaunchedEffect ekleyebiliriz
    LaunchedEffect(responseCodeValue) {
        if (responseCodeValue == 99) {
            Log.d("SaleScreen", "Cancel code detected (99), clearing fields.")
            viewModel.clearFields()
            // ViewModel'deki state'i resetle (opsiyonel, clearFields zaten -1 yapıyor)
            viewModel.updatePaymentResponseCode(-1) 
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Satış Ekranı",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))

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
}

/*
@Preview(showBackground = true)
@Composable
fun SaleScreenPreview() {
    SaleScreen(viewModel = SaleViewModel(), onSubmit = {},
        navHostController = navHostController))
}
*/
