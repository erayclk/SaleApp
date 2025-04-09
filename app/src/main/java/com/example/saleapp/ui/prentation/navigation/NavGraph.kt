package com.example.saleapp.ui.prentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.saleapp.model.Product
import com.example.saleapp.ui.prentation.login.LoginScreen
import com.example.saleapp.ui.prentation.payment.PaymentScreen
import com.example.saleapp.ui.prentation.payment.QrCodeScreen
import com.example.saleapp.ui.prentation.sale.SaleScreen
import com.example.saleapp.ui.prentation.sale.SaleViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sharedViewModel: SaleViewModel? = null
) {
    // Eğer sharedViewModel null ise, yerel ViewModel oluştur
    val viewModel = sharedViewModel ?: viewModel<SaleViewModel>()
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("sale") })
        }

        composable("sale") {
            SaleScreen(
                viewModel = viewModel,
                navController = navController,
                onSubmit = { product ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "product",
                        product
                    )
                    navController.navigate("payment") {
                        popUpTo("sale") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable("payment") {
            PaymentScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        composable("qr_code") {
            // QR Code ekranını buraya ekledik
            // Ürün bilgisini doğrudan önceki ekrandan alıyoruz
            val product = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Product>("product")
            
            Log.d("NavGraph", "QrCodeScreen: product=${product?.id}-${product?.name ?: "null"}")
            
            // Ürün bilgisi ile QrCodeScreen'i başlat
            QrCodeScreen(
                navController = navController,
                viewModel = viewModel,
                product = product,
                qrCodeBitmap = null,
                serverResponse = ""
            )
        }
    }
}