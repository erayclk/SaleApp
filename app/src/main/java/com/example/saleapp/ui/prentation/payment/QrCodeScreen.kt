package com.example.saleapp.ui.prentation.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.saleapp.model.Product
import com.example.saleapp.ui.prentation.sale.SaleViewModel
import android.graphics.Bitmap
import android.util.Log
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.saleapp.model.PaymentConstants
import com.example.saleapp.ui.prentation.payment.qrcode.generateQRCode
import org.json.JSONObject

// LocalBroadcastManager için doğru import
import androidx.localbroadcastmanager.content.LocalBroadcastManager

// QrPaymentReceiver statik değerleri
// private const val ACTION_QR_PAYMENT_PROCESSED = "com.example.saleapp.QR_PAYMENT_PROCESSED"
// private var lastQrResponse = ""
// private var lastQrContent = ""
// private var lastResponseReceived = false

@Composable
fun QrCodeScreen(
    navController: NavHostController,
    viewModel: SaleViewModel,
    product: Product?,
    qrCodeBitmap: Bitmap? = null,
    serverResponse: String = ""
) {
    val context = LocalContext.current
    
    // State'leri tanımla
    val qrCodeState = remember { mutableStateOf(qrCodeBitmap) }
    val serverResponseState = remember { mutableStateOf(serverResponse) }
    val qrContentState = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(true) }
    
    // Başlangıçta bir başlangıç QR kodu oluştur (ürün bilgileri varsa)
    LaunchedEffect(product) {
        Log.d("QrCodeScreen", "LaunchedEffect - Creating initial QR code")
        if (product != null) {
            Log.d("QrCodeScreen", "Product data available: ID=${product.id}, Name=${product.name}, Price=${product.price}")
            try {
                val initialQrContent = "product id=${product.id}, product name=${product.name}, price=${product.price}, vat rate=${product.vatRate}"
                qrContentState.value = initialQrContent
                
                // QR kodunu oluştur
                val generatedQrCode = generateQRCode(initialQrContent)
                qrCodeState.value = generatedQrCode
                
                Log.d("QrCodeScreen", "Created initial QR code from product data: ${generatedQrCode != null}")
                
                // QR kodu başarıyla oluşturulduysa yükleme durumunu kapat
                if (generatedQrCode != null) {
                    isLoading.value = false
                    Log.d("QrCodeScreen", "Loading state set to false, QR code is ready to display")
                } else {
                    Log.e("QrCodeScreen", "Failed to generate QR code - result was null")
                    // Yükleme durumunu güncelle ama QR hatası göster
                    isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("QrCodeScreen", "Error generating initial QR code: ${e.message}")
                e.printStackTrace()
                // Hata durumunda da yükleme durumunu kapat
                isLoading.value = false
            }
        } else {
            // Ürün yoksa da yüklemeyi kapat ve uyarı ver
            Log.w("QrCodeScreen", "No product data available for QR code generation")
            isLoading.value = false
        }
    }
    
    // Global BroadcastReceiver tanımla
    val qrPaymentReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("QrCodeScreen", "Broadcast received: ${intent.action}")
                
                if (intent.action == PaymentConstants.PAYMENT_RESPONSE_ACTION) {
                    try {
                        // QR içeriğini al
                        val qrContent = intent.getStringExtra("QR_CONTENT") ?: ""
                        Log.d("QrCodeScreen", "Received QR_CONTENT: $qrContent")
                        qrContentState.value = qrContent
                        
                        // Server yanıtını al
                        val response = intent.getStringExtra(PaymentConstants.RESPONSE_DATA) ?: ""
                        Log.d("QrCodeScreen", "Received RESPONSE_DATA: $response")
                        serverResponseState.value = response
                        
                        // QR kod bitmap'i oluştur
                        if (qrContent.isNotEmpty()) {
                            Log.d("QrCodeScreen", "Generating QR code from content")
                            qrCodeState.value = generateQRCode(qrContent)
                            Log.d("QrCodeScreen", "QR code generated successfully")
                        }
                        
                        // Yükleme durumunu kapat
                        isLoading.value = false
                    } catch (e: Exception) {
                        Log.e("QrCodeScreen", "Error processing broadcast: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    // Local BroadcastReceiver tanımla
    val localBroadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("QrCodeScreen", "Local broadcast received: ${intent.action}")
                
                // QR içeriği ve yanıtı al
                val qrContent = intent.getStringExtra("QR_CONTENT") ?: ""
                val response = intent.getStringExtra(PaymentConstants.RESPONSE_DATA) ?: ""
                
                Log.d("QrCodeScreen", "Local broadcast QR content: $qrContent")
                Log.d("QrCodeScreen", "Local broadcast response: $response")
                
                // State'leri güncelle
                qrContentState.value = qrContent
                serverResponseState.value = response
                
                // QR kod bitmap'i oluştur
                if (qrContent.isNotEmpty()) {
                    qrCodeState.value = generateQRCode(qrContent)
                }
                
                // Yükleme durumunu kapat
                isLoading.value = false
            }
        }
    }
    
    // BroadcastReceiver'ları kaydet
    DisposableEffect(context) {
        var globalReceiverRegistered = false
        var localReceiverRegistered = false
        
        try {
            // Local broadcast receiver'ı kaydet
            val localFilter = IntentFilter(QrPaymentReceiver.ACTION_QR_PAYMENT_PROCESSED)
            try {
                val localBroadcastManager = LocalBroadcastManager.getInstance(context)
                localBroadcastManager.registerReceiver(localBroadcastReceiver, localFilter)
                localReceiverRegistered = true
                Log.d("QrCodeScreen", "Local broadcast receiver registered")
            } catch (e: Exception) {
                Log.e("QrCodeScreen", "Error registering local receiver: ${e.message}")
            }
            
            // Global değişkenlerdeki değerleri kontrol et
            if (QrPaymentReceiver.lastResponseReceived) {
                Log.d("QrCodeScreen", "Using cached response from QrPaymentReceiver")
                qrContentState.value = QrPaymentReceiver.lastQrContent
                serverResponseState.value = QrPaymentReceiver.lastQrResponse
                
                if (QrPaymentReceiver.lastQrContent.isNotEmpty()) {
                    qrCodeState.value = generateQRCode(QrPaymentReceiver.lastQrContent)
                }
                
                isLoading.value = false
            }
            
            // Global broadcast receiver'ı kaydet - Android 14+ için RECEIVER_NOT_EXPORTED flag'ini kullan
            val globalFilter = IntentFilter()
            globalFilter.addAction(PaymentConstants.PAYMENT_RESPONSE_ACTION) // com.example.saleapp.PAYMENT_RESPONSE
            
            try {
                if (Build.VERSION.SDK_INT >= 34) {  // Android 14+ (UPSIDE_DOWN_CAKE)
                    // Android 14+ için registerReceiver RECEIVER_NOT_EXPORTED bayrağı gerektirir
                    context.registerReceiver(
                        qrPaymentReceiver,
                        globalFilter, 
                        Context.RECEIVER_NOT_EXPORTED  // Koruma düzeyi - dışarı açık olmayan alıcı
                    )
                    Log.d("QrCodeScreen", "Registered with RECEIVER_NOT_EXPORTED flag for API 34+")
                } 
                else if (Build.VERSION.SDK_INT >= 33) {  // Android 13 (TIRAMISU)
                    // Android 13 için de RECEIVER_NOT_EXPORTED kullan
                    context.registerReceiver(
                        qrPaymentReceiver, 
                        globalFilter, 
                        Context.RECEIVER_NOT_EXPORTED
                    )
                    Log.d("QrCodeScreen", "Registered with RECEIVER_NOT_EXPORTED flag for API 33")
                } 

                globalReceiverRegistered = true
                Log.d("QrCodeScreen", "Global broadcast receiver registered for action: ${PaymentConstants.PAYMENT_RESPONSE_ACTION}")
            } catch (e: Exception) {
                Log.e("QrCodeScreen", "Error registering global receiver: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e("QrCodeScreen", "Error in receiver setup: ${e.message}")
            e.printStackTrace()
        }
        
        onDispose {
            try {
                // Local receiver'ı kaldır
                if (localReceiverRegistered) {
                    try {
                        val localBroadcastManager = LocalBroadcastManager.getInstance(context)
                        localBroadcastManager.unregisterReceiver(localBroadcastReceiver)
                        Log.d("QrCodeScreen", "Local broadcast receiver unregistered")
                    } catch (e: Exception) {
                        Log.e("QrCodeScreen", "Error unregistering local receiver: ${e.message}")
                    }
                }
                
                // Global receiver'ı kaldır
                if (globalReceiverRegistered) {
                    try {
                        context.unregisterReceiver(qrPaymentReceiver)
                        Log.d("QrCodeScreen", "Global broadcast receiver unregistered")
                    } catch (e: Exception) {
                        Log.e("QrCodeScreen", "Error unregistering global receiver: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("QrCodeScreen", "Error unregistering receivers: ${e.message}")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "QR Ödeme",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // QR kodu göster
        if (isLoading.value) {
            CircularProgressIndicator(
                modifier = Modifier.size(120.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "QR kod oluşturuluyor...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            qrCodeState.value?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(280.dp)
                        .padding(16.dp)
                )
                
                // QR gösterildiğine dair log ekleyelim
                Log.d("QrCodeScreen", "Displaying QR code bitmap of size: ${bitmap.width}x${bitmap.height}")
            } ?: run {
                // QR kodu oluşturulamadıysa hata mesajı göster
                Text(
                    text = "QR kod oluşturulamadı!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Log.e("QrCodeScreen", "No QR code bitmap available to display")
                
                // Yeniden denemek için bir buton ekleyelim
                Button(
                    onClick = {
                        if (product != null) {
                            try {
                                // Yeniden QR kodu oluşturmaya çalış
                                val initialQrContent = "product id=${product.id}, product name=${product.name}, price=${product.price}, vat rate=${product.vatRate}"
                                qrContentState.value = initialQrContent
                                qrCodeState.value = generateQRCode(initialQrContent)
                                Log.d("QrCodeScreen", "Retried creating QR code")
                            } catch (e: Exception) {
                                Log.e("QrCodeScreen", "Error retrying QR code generation: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Yeniden Dene")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // QR içeriği göster
        if (qrContentState.value.isNotEmpty()) {
            Text(
                text = "QR İçeriği:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = qrContentState.value,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sunucudan gelen veri varsa göster
        if (serverResponseState.value.isNotEmpty()) {
            // JSON parse işlemini composable'ların dışında yapıyoruz
            val jsonObject = try {
                JSONObject(serverResponseState.value)
            } catch (e: Exception) {
                null
            }
            
            // JSON başarıyla parse edildiyse
            if (jsonObject != null) {
                Text(
                    text = "Sunucu Yanıtı:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                ) {
                    if (jsonObject.has("ProductId")) {
                        Text("Ürün ID: ${jsonObject.optString("ProductId")}")
                    }
                    if (jsonObject.has("ProductName")) {
                        Text("Ürün Adı: ${jsonObject.optString("ProductName")}")
                    }
                    if (jsonObject.has("Amount")) {
                        Text("Tutar: ${jsonObject.optString("Amount")}")
                    }
                    if (jsonObject.has("PaymentType")) {
                        Text("Ödeme Tipi: ${jsonObject.optString("PaymentType")}")
                    }
                    if (jsonObject.has("ResponseCode")) {
                        Text("Yanıt Kodu: ${jsonObject.optString("ResponseCode")}")
                    }
                    if (jsonObject.has("VatRate")) {
                        Text("KDV Oranı: ${jsonObject.optString("VatRate")}")
                    }
                }
            } else {
                // JSON parse edilemezse ham veriyi göster
                Text(
                    text = "Ham Veri: ${serverResponseState.value}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // Ürün bilgisi varsa göster
        else if (product != null) {
            Text(
                text = "Ödeme Detayları",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(text = "Ürün: ${product.name}")
            Text(text = "Fiyat: ${product.price}")
            Text(text = "KDV: %${product.vatRate}")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // QR ödeme başarılı - önce ödeme durumunu güncelle
                    viewModel.updatePaymentResponseCode(3)
                    Log.d("QrCodeScreen", "QR payment completed, setting responseCode=3")
                    
                    // Sunucu yanıtı varsa kaydet
                    if (serverResponseState.value.isNotEmpty()) {
                        viewModel.updateRawResponse(serverResponseState.value)
                    }
                    
                    // Navigasyon öncesi kısa bir gecikme ekliyoruz
                    // Bu işlem ViewModel'deki durumun güncellenmesi için zaman tanır
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        // SaleScreen'e doğrudan ve tek adımda navigasyon yap
                        navController.navigate("sale") {
                            // Mevcut backstack'i temizle
                            popUpTo("sale") { inclusive = false }
                            // Tek varış noktası
                            launchSingleTop = true
                        }
                    }, 100) // 100ms gecikme
                }
            ) {
                Text("Ödemeyi Tamamla")
            }
            
            Button(
                onClick = {
                    navController.popBackStack() // Sadece QR ekranını kapat
                }
            ) {
                Text("Geri Dön")
            }
        }
    }
} 