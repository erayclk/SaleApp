package com.example.saleapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.saleapp.model.PaymentConstants
import com.example.saleapp.ui.prentation.navigation.AppNavigation
import com.example.saleapp.ui.prentation.sale.SaleViewModel
import com.example.saleapp.ui.theme.SaleAppTheme

class MainActivity : ComponentActivity() {
    // Aktivite düzeyinde bir SaleViewModel oluştur
    private val saleViewModel: SaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SaleAppTheme {
                val navController = rememberNavController()

                // Ortak viewModel'i NavGraph'a geçir
                AppNavigation(
                    navController = navController,
                    sharedViewModel = saleViewModel
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

