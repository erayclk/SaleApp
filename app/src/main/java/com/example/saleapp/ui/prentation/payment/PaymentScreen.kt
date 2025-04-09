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
    val qrCodeBitmap = remember { mutableStateOf<Bitmap?>(null) }

    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("PaymentScreen", "Payment result received: resultCode=${result.resultCode}, data=${result.data}")
        
        var responseCode = -1 // Default değer
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            responseCode = result.data?.getIntExtra(PaymentConstants.RESPONSE_CODE, -1) ?: -1
            Log.d("PaymentScreen", "Extracted responseCode from ActivityResult: $responseCode")
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
                // QR ödeme için response code 3
                viewModel.updatePaymentResponseCode(3)
                Log.d("PaymentScreen", "QR payment selected, setting responseCode=3")
                
                product?.let {
                    // QR kodu göster
                    qrCodeBitmap.value = generateQRCode("product id=${it.id}, product name=${it.name}, price=${it.price}, vat rate=${it.vatRate}")
                    
                    // Gerçek bir QR işlemi yapmak isterseniz bu kısmı aktif edebilirsiniz

                    val intent = communicator.createQrPaymentIntent(
                        context = context,
                        productId = it.id,
                        productName = it.name,
                        payAmount = it.price,
                        vatRate = it.vatRate
                    )
                    paymentLauncher.launch(intent)
                    sendRequest { result ->
                        Log.d("OkHttp", " qr Sunucudan gelen yanıt: $result")
                    }

                }
                
                // QR kodu gösterdikten sonra 3 saniye sonra geri dönmeyi devre dışı bıraktım
                // Böylece kullanıcı QR kodu görüntüleyebilir
                // navController.popBackStack()
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("QR Code")
        }

        // Display QR code if available
        qrCodeBitmap.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.padding(16.dp)
            )
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