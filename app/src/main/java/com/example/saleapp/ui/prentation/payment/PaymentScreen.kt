package com.example.saleapp.ui.prentation.payment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.PaymentConstants

import com.example.saleapp.model.Product

import com.example.saleapp.service.RegistryService
import com.example.saleapp.ui.prentation.payment.qrcode.generateQRCode
import com.example.saleapp.ui.prentation.sale.SaleViewModel
import sendRequest
import org.json.JSONObject
@Composable
fun PaymentScreen(navController: NavHostController, viewModel: SaleViewModel) {
    val context = LocalContext.current
    val product = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Product>("product")

    val communicator = remember { PaymentServiceHelper() }

    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val responseCode = if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.getIntExtra(PaymentConstants.RESPONSE_CODE, -1) ?: -1
        } else -1

        result.data?.getStringExtra(PaymentConstants.RESPONSE_DATA)?.let {
            if (it.isNotEmpty()) viewModel.updateRawResponse(it)
        }

        viewModel.updatePaymentResponseCode(responseCode)
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        product?.let {
            val intent = Intent(context, RegistryService::class.java).apply {
                putExtra("productId", it.id)
                putExtra("productName", it.name)
                putExtra("price", it.price)
                putExtra("vatRate", it.vatRate)
            }
            context.startService(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,

    ) {
        product?.let {
            ProductCard(it)
        } ?: Text("No product selected", style = MaterialTheme.typography.bodyLarge)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentButton("Cash Payment") {
                product?.let {
                    val intent = communicator.createCachPaymentIntent(
                        context, it.id, it.name, it.price, it.vatRate
                    )
                    paymentLauncher.launch(intent)
                }
            }

            PaymentButton("Credit Payment") {
                product?.let {
                    val intent = communicator.createCreditPaymentIntent(
                        context, it.id, it.name, it.price, it.vatRate
                    )
                    paymentLauncher.launch(intent)
                    sendRequest { result ->
                        Log.d("OkHttp", "Sunucudan gelen yanıt: $result")
                    }
                }
            }

            PaymentButton("QR Code") {
                product?.let {
                    val intent = Intent(context, RegistryService::class.java).apply {
                        putExtra(PaymentConstants.PRODUCT_ID, it.id)
                        putExtra(PaymentConstants.PRODUCT_NAME, it.name)
                        putExtra(PaymentConstants.PAY_AMOUNT, it.price)
                        putExtra(PaymentConstants.VAT_RATE, it.vatRate)
                        putExtra(PaymentConstants.PAY_TYPE, PaymentConstants.PAYMENT_QR)
                    }
                    context.startService(intent)
                    navController.currentBackStackEntry?.savedStateHandle?.set("product", it)
                    navController.navigate("qr_code")
                }
            }

            PaymentButton("Cancel") {
                navController.previousBackStackEntry?.savedStateHandle?.set("responseCode", 99)
                navController.popBackStack()
                viewModel.clearFields()
            }
        }
    }
}

@Composable
fun PaymentButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}

@Composable
fun ProductCard(product: Product) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Product: ${product.name}", style = MaterialTheme.typography.titleMedium)
        Text("Price: ${product.price} ", style = MaterialTheme.typography.bodyMedium)
        Text("VAT: ${product.vatRate}%", style = MaterialTheme.typography.bodyMedium)
        Text("ID: ${product.id}", style = MaterialTheme.typography.labelMedium)
    }
}
