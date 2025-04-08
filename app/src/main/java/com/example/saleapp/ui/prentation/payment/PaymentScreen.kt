package com.example.saleapp.ui.prentation.payment

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import org.json.JSONObject

@Composable
fun PaymentScreen(navController: NavHostController, viewModel: SaleViewModel) {
    val context = LocalContext.current
    val product = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<Product>("product")

    val communicator = remember { PaymentServiceHelper() }
    val qrCodeBitmap = remember { mutableStateOf<Bitmap?>(null) }
    val showQrCode = remember { mutableStateOf(false) }

    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val responseCode = result.data?.getIntExtra(PaymentConstants.RESPONSE_CODE, -1) ?: -1

        // Database insert after response for Credit Payment
        if (product != null && responseCode == 2) {
            viewModel.insertTransaction(product, status = 1, paymentType = 2)
        }

        navController.previousBackStackEntry?.savedStateHandle?.set("responseCode", responseCode)
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showQrCode.value && qrCodeBitmap.value != null) {
            // Display QR code
            Image(
                bitmap = qrCodeBitmap.value!!.asImageBitmap(),
                contentDescription = "QR Code for payment",
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            )
            
            Text(text = "Scan this QR code to pay", modifier = Modifier.padding(bottom = 16.dp))
            
            Button(
                onClick = {
                    product?.let {
                        val intent = communicator.createQRPaymentIntent(
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
                Text("Proceed with Payment")
            }
            
            Button(
                onClick = {
                    showQrCode.value = false
                    qrCodeBitmap.value = null
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Cancel QR Payment")
            }
        } else {
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
                onClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "responseCode",
                        1
                    )
                    navController.popBackStack()
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
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("CreditPayment")
            }
            Button(
                onClick = {
                    product?.let {
                        // Generate QR code content
                        val qrContent = JSONObject().apply {
                            put("productId", it.id)
                            put("productName", it.name)
                            put("amount", it.price)
                            put("timestamp", System.currentTimeMillis())
                        }.toString()
                        
                        // Generate QR code bitmap
                        qrCodeBitmap.value = generateQRCode(qrContent)
                        showQrCode.value = true
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("QRPayment")
            }

            if (product != null) {
                ProductInfo(product)
            } else {
                Text(text = "No product selected")
            }
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