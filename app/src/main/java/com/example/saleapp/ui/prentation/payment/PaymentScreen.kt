package com.example.saleapp.ui.prentation.payment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        Log.d("PaymentScreen", "Payment result received: resultCode=${result.resultCode}, data=${result.data}")
        
        var responseCode = -1 // Default değer
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            responseCode = result.data?.getIntExtra(PaymentConstants.RESPONSE_CODE, -1) ?: -1
            Log.d("PaymentScreen", "Extracted responseCode from ActivityResult: $responseCode")
            
            // Get the full response data
            val responseData = result.data?.getStringExtra(PaymentConstants.RESPONSE_DATA) ?: ""
            if (responseData.isNotEmpty()) {
                Log.d("PaymentScreen", "Full response data: $responseData")
                // Store the raw response in the ViewModel
                viewModel.updateRawResponse(responseData)
            }
        } else {
            Log.w("PaymentScreen", "Payment activity did not return RESULT_OK or data was null. ResultCode: ${result.resultCode}")
            // Hata veya iptal durumu için bir varsayılan kod atanabilir, örneğin 99
            // responseCode = 99 
        }
        
        // ViewModel'i Güncelle
        viewModel.updatePaymentResponseCode(responseCode)
        Log.d("PaymentScreen", "Updated shared ViewModel with responseCode=$responseCode. Current ViewModel state: ${viewModel.paymentResponseCode.value}")
        
        // Geri dön
        navController.popBackStack()
    }

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
            .padding(top = 35.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = {
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "responseCode",
                    99
                )
                navController.popBackStack()
                viewModel.clearFields()
            },
        ) {
            Text("Cancel")
        }
        Button(
            onClick = {



                product?.let {
                    val intent = communicator.createCachPaymentIntent(
                        context = context,
                        productId = it.id,
                        productName = it.name,
                        payAmount = it.price,
                        vatRate = it.vatRate
                    )
                    paymentLauncher.launch(intent)
                }

                

            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("CashPayment")
        }
        Button(
            onClick = {
                product?.let {
                    val intent = communicator.createCreditPaymentIntent(
                        context = context,
                        productId = it.id,
                        productName = it.name,
                        payAmount = it.price,
                        vatRate = it.vatRate
                    )
                    paymentLauncher.launch(intent)
                    sendRequest { result ->
                        Log.d("OkHttp", " kredi Sunucudan gelen yanıt: $result")
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("CreditPayment")
        }

        Button(
            onClick = {
                // QR ödeme için RegistryService'e yönlendir
                product?.let {
                    // QR ödeme tipini belirle
                    val intent = Intent(context, RegistryService::class.java).apply {
                        putExtra(PaymentConstants.PRODUCT_ID, it.id)
                        putExtra(PaymentConstants.PRODUCT_NAME, it.name)
                        putExtra(PaymentConstants.PAY_AMOUNT, it.price)
                        putExtra(PaymentConstants.VAT_RATE, it.vatRate)
                        putExtra(PaymentConstants.PAY_TYPE, PaymentConstants.PAYMENT_QR) // QR ödeme tipi
                    }
                    
                    // RegistryService'i başlat
                    context.startService(intent)
                    
                    // Ürün bilgisini qr_code ekranına iletmek için savedStateHandle'a kaydet
                    navController.currentBackStackEntry?.savedStateHandle?.set("product", it)
                    
                    // QR kodu oluşturulması ve görüntülenmesi için QrCodeScreen'e geçiş yap
                    // QR içeriği RegistryService tarafından broadcast olarak geri gönderilecek
                    navController.navigate("qr_code")
                    
                    Log.d("PaymentScreen", "Navigating to QR code screen with product: ${it.id}-${it.name}")
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("QR Code")
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
    Column {
        Text(text = "Product ID: ${product.id}")
        Text(text = "Product Name: ${product.name}")
        Text(text = "Price: ${product.price}")
        Text(text = "VAT Rate: ${product.vatRate}")
    }
}